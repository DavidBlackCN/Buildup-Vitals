package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

/** Same-server interleaving; these mock connections are not a multi-client network load test. */
public class CoreFreezeGameTests {
    @GameTest
    public void interleavedPlayersKeepIndependentStateAndSaves(GameTestHelper helper) {
        var meal = player(helper);
        var fruit = player(helper);
        var observer = player(helper);
        meal.getFoodData().setFoodLevel(0);
        fruit.getFoodData().setFoodLevel(0);
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), meal);
        new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), fruit);
        helper.assertTrue(PlayerRecovery.state(meal).reserve() == 3 && PlayerRecovery.state(fruit).reserve() == 1,
                "Same-tick consumption uses each player's food budget");
        helper.assertTrue(meal.hasEffect(BuildupEffects.RESTORATIVE) && !fruit.hasEffect(BuildupEffects.RESTORATIVE),
                "Meal effect belongs only to its consumer");
        for (int i = 0; i < 10; i++) {
            ticks(fruit, 1);
            ticks(meal, 1);
            ticks(observer, 1);
        }
        helper.assertTrue(meal.getHealth() == 11 && fruit.getHealth() == 10 && observer.getHealth() == 10,
                "Players advance independent recovery clocks");
        ticks(fruit, 2);
        helper.assertTrue(fruit.getHealth() == 11 && PlayerRecovery.state(fruit).reserve() == 0
                && PlayerRecovery.state(meal).reserve() == 2, "One reserve expiring cannot clear another");
        meal.getFoodData().setFoodLevel(20);
        fruit.getFoodData().setFoodLevel(20);
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), meal);
        new ItemStack(Items.APPLE).finishUsingItem(helper.getLevel(), fruit);
        helper.assertTrue(PlayerOvereat.state(meal).load() == 6 && PlayerOvereat.state(fruit).load() == 4,
                "Full-hunger load is isolated per consumer");
        helper.assertTrue(!PlayerDiet.state(meal).equals(PlayerDiet.state(fruit)), "Diet histories remain distinct");
        for (var original : new ServerPlayer[]{meal, fruit}) {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            original.saveWithoutId(output);
            var loaded = player(helper);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
            helper.assertTrue(PlayerRecovery.state(loaded).equals(PlayerRecovery.state(original))
                    && PlayerDiet.state(loaded).equals(PlayerDiet.state(original))
                    && PlayerOvereat.state(loaded).equals(PlayerOvereat.state(original))
                    && loaded.hasEffect(BuildupEffects.RESTORATIVE) == original.hasEffect(BuildupEffects.RESTORATIVE),
                    "Each save restores its owner's recovery, diet, load and effect");
        }
        var fruitRecovery = PlayerRecovery.state(fruit);
        var fruitDiet = PlayerDiet.state(fruit);
        var fruitLoad = PlayerOvereat.state(fruit);
        meal.hurtServer(helper.getLevel(), meal.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(PlayerRecovery.state(meal).reserve() == 0 && PlayerDiet.state(meal).entries().isEmpty()
                && PlayerOvereat.state(meal).load() == 0, "Death clears only the dead player's state");
        helper.assertTrue(PlayerRecovery.state(fruit).equals(fruitRecovery) && PlayerDiet.state(fruit).equals(fruitDiet)
                && PlayerOvereat.state(fruit).equals(fruitLoad), "Another player's death does not mutate the survivor");
        helper.assertTrue(PlayerRecovery.state(observer).reserve() == 0 && PlayerDiet.state(observer).entries().isEmpty()
                && PlayerOvereat.state(observer).load() == 0 && !observer.hasEffect(BuildupEffects.RESTORATIVE),
                "Uninvolved player receives no food state");
        helper.succeed();
    }
}
