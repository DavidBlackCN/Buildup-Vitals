package com.davidblackcn.buildupvitals.food.profile;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.resources.Identifier;

/** Food metadata. Stage 4 also snapshots quality, categories and variety group into diet memory. */
public record FoodProfile(
        FoodQuality quality,
        double recoveryHealth,
        Hydration hydration,
        List<DietCategory> categories,
        Optional<Identifier> varietyGroup,
        List<String> traits,
        Optional<Identifier> mealBenefit,
        Overrides overrides,
        Optional<ConsumptionSpeed> consumptionSpeed
) {
    public FoodProfile(FoodQuality quality, double recoveryHealth, Hydration hydration, List<DietCategory> categories,
                       Optional<Identifier> varietyGroup, List<String> traits, Optional<Identifier> mealBenefit, Overrides overrides) {
        this(quality, recoveryHealth, hydration, categories, varietyGroup, traits, mealBenefit, overrides, Optional.empty());
    }

    public FoodProfile {
        java.util.Objects.requireNonNull(consumptionSpeed);
        categories = List.copyOf(categories);
        traits = List.copyOf(traits);
    }

    public static FoodProfile fallback() {
        return new FoodProfile(FoodQuality.BASIC, 0, new Hydration(0, 0), List.of(),
                Optional.empty(), List.of(), Optional.empty(), Overrides.NONE);
    }

    public Identifier varietyGroupFor(Identifier item) {
        return varietyGroup.orElse(item);
    }

    public record Hydration(int thirst, int quenched) { }

    /** Optional absolute food points, not vanilla's saturation modifier. Reserved for future balancing. */
    public record Overrides(OptionalInt hunger, OptionalDouble saturation) {
        public static final Overrides NONE = new Overrides(OptionalInt.empty(), OptionalDouble.empty());
    }
}
