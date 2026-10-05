package com.davidblackcn.buildupvitals.diet;

import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodProfile;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Snapshot at consumption time. Reloading profiles never rewrites the player's history. */
public record DietEntry(Identifier foodId, FoodQuality quality, List<DietCategory> categories,
                        Identifier varietyGroup, long timestamp) {
    public DietEntry {
        Objects.requireNonNull(foodId);
        Objects.requireNonNull(quality);
        Objects.requireNonNull(varietyGroup);
        categories = List.copyOf(categories);
        if (categories.stream().distinct().count() != categories.size()) {
            throw new IllegalArgumentException("Duplicate diet categories");
        }
    }

    public static DietEntry consumed(Identifier foodId, FoodProfile profile, long timestamp) {
        return new DietEntry(foodId, profile.quality(), profile.categories(), profile.varietyGroupFor(foodId), timestamp);
    }
}
