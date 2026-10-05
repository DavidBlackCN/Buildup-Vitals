package com.davidblackcn.buildupvitals.diet;

import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class VarietyCalculator {
    private VarietyCalculator() { }

    public static VarietyResult calculate(List<DietEntry> entries) {
        if (entries.isEmpty()) return VarietyResult.BASELINE;
        var categories = new EnumMap<DietCategory, Double>(DietCategory.class);
        var groups = new HashMap<Identifier, Double>();
        int knownCategories = 0;
        double quality = 0;
        for (var entry : entries) {
            groups.merge(entry.varietyGroup(), 1.0, Double::sum);
            if (!entry.categories().isEmpty()) {
                knownCategories++;
                double share = 1.0 / entry.categories().size();
                for (var category : entry.categories()) categories.merge(category, share, Double::sum);
            }
            quality += qualityWeight(entry.quality());
        }
        double effectiveGroups = effectiveCount(groups.values());
        double groupDiversity = diversity(effectiveGroups);
        double categoryDiversity = diversity(effectiveCount(categories.values())) * knownCategories / entries.size();
        quality /= entries.size();
        // A single group, including a repeated multi-category feast, remains exactly baseline.
        double gate = clamp(effectiveGroups - 1);
        double fullness = Math.min(1, entries.size() / (double) VarietyBalance.WINDOW);
        double score = clamp(fullness * (VarietyBalance.CATEGORY_WEIGHT * categoryDiversity * gate
                + VarietyBalance.GROUP_WEIGHT * groupDiversity
                + VarietyBalance.QUALITY_WEIGHT * quality * groupDiversity));
        var newest = entries.getLast();
        long repeated = entries.stream().filter(entry -> entry.foodId().equals(newest.foodId())
                || entry.varietyGroup().equals(newest.varietyGroup())).count();
        double repeatFactor = 1 - (1 - VarietyBalance.MIN_REPEAT_FACTOR)
                * Math.min(1, (repeated - 1.0) / (VarietyBalance.WINDOW - 1));
        return new VarietyResult(categoryDiversity, groupDiversity, quality, score, repeatFactor);
    }

    private static double effectiveCount(Collection<Double> masses) {
        double total = masses.stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return 0;
        double squares = masses.stream().mapToDouble(value -> value * value).sum();
        return total * total / squares;
    }

    private static double diversity(double effectiveCount) {
        return clamp((effectiveCount - 1) / (VarietyBalance.TARGET_DIVERSITY - 1));
    }

    private static double clamp(double value) { return Math.clamp(value, 0, 1); }

    private static double qualityWeight(FoodQuality quality) {
        return switch (quality) {
            case BASIC -> 0;
            case PREPARED -> 1.0 / 3;
            case MEAL -> 2.0 / 3;
            case FEAST -> 1;
        };
    }
}
