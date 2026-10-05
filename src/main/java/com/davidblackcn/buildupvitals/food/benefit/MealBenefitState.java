package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** One main benefit. No amplifier stacking, wall clock expiry or secondary slot. */
public record MealBenefitState(Optional<Identifier> type, int remainingTicks) {
    public static final MealBenefitState EMPTY = new MealBenefitState(Optional.empty(), 0);

    public MealBenefitState {
        if (type == null || remainingTicks < 0 || remainingTicks > MealBenefitBalance.MAX_DURATION
                || type.isEmpty() != (remainingTicks == 0)) {
            throw new IllegalArgumentException("Invalid meal benefit state");
        }
    }

    public MealBenefitState grant(Optional<Identifier> incoming, FoodQuality quality) {
        if (incoming.isEmpty() || !MealBenefitRegistry.available(incoming.get())) {
            return this;
        }
        int duration = MealBenefitBalance.duration(quality);
        return new MealBenefitState(incoming, incoming.equals(type) ? Math.max(remainingTicks, duration) : duration);
    }

    public boolean is(MealBenefitType expected) {
        return expected.implemented() && type.filter(expected.id()::equals).isPresent();
    }

    public MealBenefitState tick() {
        return remainingTicks <= 1 || !MealBenefitRegistry.available(type.orElseThrow())
                ? EMPTY : new MealBenefitState(type, remainingTicks - 1);
    }
}
