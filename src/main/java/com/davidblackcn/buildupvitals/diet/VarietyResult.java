package com.davidblackcn.buildupvitals.diet;

import com.davidblackcn.buildupvitals.food.recovery.RecoveryBalance;

/** Repeat reduction applies to the bonus above 1.0, never to baseline food value. */
public record VarietyResult(double categoryDiversity, double groupDiversity, double qualityWeight,
                            double score, double repeatFactor) {
    public static final VarietyResult BASELINE = new VarietyResult(0, 0, 0, 0, 1);

    public double foodMultiplier() { return 1 + VarietyBalance.MAX_FOOD_BONUS * score * repeatFactor; }
    public double benefitMultiplier() { return 1 + VarietyBalance.MAX_BENEFIT_BONUS * score * repeatFactor; }
    public double wellFedMultiplier() { return 1 + VarietyBalance.MAX_WELL_FED_BONUS * score * repeatFactor; }

    public int wellFedInterval() {
        // Ceiling keeps the actual speed increase at or below the configured bound.
        return (int) Math.ceil(RecoveryBalance.WELL_FED_TICKS / wellFedMultiplier());
    }
}
