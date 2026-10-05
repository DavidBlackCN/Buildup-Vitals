package com.davidblackcn.buildupvitals.network;

import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import com.davidblackcn.buildupvitals.food.profile.ProfileSnapshot;
import com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Display metadata only: no player state or client-authoritative gameplay. */
public record TooltipProfile(FoodQuality quality, double recovery, List<DietCategory> categories,
                             Optional<Identifier> benefit, Identifier group, Optional<Identifier> profileId, Hydration hydration) {
    public TooltipProfile {
        Objects.requireNonNull(quality);
        Objects.requireNonNull(benefit);
        Objects.requireNonNull(group);
        Objects.requireNonNull(profileId);
        Objects.requireNonNull(hydration);
        if (hydration.thirst() < 0 || hydration.quenched() < 0) throw new IllegalArgumentException("Negative hydration");
        categories = List.copyOf(categories);
        if (!Double.isFinite(recovery) || recovery < 0 || categories.stream().distinct().count() != categories.size()) {
            throw new IllegalArgumentException("Invalid tooltip profile");
        }
    }

    public TooltipProfile(FoodQuality quality, double recovery, List<DietCategory> categories,
                          Optional<Identifier> benefit, Identifier group, Optional<Identifier> profileId) {
        this(quality, recovery, categories, benefit, group, profileId, new Hydration(0, 0));
    }

    public static TooltipProfile from(Identifier item, ProfileSnapshot.Match match) {
        var profile = match.profile();
        return new TooltipProfile(profile.quality(), profile.recoveryHealth(), profile.categories(), profile.mealBenefit(),
                profile.varietyGroupFor(item), match.definition().map(definition -> definition.source().id()), profile.hydration());
    }

    public static TooltipProfile fallback(Identifier item) {
        return new TooltipProfile(FoodQuality.BASIC, 0, List.of(), Optional.empty(), item, Optional.empty());
    }
}
