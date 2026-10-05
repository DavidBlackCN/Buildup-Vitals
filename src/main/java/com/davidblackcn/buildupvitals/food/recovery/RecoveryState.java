package com.davidblackcn.buildupvitals.food.recovery;

/** Immutable, server-owned short-term state. No wall clock or damage cooldown. */
public record RecoveryState(double reserve, int progress, Mode mode) {
    public static final RecoveryState EMPTY = new RecoveryState(0, 0, Mode.NONE);

    public RecoveryState {
        if (!Double.isFinite(reserve) || reserve < 0 || reserve > RecoveryBalance.MAX_RESERVE
                || mode == null || progress < 0 || progress >= mode.interval()) {
            throw new IllegalArgumentException("Invalid recovery state");
        }
        if (reserve < RecoveryBalance.MIN_RESERVE) {
            reserve = 0;
        }
    }

    public RecoveryState addFood(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return this;
        }
        double added = Math.min(RecoveryBalance.MAX_RESERVE, reserve + Math.min(amount, RecoveryBalance.MAX_RESERVE));
        if (added < RecoveryBalance.MIN_RESERVE) {
            return this;
        }
        return new RecoveryState(added, mode == Mode.FOOD ? progress : 0, Mode.FOOD);
    }

    public RecoveryState idle() {
        return reserve == 0 ? EMPTY : new RecoveryState(reserve, 0, Mode.NONE);
    }

    public enum Mode {
        NONE(1), FOOD(RecoveryBalance.FOOD_TICKS), STABLE(RecoveryBalance.STABLE_TICKS), WELL_FED(RecoveryBalance.WELL_FED_TICKS);

        private final int interval;

        Mode(int interval) {
            this.interval = interval;
        }

        public int interval() {
            return interval;
        }
    }
}
