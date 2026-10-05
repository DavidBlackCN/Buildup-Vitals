package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.food.recovery.RecoveryState.Mode;

/** Pure scheduling logic. The caller applies healing and settles only the health actually gained. */
public final class RecoveryController {
    private RecoveryController() { }

    public record Conditions(boolean alive, boolean active, double health, double maxHealth,
                             int hunger, float saturation, boolean naturalEnabled) { }

    public record Step(RecoveryState state, float healing) {
        public RecoveryState settle(float actualHealing) {
            if (state.mode() != Mode.FOOD || healing <= 0 || !Float.isFinite(actualHealing) || actualHealing <= 0) {
                return state;
            }
            return new RecoveryState(Math.max(0, state.reserve() - Math.min(healing, actualHealing)),
                    state.progress(), state.mode());
        }

        public float exhaustion(float actualHealing) {
            return state.mode() != Mode.FOOD && healing > 0 && Float.isFinite(actualHealing)
                    ? Math.max(0, Math.min(healing, actualHealing)) * RecoveryBalance.NATURAL_EXHAUSTION_PER_HP : 0;
        }
    }

    public static Step tick(RecoveryState state, Conditions conditions) {
        if (!conditions.alive()) {
            return new Step(RecoveryState.EMPTY, 0);
        }
        if (!conditions.active() || !Double.isFinite(conditions.health()) || !Double.isFinite(conditions.maxHealth())
                || conditions.health() <= 0 || conditions.health() >= conditions.maxHealth()) {
            return new Step(state.idle(), 0);
        }
        Mode mode = mode(state, conditions);
        if (mode == Mode.NONE) {
            return new Step(state.idle(), 0);
        }
        int progress = (mode == state.mode() ? state.progress() : 0) + 1;
        if (progress < mode.interval()) {
            return new Step(new RecoveryState(state.reserve(), progress, mode), 0);
        }
        double healing = Math.min(1, conditions.maxHealth() - conditions.health());
        if (mode == Mode.FOOD) {
            healing = Math.min(healing, state.reserve());
        }
        return new Step(new RecoveryState(state.reserve(), 0, mode), (float) healing);
    }

    private static Mode mode(RecoveryState state, Conditions conditions) {
        if (state.reserve() > 0) {
            return Mode.FOOD;
        }
        if (!conditions.naturalEnabled() || !Float.isFinite(conditions.saturation())) {
            return Mode.NONE;
        }
        if (conditions.hunger() >= RecoveryBalance.WELL_FED_HUNGER && conditions.saturation() >= RecoveryBalance.WELL_FED_SATURATION) {
            return Mode.WELL_FED;
        }
        if (conditions.hunger() >= RecoveryBalance.STABLE_HUNGER && conditions.saturation() >= RecoveryBalance.STABLE_SATURATION) {
            return Mode.STABLE;
        }
        return Mode.NONE;
    }
}
