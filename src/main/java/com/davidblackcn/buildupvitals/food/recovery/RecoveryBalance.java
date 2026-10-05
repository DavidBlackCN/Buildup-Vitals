package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.config.CoreBalance;

/** Stable entry point; numerical defaults live in CoreBalance. */
public final class RecoveryBalance {
    public static final double MAX_RESERVE = CoreBalance.Recovery.MAX_RESERVE;
    public static final double MIN_RESERVE = CoreBalance.Recovery.MIN_RESERVE;
    public static final int FOOD_TICKS = CoreBalance.Recovery.FOOD_TICKS;
    public static final int STABLE_TICKS = CoreBalance.Recovery.STABLE_TICKS;
    public static final int WELL_FED_TICKS = CoreBalance.Recovery.WELL_FED_TICKS;
    public static final int STABLE_HUNGER = CoreBalance.Recovery.STABLE_HUNGER;
    public static final float STABLE_SATURATION = CoreBalance.Recovery.STABLE_SATURATION;
    public static final int WELL_FED_HUNGER = CoreBalance.Recovery.WELL_FED_HUNGER;
    public static final float WELL_FED_SATURATION = CoreBalance.Recovery.WELL_FED_SATURATION;
    public static final float NATURAL_EXHAUSTION_PER_HP = CoreBalance.Recovery.NATURAL_EXHAUSTION_PER_HP;

    private RecoveryBalance() { }
}
