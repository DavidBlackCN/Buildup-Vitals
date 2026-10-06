package com.davidblackcn.buildupvitals.food.profile;

import java.util.Locale;
import com.davidblackcn.buildupvitals.config.CoreBalance;

/** Explicit tiers override duration; an absent tier preserves the native duration. */
public enum ConsumptionSpeed {
    NORMAL(32), QUICK(21), FAST(16);
    private final int ticks;
    ConsumptionSpeed(int ticks) { this.ticks = ticks; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public int ticks() { return ticks; }
    public static int duration(java.util.Optional<ConsumptionSpeed> tier, int original, boolean overfull) {
        int base = tier.map(ConsumptionSpeed::ticks).orElse(original);
        return overfull ? (int) Math.min(Integer.MAX_VALUE, Math.ceil(base * CoreBalance.Overeat.DURATION_MULTIPLIER)) : base;
    }
}
