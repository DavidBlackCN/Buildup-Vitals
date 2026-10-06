package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

/** FoodData/Mixin/effects/TWT2 runtime counterpart to the independent vanilla math comparison. */
public class RecoveryBalanceGameTests {
    @GameTest public void finiteSaturationSavingsAreNotHiddenByPeakRate(GameTestHelper helper) {
        for (float saturation : new float[]{12.8f, 15}) {
            for (boolean apple : new boolean[]{false, true}) {
                var p = player(helper);
                p.getFoodData().setFoodLevel(20);
                if (apple) new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), p);
                p.getFoodData().setSaturation(saturation); // Compare equal post-meal nutrition.
                if (HydrationAdapter.enabled()) ThirstTestSupport.prepareBalanceFixture(p, false);
                int elapsed = 0;
                while (p.getHealth() < 20 && elapsed < 400) {
                    ticks(p, 1);
                    if (HydrationAdapter.enabled()) ThirstTestSupport.tickBalanceFixture(p);
                    elapsed++;
                }
                int expected = saturation == 15 ? (apple ? 121 : 154) : (apple ? 245 : 314);
                helper.assertTrue(p.getHealth() == 20 && elapsed == expected, "Finite saturation timing matches the math: saturation=" + saturation + " apple=" + apple + " ticks=" + elapsed);
                BuildupVitals.LOGGER.info("HOTFIX finite saturation={} apple={} ticks={}", saturation, apple, elapsed);
            }
        }
        helper.succeed();
    }

    @GameTest public void halfHealthRuntimeMatrix(GameTestHelper helper) {
        for (boolean quenched : new boolean[]{false, true}) {
            if (quenched && !HydrationAdapter.enabled()) continue;
            for (boolean variety : new boolean[]{false, true}) {
                for (Item item : new Item[]{Items.AIR, Items.APPLE, Items.RABBIT_STEW, Items.MUSHROOM_STEW}) {
                    var p = player(helper);
                    p.setHealth(20);
                    if (variety) DietGameTests.mixedMeals(p);
                    p.removeAttached(RecoveryAttachments.RECOVERY);
                    p.removeAllEffects();
                    p.getFoodData().setFoodLevel(20);
                    p.getFoodData().setSaturation(20);
                    if (item != Items.AIR) new ItemStack(item).finishUsingItem(helper.getLevel(), p);
                    if (HydrationAdapter.enabled()) ThirstTestSupport.prepareBalanceFixture(p, quenched);
                    double reserve = PlayerRecovery.state(p).reserve();
                    double speed = PlayerRecovery.naturalSpeed(p);
                    helper.assertTrue(!variety || PlayerDiet.state(p).variety().wellFedMultiplier() > 1,
                            "Actual varied meals supply a derived bonus");
                    p.setHealth(10);
                    int elapsed = 0;
                    while (p.getHealth() < 20 && elapsed < 200) {
                        float before = p.getHealth();
                        ticks(p, 1);
                        if (HydrationAdapter.enabled()) ThirstTestSupport.tickBalanceFixture(p);
                        helper.assertTrue(p.getHealth() - before <= 1, "No parallel healing pulse");
                        elapsed++;
                    }
                    helper.assertTrue(p.getHealth() == 20 && elapsed >= 100 && elapsed <= 110,
                            "Half-health restoration stays within vanilla high-saturation time +10%: " + item + " ticks=" + elapsed);
                    if (!variety) helper.assertTrue(elapsed == (quenched ? 100 : item == Items.MUSHROOM_STEW ? 107 : 110),
                            "Exact base/meal/quenched completion tick");
                    helper.assertTrue(PlayerRecovery.state(p).reserve() == 0, "One meal's reserve is fully spent");
                    BuildupVitals.LOGGER.info("HOTFIX runtime item={} variety={} quenched={} reserve={} speed={} ticks={}",
                            item, variety, quenched, reserve, speed, elapsed);
                }
            }
        }
        helper.succeed();
    }

    @GameTest public void foodAndRestorativeWithoutNaturalRecovery(GameTestHelper helper) {
        for (var item : new Item[]{Items.APPLE, Items.RABBIT_STEW, Items.MUSHROOM_STEW}) {
            var p = player(helper);
            p.getFoodData().setFoodLevel(0);
            new ItemStack(item).finishUsingItem(helper.getLevel(), p);
            int expectedTicks = item == Items.APPLE ? 12 : item == Items.RABBIT_STEW ? 48 : 30;
            float expectedGain = item == Items.APPLE ? 1 : item == Items.RABBIT_STEW ? 4 : 3;
            ticks(p, expectedTicks - 1);
            helper.assertTrue(p.getHealth() < 10 + expectedGain, "Food reserve cannot complete early");
            ticks(p, 1);
            helper.assertTrue(p.getHealth() == 10 + expectedGain && PlayerRecovery.state(p).reserve() == 0,
                    "Food-only budget and Restorative speed retained");
        }
        helper.succeed();
    }
}
