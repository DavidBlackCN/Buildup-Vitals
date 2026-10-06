package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.config.CoreBalance;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState.Mode;

/** One recovery clock; food pays first, natural recovery can fill a fractional food tail. */
public final class RecoveryController {
    private RecoveryController() { }

    public record Conditions(boolean alive, boolean active, double health, double maxHealth,
                             int hunger, float saturation, boolean naturalEnabled) { }

    public record Step(RecoveryState state, float healing, float foodHealing) {
        private float actual(float gained) {
            return Float.isFinite(gained) ? Math.clamp(gained, 0, healing) : 0;
        }
        public RecoveryState settle(float gained) {
            return new RecoveryState(Math.max(0, state.reserve() - Math.min(foodHealing, actual(gained))),
                    state.progress(), state.mode());
        }
        public float exhaustion(float gained) {
            return Math.max(0, actual(gained) - foodHealing) * RecoveryBalance.NATURAL_EXHAUSTION_PER_HP;
        }
    }

    public static Step tick(RecoveryState state, Conditions conditions) {
        return tick(state, conditions, RecoveryBalance.FOOD_TICKS);
    }
    public static Step tick(RecoveryState state, Conditions conditions, int foodInterval) {
        return tick(state, conditions, foodInterval, 1.0, true);
    }
    public static Step tick(RecoveryState state, Conditions conditions, int foodInterval, int wellFedInterval) {
        return tick(state, conditions, foodInterval, RecoveryBalance.WELL_FED_TICKS / (double) wellFedInterval, true);
    }

    public static double naturalInterval(Conditions c, double speed) {
        if (!Double.isFinite(speed) || speed < 1) throw new IllegalArgumentException("Invalid natural recovery speed");
        if (!c.naturalEnabled() || !Float.isFinite(c.saturation()) || c.hunger() < 18) return Double.POSITIVE_INFINITY;
        return Math.max(CoreBalance.Recovery.MIN_NATURAL_TICKS,
                (c.hunger() >= 20 && c.saturation() > 0 ? RecoveryBalance.WELL_FED_TICKS : RecoveryBalance.STABLE_TICKS) / speed);
    }

    public static Step tick(RecoveryState state, Conditions c, int foodInterval, double naturalSpeed, boolean foodAllowed) {
        if (foodInterval < 10 || foodInterval > RecoveryBalance.FOOD_TICKS) throw new IllegalArgumentException("Invalid food interval");
        double naturalInterval = naturalInterval(c, naturalSpeed);
        if (!c.alive()) return new Step(RecoveryState.EMPTY, 0, 0);
        if (!c.active() || !Double.isFinite(c.health()) || !Double.isFinite(c.maxHealth())
                || c.health() <= 0 || c.health() >= c.maxHealth()) return new Step(state.idle(), 0, 0);
        boolean food = foodAllowed && state.reserve() > 0;
        boolean natural = Double.isFinite(naturalInterval);
        if (!food && !natural) return new Step(state.idle(), 0, 0);
        boolean fast = c.hunger() >= 20 && c.saturation() > 0;
        Mode mode = food ? Mode.FOOD : fast ? Mode.WELL_FED : Mode.STABLE;
        double interval = food ? Math.min(foodInterval, naturalInterval) : naturalInterval;
        // Preserve elapsed time when food is added/removed. Never postpone an already due natural heal.
        double progress = state.progress() + 1;
        if (progress + 1e-9 < interval) return new Step(new RecoveryState(state.reserve(), progress, mode), 0, 0);
        double naturalAmount = natural ? (fast ? Math.min(c.saturation(), 6) / 6.0 : 1) : 0;
        double foodAmount = food ? Math.min(1, state.reserve()) : 0;
        float heal = (float) Math.min(c.maxHealth() - c.health(), Math.max(foodAmount, naturalAmount));
        double remainder = Math.clamp(progress - interval, 0, Math.min(1, mode.interval()) - 1e-6);
        return new Step(new RecoveryState(state.reserve(), remainder, mode), heal, (float) Math.min(foodAmount, heal));
    }
}
