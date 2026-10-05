package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.diet.DietMemory;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.player.DietAttachments;
import com.davidblackcn.buildupvitals.player.MealBenefitAttachments;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

public class DietGameTests {
    static void mixedMeals(ServerPlayer player) {
        for (Item item : new Item[]{Items.COOKED_BEEF, Items.BREAD, Items.MUSHROOM_STEW, Items.APPLE, Items.PUMPKIN_PIE,
                Items.COOKED_BEEF, Items.BREAD, Items.MUSHROOM_STEW, Items.APPLE, Items.PUMPKIN_PIE}) {
            new ItemStack(item).finishUsingItem(player.level(), player);
        }
    }

    @GameTest
    public void finishedMealsRecordBoundedSnapshotsIncludingFallback(GameTestHelper helper) {
        var player = player(helper);
        helper.assertTrue(PlayerDiet.state(player).entries().isEmpty(), "Holding food does not record a meal");
        mixedMeals(player);
        var memory = PlayerDiet.state(player);
        helper.assertTrue(memory.entries().size() == 10, "Each completed food contributes exactly one entry");
        var pie = memory.entries().getLast();
        helper.assertTrue(pie.quality() == FoodQuality.PREPARED && pie.categories().contains(DietCategory.GRAIN)
                && pie.categories().contains(DietCategory.SWEET), "Quality and all categories are snapshotted");
        helper.assertTrue(pie.timestamp() == helper.getLevel().getServer().overworld().getGameTime(), "Timestamp uses shared overworld game ticks");
        new ItemStack(Items.BAKED_POTATO).finishUsingItem(helper.getLevel(), player);
        memory = PlayerDiet.state(player);
        helper.assertTrue(memory.entries().size() == 10 && memory.entries().getFirst().foodId().equals(Identifier.parse("minecraft:bread")),
                "Eleventh meal immediately evicts the oldest without a time delay");
        var fallback = memory.entries().getLast();
        helper.assertTrue(fallback.quality() == FoodQuality.BASIC && fallback.categories().isEmpty()
                && fallback.varietyGroup().equals(Identifier.parse("minecraft:baked_potato")), "Fallback food records honest metadata");
        helper.succeed();
    }

    @GameTest
    public void diverseDietRewardsFoodAndBenefitButNotNutrition(GameTestHelper helper) {
        var diverse = player(helper);
        var baseline = player(helper);
        mixedMeals(diverse);
        diverse.removeAttached(RecoveryAttachments.RECOVERY);
        diverse.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
        for (var player : new ServerPlayer[]{diverse, baseline}) {
            player.setHealth(10);
            player.getFoodData().setFoodLevel(5);
            player.getFoodData().setSaturation(0);
            new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), player);
        }
        double multiplier = PlayerDiet.state(diverse).variety().foodMultiplier();
        helper.assertTrue(multiplier > 1 && multiplier <= 1.15, "Diverse food bonus is positive and bounded");
        helper.assertTrue(Math.abs(PlayerRecovery.state(diverse).reserve() - 3 * multiplier) < 1e-10, "This completed meal receives the derived recovery bonus once");
        helper.assertTrue(PlayerRecovery.state(baseline).reserve() == 3, "Single food retains full base recovery");
        helper.assertTrue(PlayerMealBenefits.state(diverse).remainingTicks()
                == (int) Math.floor(2400 * PlayerDiet.state(diverse).variety().benefitMultiplier()), "Benefit receives only the duration bonus");
        helper.assertTrue(diverse.getHealth() == 10 && baseline.getHealth() == 10, "No instant variety healing");
        helper.assertTrue(diverse.getFoodData().getFoodLevel() == baseline.getFoodData().getFoodLevel()
                && diverse.getFoodData().getSaturationLevel() == baseline.getFoodData().getSaturationLevel(), "Hunger and saturation are independent of diet history");
        helper.succeed();
    }

    @GameTest
    public void varietyOnlySpeedsWellFedWithoutDiscountingExhaustion(GameTestHelper helper) {
        var player = player(helper);
        mixedMeals(player);
        player.removeAttached(RecoveryAttachments.RECOVERY);
        player.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
        player.setHealth(10);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(10);
        int interval = PlayerDiet.state(player).variety().wellFedInterval();
        helper.assertTrue(interval < 80 && interval >= 75, "Diverse diet has bounded Well-fed speed bonus");
        ticks(player, interval - 1);
        helper.assertTrue(player.getHealth() == 10, "No early healing");
        ticks(player, 1);
        helper.assertTrue(player.getHealth() == 11, "Well-fed uses derived interval");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.getFoodData().addAdditionalSaveData(output);
        helper.assertTrue(output.buildResult().getFloatOr("foodExhaustionLevel", -1) == 6, "Natural recovery still pays 6 exhaustion per HP");
        helper.succeed();
    }

    @GameTest
    public void repetitionKeepsBasicFoodReliableAndOtherPlayersIndependent(GameTestHelper helper) {
        var player = player(helper);
        mixedMeals(player);
        for (int i = 0; i < 10; i++) {
            player.getFoodData().setFoodLevel(10);
            player.getFoodData().setSaturation(0);
            new ItemStack(Items.COOKED_BEEF).finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(player.getFoodData().getFoodLevel() == 18
                    && Math.abs(player.getFoodData().getSaturationLevel() - 12.8F) < 0.0001F, "Repeated beef retains all vanilla nutrition");
        }
        var memory = PlayerDiet.state(player);
        helper.assertTrue(memory.variety().foodMultiplier() == 1 && memory.variety().benefitMultiplier() == 1
                && memory.variety().wellFedInterval() == 80, "Repeated basic diet returns to full baseline");
        helper.assertTrue(PlayerDiet.state(player(helper)).equals(DietMemory.EMPTY), "No history leaks to other players");
        for (var mode : new GameType[]{GameType.CREATIVE, GameType.SPECTATOR}) {
            var inactive = player(helper, mode);
            new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), inactive);
            helper.assertTrue(PlayerDiet.state(inactive).equals(DietMemory.EMPTY), "Inactive players do not record meals");
        }
        helper.succeed();
    }

    @GameTest
    public void saveLoadAliveTransferAndDeath(GameTestHelper helper) {
        var original = player(helper);
        mixedMeals(original);
        var expected = PlayerDiet.state(original);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        var loaded = player(helper);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(PlayerDiet.state(loaded).equals(expected), "Every entry and timestamp survives player NBT");
        helper.assertTrue(PlayerDiet.state(loaded).variety().equals(expected.variety()), "Derived score is recomputed consistently");
        var transfer = player(helper);
        transfer.restoreFrom(loaded, true);
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(loaded, transfer, true);
        helper.assertTrue(PlayerDiet.state(transfer).equals(expected), "Alive replacement preserves history");
        var respawned = player(helper);
        respawned.restoreFrom(loaded, false);
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(loaded, respawned, false);
        helper.assertTrue(PlayerDiet.state(respawned).equals(DietMemory.EMPTY), "Death replacement does not copy diet");
        helper.succeed();
    }

    @GameTest
    public void malformedSavedHistoryIsRejectedWithoutThrowing(GameTestHelper helper) {
        var player = player(helper);
        mixedMeals(player);
        var encoded = DietAttachments.CODEC.encodeStart(JsonOps.INSTANCE, PlayerDiet.state(player)).getOrThrow().getAsJsonObject();
        var entries = encoded.getAsJsonArray("entries");
        entries.add(entries.get(0).deepCopy());
        helper.assertTrue(DietAttachments.CODEC.parse(JsonOps.INSTANCE, encoded).isError(), "Oversized saved window is rejected");
        entries.remove(entries.size() - 1);
        entries.get(0).getAsJsonObject().add("categories", JsonParser.parseString("[\"fruit\",\"fruit\"]"));
        helper.assertTrue(DietAttachments.CODEC.parse(JsonOps.INSTANCE, encoded).isError(), "Duplicate categories are rejected by codec before construction");
        entries.get(0).getAsJsonObject().add("categories", new JsonArray());
        entries.get(0).getAsJsonObject().addProperty("quality", "not_a_quality");
        helper.assertTrue(DietAttachments.CODEC.parse(JsonOps.INSTANCE, encoded).isError(), "Unknown quality is rejected");
        helper.succeed();
    }
}
