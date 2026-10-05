package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.food.profile.FoodQuality;

/** Functional prototype values. Balance evaluation is deferred until the core mechanisms are complete. */
public final class MealBenefitBalance {
    public static final int MAX_DURATION = 3600;
    public static final int RESTORATIVE_FOOD_TICKS = 40;
    public static final float INVIGORATED_EXHAUSTION_MULTIPLIER = 0.9F;

    private MealBenefitBalance() { }

    public static int duration(FoodQuality quality) {
        return switch (quality) {
            case BASIC -> 600;
            case PREPARED -> 1200;
            case MEAL -> 2400;
            case FEAST -> MAX_DURATION;
        };
    }
}
