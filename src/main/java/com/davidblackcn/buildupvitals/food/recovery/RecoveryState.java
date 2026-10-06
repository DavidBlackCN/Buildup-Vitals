package com.davidblackcn.buildupvitals.food.recovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;

/** Immutable, server-owned short-term state. No wall clock or damage cooldown. */
public record RecoveryState(double reserve, double progress, Mode mode) {
    private static final Codec<RecoveryState.Mode> MODE = Codec.STRING.comapFlatMap(value -> {
        for (var mode : RecoveryState.Mode.values()) {
            if (mode.name().toLowerCase(Locale.ROOT).equals(value)) {
                return DataResult.success(mode);
            }
        }
        return DataResult.error(() -> "Unknown recovery mode: " + value);
    }, mode -> mode.name().toLowerCase(Locale.ROOT));

    public static final Codec<RecoveryState> CODEC = RecordCodecBuilder.<RecoveryState>create(instance -> instance.group(
            Codec.doubleRange(0, RecoveryBalance.MAX_RESERVE)
                    .validate(value -> Double.isFinite(value) ? DataResult.success(value) : DataResult.error(() -> "Non-finite reserve"))
                    .fieldOf("reserve").forGetter(RecoveryState::reserve),
            Codec.doubleRange(0, 119)
                    .validate(value -> Double.isFinite(value) ? DataResult.success(value) : DataResult.error(() -> "Non-finite progress"))
                    .fieldOf("progress").forGetter(RecoveryState::progress),
            MODE.fieldOf("mode").forGetter(RecoveryState::mode)
    ).apply(instance, (reserve, progress, mode) -> new RecoveryState(reserve,
            progress >= mode.interval() ? mode.interval() - 1 : progress, mode)));

    public static final RecoveryState EMPTY = new RecoveryState(0, 0, Mode.NONE);

    public RecoveryState {
        if (!Double.isFinite(reserve) || reserve < 0 || reserve > RecoveryBalance.MAX_RESERVE
                || mode == null || !Double.isFinite(progress) || progress < 0 || progress >= mode.interval()) {
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
        return new RecoveryState(added, Math.min(progress, Mode.FOOD.interval() - 0.000001), Mode.FOOD);
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
