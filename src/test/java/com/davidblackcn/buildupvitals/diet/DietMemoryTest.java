package com.davidblackcn.buildupvitals.diet;

import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DietMemoryTest {
    private static DietEntry entry(int item, int group, FoodQuality quality, DietCategory... categories) {
        return new DietEntry(Identifier.parse("test:item_" + item), quality, List.of(categories), Identifier.parse("test:group_" + group), item);
    }

    @Test
    void windowRollsByMealsRatherThanTimeAndSnapshotsAreImmutable() {
        var memory = DietMemory.EMPTY;
        for (int i = 0; i < 15; i++) memory = memory.append(entry(i, i, FoodQuality.BASIC, DietCategory.FRUIT));
        assertEquals(10, memory.entries().size());
        assertEquals(5, memory.entries().getFirst().timestamp());
        assertEquals(14, memory.entries().getLast().timestamp());
        var before = memory;
        var sameTime = new DietEntry(Identifier.parse("test:new"), FoodQuality.BASIC, List.of(), Identifier.parse("test:new"), 14);
        memory = memory.append(sameTime);
        assertEquals(6, memory.entries().getFirst().timestamp());
        assertEquals(5, before.entries().getFirst().timestamp());
        var immutable = memory;
        assertThrows(UnsupportedOperationException.class, () -> immutable.entries().clear());
        assertEquals(DietMemory.EMPTY, new DietMemory(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DietMemory(java.util.Collections.nCopies(11, sameTime)));
    }

    @Test
    void repeatedFoodAndDifferentIdsInSameGroupRemainBaseline() {
        for (boolean differentIds : new boolean[]{false, true}) {
            var memory = DietMemory.EMPTY;
            for (int i = 0; i < 10; i++) memory = memory.append(entry(differentIds ? i : 0, 0, FoodQuality.FEAST,
                    DietCategory.PROTEIN, DietCategory.GRAIN, DietCategory.VEGETABLE));
            var result = memory.variety();
            assertEquals(0, result.score());
            assertEquals(0.9, result.repeatFactor(), 1e-12);
            assertEquals(1, result.foodMultiplier());
            assertEquals(1, result.benefitMultiplier());
            assertEquals(11, result.wellFedInterval());
        }
    }

    @Test
    void fruitVarietyHasSmallRewardAndMixedCategoriesMatterMoreThanQuality() {
        var fruit = DietMemory.EMPTY;
        var mixed = DietMemory.EMPTY;
        var fancyFruit = DietMemory.EMPTY;
        var categories = new DietCategory[]{DietCategory.PROTEIN, DietCategory.GRAIN, DietCategory.VEGETABLE, DietCategory.FRUIT};
        for (int i = 0; i < 10; i++) {
            fruit = fruit.append(entry(i, i, FoodQuality.BASIC, DietCategory.FRUIT));
            fancyFruit = fancyFruit.append(entry(i, i, FoodQuality.FEAST, DietCategory.FRUIT));
            mixed = mixed.append(entry(i, i, FoodQuality.BASIC, categories[i % 4]));
        }
        assertEquals(0, fruit.variety().categoryDiversity());
        assertTrue(fruit.variety().foodMultiplier() > 1);
        assertTrue(fruit.variety().foodMultiplier() < 1.05);
        assertTrue(mixed.variety().score() > fancyFruit.variety().score());
    }

    @Test
    void multiCategoryMealAddsCoverageButCountsAsOneMealAndOneGroup() {
        var ordinary = DietMemory.EMPTY;
        var mixed = DietMemory.EMPTY;
        for (int i = 0; i < 10; i++) {
            ordinary = ordinary.append(entry(i, i, FoodQuality.MEAL, DietCategory.FRUIT));
            mixed = mixed.append(entry(i, i, FoodQuality.MEAL, DietCategory.PROTEIN, DietCategory.GRAIN, DietCategory.VEGETABLE));
        }
        assertEquals(ordinary.variety().groupDiversity(), mixed.variety().groupDiversity());
        assertTrue(mixed.variety().categoryDiversity() > ordinary.variety().categoryDiversity());
        assertEquals(10, mixed.entries().size());
    }

    @Test
    void repetitionReducesOnlyBonusAndPushesOutPreviousDiversity() {
        var memory = DietMemory.EMPTY;
        for (int i = 0; i < 10; i++) memory = memory.append(entry(i, i, FoodQuality.MEAL, DietCategory.values()[i % 6]));
        double diverse = memory.variety().foodMultiplier();
        for (int i = 0; i < 10; i++) {
            memory = memory.append(entry(0, 0, FoodQuality.BASIC, DietCategory.PROTEIN));
            assertTrue(memory.variety().foodMultiplier() >= 1);
        }
        assertEquals(1, memory.variety().foodMultiplier());
        assertTrue(diverse > memory.variety().foodMultiplier());
        assertEquals(1, memory.entries().stream().map(DietEntry::foodId).distinct().count());
    }

    @Test
    void sameFoodStillCountsAsRepeatedIfDatapackChangesItsGroup() {
        var memory = DietMemory.EMPTY;
        for (int i = 0; i < 10; i++) memory = memory.append(entry(0, i, FoodQuality.BASIC, DietCategory.FRUIT));
        assertEquals(0.9, memory.variety().repeatFactor(), 1e-12);
        assertEquals(1 + 0.15 * memory.variety().score() * 0.9, memory.variety().foodMultiplier(), 1e-12);
    }

    @Test
    void unknownCategoriesDoNotInventCategoryCoverageAndShortHistoryGrowsGradually() {
        var memory = DietMemory.EMPTY;
        for (int i = 0; i < 10; i++) memory = memory.append(entry(i, i, FoodQuality.BASIC));
        assertEquals(0, memory.variety().categoryDiversity());
        assertEquals(0, memory.variety().qualityWeight());
        assertEquals(0.25, memory.variety().score(), 1e-12);
        var shortHistory = new DietMemory(memory.entries().subList(0, 2));
        assertTrue(shortHistory.variety().score() < memory.variety().score());
        assertEquals(1, DietMemory.EMPTY.variety().foodMultiplier());
    }

    @Test
    void randomizedWindowsStayWithinRewardOnlyBounds() {
        var random = new Random(93481);
        for (int trial = 0; trial < 1000; trial++) {
            var memory = DietMemory.EMPTY;
            for (int i = 0; i < random.nextInt(20) + 1; i++) {
                var categories = new ArrayList<DietCategory>();
                for (var category : DietCategory.values()) if (random.nextBoolean()) categories.add(category);
                memory = memory.append(new DietEntry(Identifier.parse("test:item_" + random.nextInt(4)),
                        FoodQuality.values()[random.nextInt(4)], categories, Identifier.parse("test:group_" + random.nextInt(4)), i));
            }
            var result = memory.variety();
            assertTrue(result.foodMultiplier() >= 1 && result.foodMultiplier() <= 1.15);
            assertTrue(result.benefitMultiplier() >= 1 && result.benefitMultiplier() <= 1.10);
            assertTrue(result.wellFedMultiplier() >= 1 && result.wellFedMultiplier() <= 1.075);
            assertTrue(result.repeatFactor() >= 0.9 && result.repeatFactor() <= 1);
            assertEquals(11, result.wellFedInterval());
        }
    }
}
