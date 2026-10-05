package com.davidblackcn.buildupvitals.food.recovery;

/** Stage 2 prototype values, in HP and server ticks (20 ticks per second). */
public final class RecoveryBalance {
    public static final double MAX_RESERVE = 20;
    public static final double MIN_RESERVE = 0.000001;
    public static final int FOOD_TICKS = 50;
    public static final int STABLE_TICKS = 120;
    public static final int WELL_FED_TICKS = 80;
    public static final int STABLE_HUNGER = 18;
    public static final float STABLE_SATURATION = 1;
    public static final int WELL_FED_HUNGER = 20;
    public static final float WELL_FED_SATURATION = 6;
    public static final float NATURAL_EXHAUSTION_PER_HP = 6;

    private RecoveryBalance() { }
}
