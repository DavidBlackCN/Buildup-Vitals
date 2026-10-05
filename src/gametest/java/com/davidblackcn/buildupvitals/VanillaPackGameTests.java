package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.food.benefit.MealBenefitType;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

/** Functional scenario checks, not measurements of long-session gameplay balance. */
public class VanillaPackGameTests {
    @GameTest
    public void everyVanillaConsumableFoodHasAnOfficialProfile(GameTestHelper helper) {
        var snapshot = FoodProfileLoader.snapshot(helper.getLevel().getServer());
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            var stack = new ItemStack(item);
            if (!id.getNamespace().equals("minecraft") || !stack.has(DataComponents.FOOD)
                    || !stack.has(DataComponents.CONSUMABLE)) continue;
            var match = snapshot.resolve(id);
            helper.assertTrue(!match.fallback(), "Missing official food: " + id);
            helper.assertTrue(match.definition().orElseThrow().source().id().getNamespace().equals("buildup_vitals"),
                    "Test fixture must not replace official food: " + id);
            var profile = match.profile();
            helper.assertTrue(profile.recoveryHealth() <= 4 && profile.quality() != FoodQuality.FEAST,
                    "Vanilla prototype uses modest recovery and no artificial Feast: " + id);
            helper.assertTrue(profile.overrides().hunger().isEmpty() && profile.overrides().saturation().isEmpty(),
                    "Native nutrition is preserved: " + id);
            count++;
        }
        helper.assertTrue(count == 40, "Verified 26.3 food inventory contains 40 items, got " + count);
        helper.assertTrue(snapshot.resolve(BuiltInRegistries.ITEM.getKey(TestFoods.FALLBACK)).fallback(),
                "Unknown mod food remains an honest fallback");
        helper.succeed();
    }

    @GameTest
    public void earlyStaplesAndSteakExpeditionKeepNativeNutrition(GameTestHelper helper) {
        for (Item item : new Item[]{Items.BREAD, Items.BAKED_POTATO, Items.COOKED_BEEF, Items.COOKED_COD}) {
            var player = player(helper);
            var food = new ItemStack(item).get(DataComponents.FOOD);
            for (int meal = 0; meal < 12; meal++) {
                player.getFoodData().setFoodLevel(10);
                player.getFoodData().setSaturation(0);
                new ItemStack(item).finishUsingItem(helper.getLevel(), player);
                helper.assertTrue(player.getFoodData().getFoodLevel() == Math.min(20, 10 + food.nutrition()),
                        "Repeated staples retain nutrition");
                helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - food.saturation()) < 0.0001,
                        "Repeated staples retain saturation");
                helper.assertTrue(PlayerRecovery.state(player).reserve() == 0 && PlayerMealBenefits.state(player).type().isEmpty(),
                        "Basic staples do not invent recovery or buffs");
            }
            helper.assertTrue(PlayerDiet.state(player).variety().foodMultiplier() == 1, "Single-food diet has no penalty");
        }
        helper.succeed();
    }

    @GameTest
    public void farmDietAndCookedVariantsHaveDistinctRoles(GameTestHelper helper) {
        var player = player(helper);
        for (int i = 0; i < 5; i++) {
            new ItemStack(Items.CARROT).finishUsingItem(helper.getLevel(), player);
            new ItemStack(Items.GOLDEN_CARROT).finishUsingItem(helper.getLevel(), player);
        }
        helper.assertTrue(PlayerDiet.state(player).variety().foodMultiplier() == 1, "Gold coating does not create a new diet group");
        for (Item item : new Item[]{Items.BREAD, Items.BAKED_POTATO, Items.APPLE, Items.COOKED_CHICKEN,
                Items.BEETROOT_SOUP, Items.PUMPKIN_PIE, Items.BREAD, Items.APPLE, Items.COOKED_COD, Items.RABBIT_STEW}) {
            new ItemStack(item).finishUsingItem(helper.getLevel(), player);
        }
        helper.assertTrue(PlayerDiet.state(player).variety().foodMultiplier() > 1, "Farm produce and meals earn optional variety rewards");
        helper.assertTrue(PlayerMealBenefits.state(player).is(MealBenefitType.INVIGORATED), "Rabbit stew provides expedition benefit");
        helper.succeed();
    }

    @GameTest
    public void injuredMealKeepsRecoveringAcrossRepeatedHits(GameTestHelper helper) {
        var player = player(helper);
        var remainder = new ItemStack(Items.BEETROOT_SOUP).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(remainder.is(Items.BOWL) && player.getHealth() == 10, "Meal retains bowl and no instant heal");
        for (int i = 0; i < 3; i++) {
            ticks(player, 39);
            // This test advances FoodData only; simulate elapsed entity damage cooldown separately.
            player.damageCooldownTime = 0;
            helper.assertTrue(player.hurtServer(helper.getLevel(), player.damageSources().generic(), 1), "Combat hit is applied");
            ticks(player, 1);
            helper.assertTrue(player.getHealth() == 10, "Each scheduled recovery survives another combat hit");
        }
        helper.assertTrue(PlayerRecovery.state(player).reserve() == 0, "Meal never heals beyond its reserve");
        helper.succeed();
    }

    @GameTest
    public void difficultyKeepsStarvationAndBackgroundRecoveryRules(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var previous = helper.getLevel().getDifficulty();
        try {
            for (var difficulty : Difficulty.values()) {
                server.setDifficulty(difficulty, true);
                var starving = player(helper);
                starving.setHealth(1);
                starving.getFoodData().setFoodLevel(0);
                ticks(starving, 80);
                helper.assertTrue(starving.getHealth() == (difficulty == Difficulty.HARD ? 0 : 1),
                        "Only Hard starvation is lethal: " + difficulty);
                var boundary = player(helper);
                boundary.getFoodData().setFoodLevel(0);
                ticks(boundary, 80);
                helper.assertTrue(boundary.getHealth() == (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD ? 9 : 10),
                        "Easy starvation stops at 10 HP: " + difficulty);
                var fed = player(helper);
                fed.getFoodData().setFoodLevel(20);
                fed.getFoodData().setSaturation(10);
                ticks(fed, 79);
                helper.assertTrue(fed.getHealth() == 10, "No vanilla fast food heal: " + difficulty);
                ticks(fed, 1);
                helper.assertTrue(fed.getHealth() == 11, "Background recovery works: " + difficulty);
            }
        } finally {
            server.setDifficulty(previous, true);
        }
        helper.succeed();
    }
}
