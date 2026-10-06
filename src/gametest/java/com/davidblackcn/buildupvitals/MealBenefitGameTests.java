package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.food.benefit.MealBenefitState;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.food.benefit.MealBenefitType;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.player.MealBenefitAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import java.util.Optional;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

/** Actual food consumption, vanilla exhaustion call sites and attachment lifecycle. */
public class MealBenefitGameTests {
    @GameTest
    public void unavailableReferencesRetainOtherProfileFields(GameTestHelper helper) {
        var player = player(helper);
        grant(player, MealBenefitType.RESTORATIVE, 200);
        new ItemStack(TestFoods.UNKNOWN).finishUsingItem(helper.getLevel(), player);
        new ItemStack(TestFoods.EXPERIMENTAL).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(Math.abs(PlayerRecovery.state(player).reserve() - (2 + PlayerDiet.state(player).variety().foodMultiplier())) < 1e-10,
                "Unavailable benefits do not discard recovery fields or variety bonus");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 14, "Unavailable benefits do not break eating");
        helper.assertTrue(PlayerMealBenefits.state(player).is(MealBenefitType.RESTORATIVE)
                && PlayerMealBenefits.state(player).remainingTicks() == 200, "Unknown and experimental IDs do not replace or refresh active benefit");
        helper.succeed();
    }

    private static void grant(ServerPlayer player, MealBenefitType type, int ticks) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.davidblackcn.buildupvitals.effect.BuildupEffects.forType(type), ticks, 0, false, false, true));
    }

    private static float exhaustion(ServerPlayer player) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        player.getFoodData().addAdditionalSaveData(output);
        return output.buildResult().getFloat("foodExhaustionLevel").orElseThrow();
    }

    private static void near(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001F, message + ": " + actual + " vs " + expected);
    }

    @GameTest
    public void foodGrantsRefreshesAndReplacesWithoutPotionEffects(GameTestHelper helper) {
        var player = player(helper);
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(PlayerMealBenefits.state(player).is(MealBenefitType.RESTORATIVE), "Meal grants Restorative");
        helper.assertTrue(PlayerMealBenefits.state(player).remainingTicks() == 2400, "Meal duration");
        ticks(player, 20);
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(PlayerMealBenefits.state(player).remainingTicks() == 2400, "Same type refreshes, never adds");
        new ItemStack(Items.PUMPKIN_PIE).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(PlayerMealBenefits.state(player).is(MealBenefitType.INVIGORATED)
                && PlayerMealBenefits.state(player).remainingTicks() == (int) Math.floor(1200 * PlayerDiet.state(player).variety().benefitMultiplier()),
                "Prepared benefit replaces the main slot including the variety duration bonus");
        new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(PlayerMealBenefits.state(player).is(MealBenefitType.INVIGORATED), "Basic apple does not remove benefits");
        helper.assertTrue(player.getActiveEffects().size() == 1 && !player.getActiveEffects().iterator().next().isVisible(), "Main benefit is a visible icon without particles");
        new ItemStack(Items.GOLDEN_APPLE).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION) && PlayerMealBenefits.state(player).is(MealBenefitType.INVIGORATED),
                "Vanilla effects and main benefit coexist");
        var other = player(helper);
        new ItemStack(Items.PUMPKIN_PIE).finishUsingItem(helper.getLevel(), other);
        helper.assertTrue(other.getFoodData().getFoodLevel() == 18, "Pie retains vanilla nutrition");
        near(helper, other.getFoodData().getSaturationLevel(), 4.8F, "Pie retains vanilla saturation");
        helper.succeed();
    }

    @GameTest
    public void restorativeAcceleratesWithoutCreatingExtraHealing(GameTestHelper helper) {
        var faster = player(helper);
        var baseline = player(helper);
        for (var player : new ServerPlayer[]{faster, baseline}) new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), player);
        baseline.removeAllEffects();
        ticks(faster, 10);
        ticks(baseline, 10);
        helper.assertTrue(faster.getHealth() == 11 && baseline.getHealth() == 10, "Restorative redeems reserve earlier");
        ticks(faster, 26);
        ticks(baseline, 26);
        helper.assertTrue(faster.getHealth() == 13 && baseline.getHealth() == 13, "Both redeem exactly 3 HP");
        helper.assertTrue(PlayerRecovery.state(faster).reserve() == 0 && PlayerRecovery.state(baseline).reserve() == 0, "No reserve created by speed");
        helper.succeed();
    }

    @GameTest
    public void invigoratedOnlyDiscountsActivitySites(GameTestHelper helper) {
        var baseline = player(helper);
        var active = player(helper);
        grant(active, MealBenefitType.INVIGORATED, 1200);
        for (var player : new ServerPlayer[]{baseline, active}) {
            player.setSprinting(false);
            player.jumpFromGround();
            player.setSprinting(true);
            player.jumpFromGround();
            player.setOnGround(true);
            player.checkMovementStatistics(10, 0, 0);
            player.setSwimming(true);
            player.checkMovementStatistics(10, 0, 0);
        }
        near(helper, exhaustion(baseline), 1.35F, "Vanilla jump, sprint and swim costs");
        near(helper, exhaustion(active), 1.215F, "Only these activities receive 10 percent discount");
        float before = exhaustion(active);
        active.causeFoodExhaustion(1);
        near(helper, exhaustion(active) - before, 1, "General exhaustion remains unchanged");
        var natural = player(helper);
        grant(natural, MealBenefitType.INVIGORATED, 1200);
        natural.getFoodData().setFoodLevel(20);
        natural.getFoodData().setSaturation(10);
        ticks(natural, 11);
        helper.assertTrue(natural.getHealth() == 11, "Natural healing timing remains unchanged");
        near(helper, exhaustion(natural), 6, "Natural recovery pays its full exhaustion cost");
        helper.succeed();
    }

    @GameTest
    public void timerExpiresAtFullHealthAndInactivePlayersReceiveNoEffect(GameTestHelper helper) {
        var player = player(helper);
        player.setHealth(20);
        grant(player, MealBenefitType.INVIGORATED, 2);
        ticks(player, 1);
        helper.assertTrue(PlayerMealBenefits.state(player).remainingTicks() == 1, "Full health still advances timer");
        ticks(player, 1);
        helper.assertTrue(PlayerMealBenefits.state(player).equals(MealBenefitState.EMPTY), "Expiry removes slot");
        near(helper, PlayerMealBenefits.activityExhaustion(player, 1), 1, "Expiry restores normal exhaustion");
        // Vanilla's mock overrides gameMode() with its constructor argument; use a creative mock.
        player = player(helper, GameType.CREATIVE);
        grant(player, MealBenefitType.RESTORATIVE, 100);
        helper.assertTrue(PlayerMealBenefits.foodInterval(player) == 12, "Creative receives no recovery speed bonus");
        new ItemStack(Items.PUMPKIN_PIE).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(PlayerMealBenefits.state(player).is(MealBenefitType.RESTORATIVE), "Creative food does not grant benefit");
        ticks(player, 1);
        helper.assertTrue(PlayerMealBenefits.state(player).remainingTicks() == 99, "Inactive mode does not freeze the timer");
        helper.assertTrue(PlayerMealBenefits.state(player(helper)).equals(MealBenefitState.EMPTY), "Other players are independent");
        helper.succeed();
    }

    @GameTest
    public void saveLoadAndPlayerReplacement(GameTestHelper helper) {
        var original = player(helper);
        grant(original, MealBenefitType.INVIGORATED, 731);
        var expected = PlayerMealBenefits.state(original);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        var loaded = player(helper);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(PlayerMealBenefits.state(loaded).equals(expected), "Player NBT preserves exact type and duration");
        var transferred = player(helper);
        transferred.restoreFrom(loaded, true);
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(loaded, transferred, true);
        helper.assertTrue(PlayerMealBenefits.state(transferred).equals(expected), "Alive replacement copies benefit");
        var respawned = player(helper);
        respawned.restoreFrom(loaded, false);
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(loaded, respawned, false);
        helper.assertTrue(PlayerMealBenefits.state(respawned).equals(MealBenefitState.EMPTY), "Death replacement clears benefit");
        helper.succeed();
    }
}
