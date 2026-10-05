package com.davidblackcn.buildupvitals.diet;

import com.davidblackcn.buildupvitals.config.CoreBalance;

/** Stable entry point; numerical defaults live in CoreBalance. */
public final class VarietyBalance {
    public static final int WINDOW = CoreBalance.Variety.WINDOW;
    public static final double CATEGORY_WEIGHT = CoreBalance.Variety.CATEGORY_WEIGHT;
    public static final double GROUP_WEIGHT = CoreBalance.Variety.GROUP_WEIGHT;
    public static final double QUALITY_WEIGHT = CoreBalance.Variety.QUALITY_WEIGHT;
    public static final double TARGET_DIVERSITY = CoreBalance.Variety.TARGET_DIVERSITY;
    public static final double MIN_REPEAT_FACTOR = CoreBalance.Variety.MIN_REPEAT_FACTOR;
    public static final double MAX_FOOD_BONUS = CoreBalance.Variety.MAX_FOOD_BONUS;
    public static final double MAX_BENEFIT_BONUS = CoreBalance.Variety.MAX_BENEFIT_BONUS;
    public static final double MAX_WELL_FED_BONUS = CoreBalance.Variety.MAX_WELL_FED_BONUS;

    private VarietyBalance() { }
}
