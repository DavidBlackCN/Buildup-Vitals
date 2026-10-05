package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.config.CoreBalance;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;

/** Stable entry point; numerical defaults live in CoreBalance. */
public final class MealBenefitBalance {
    public static final int MAX_DURATION = CoreBalance.Benefits.MAX_DURATION;
    public static final int RESTORATIVE_FOOD_TICKS = CoreBalance.Benefits.RESTORATIVE_FOOD_TICKS;
    public static final float INVIGORATED_EXHAUSTION_MULTIPLIER = CoreBalance.Benefits.INVIGORATED_EXHAUSTION_MULTIPLIER;

    private MealBenefitBalance() { }

    public static int duration(FoodQuality quality) {
        return switch (quality) {
            case BASIC -> CoreBalance.Benefits.BASIC_DURATION;
            case PREPARED -> CoreBalance.Benefits.PREPARED_DURATION;
            case MEAL -> CoreBalance.Benefits.MEAL_DURATION;
            case FEAST -> MAX_DURATION;
        };
    }
}
