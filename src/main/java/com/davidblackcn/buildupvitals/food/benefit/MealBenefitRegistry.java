package com.davidblackcn.buildupvitals.food.benefit;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

/** Fixed built-in types for Stage 3. Profiles reference IDs; no dynamic behavior registration. */
public final class MealBenefitRegistry {
    private static final Map<Identifier, MealBenefitType> TYPES = Arrays.stream(MealBenefitType.values())
            .collect(Collectors.toUnmodifiableMap(MealBenefitType::id, type -> type));

    private MealBenefitRegistry() { }

    public static Optional<MealBenefitType> find(Identifier id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    public static boolean available(Identifier id) {
        return find(id).filter(MealBenefitType::implemented).isPresent();
    }
}
