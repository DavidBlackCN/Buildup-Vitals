package com.davidblackcn.buildupvitals.diet;

/** Mechanism prototype values; gameplay balancing is deferred until the core is complete. */
public final class VarietyBalance {
    public static final int WINDOW = 10;
    public static final double CATEGORY_WEIGHT = 0.65;
    public static final double GROUP_WEIGHT = 0.25;
    public static final double QUALITY_WEIGHT = 0.10;
    public static final double TARGET_DIVERSITY = 4;
    public static final double MIN_REPEAT_FACTOR = 0.90;
    public static final double MAX_FOOD_BONUS = 0.15;
    public static final double MAX_BENEFIT_BONUS = 0.10;
    public static final double MAX_WELL_FED_BONUS = 0.075;

    private VarietyBalance() { }
}
