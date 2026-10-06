package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Test-only mod: actual vanilla food, healing and attachment paths under Mixin. */
public class RecoveryGameTests {
    static ServerPlayer player(GameTestHelper helper) {
        return player(helper, GameType.SURVIVAL);
    }

    static ServerPlayer player(GameTestHelper helper, GameType gameType) {
        var player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setHealth(10);
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0);
        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) ThirstTestSupport.prepareRecoveryFixture(player);
        return player;
    }

    static void ticks(ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            for (var effect : java.util.List.copyOf(player.getActiveEffects())) {
                if (!effect.tickServer(player.level(), player, () -> { })) player.removeEffect(effect.getEffect());
            }
            player.getFoodData().tick(player);
        }
    }

    @GameTest
    public void finishedFoodAndBowlRemainder(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);
        helper.assertTrue(PlayerRecovery.state(player).reserve() == 0, "No reserve before consumption");
        ItemStack remainder = stew.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(remainder.is(Items.BOWL), "Vanilla bowl remainder retained");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 16, "Vanilla nutrition retained");
        helper.assertTrue(PlayerRecovery.state(player).reserve() == 3, "Finished stew supplies exactly one profile");
        helper.assertTrue(player.getHealth() == 10, "No instant food healing");
        ticks(player, 9);
        helper.assertTrue(player.getHealth() == 10, "Restorative food waits 10 ticks");
        player.hurtServer(helper.getLevel(), player.damageSources().generic(), 2);
        helper.assertTrue(player.getHealth() == 8, "Damage is unchanged");
        ticks(player, 1);
        helper.assertTrue(player.getHealth() == 9 && PlayerRecovery.state(player).reserve() == 2,
                "Damage does not reset food recovery");
        ticks(player, 20);
        helper.assertTrue(player.getHealth() == 11 && PlayerRecovery.state(player).reserve() == 0, "Reserve exhausts gradually");
        helper.succeed();
    }

    @GameTest
    public void vanillaNaturalBurstIsReplaced(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(10);
        ticks(player, 10);
        helper.assertTrue(player.getHealth() == 10, "No vanilla 10-tick healing burst");
        ticks(player, 1);
        helper.assertTrue(player.getHealth() == 11, "Well-fed heals 1 HP per 11 ticks");
        player.removeAttached(RecoveryAttachments.RECOVERY);
        player.getFoodData().setFoodLevel(18);
        player.getFoodData().setSaturation(3);
        ticks(player, 79);
        helper.assertTrue(player.getHealth() == 11, "Stable waits 80 ticks");
        ticks(player, 1);
        helper.assertTrue(player.getHealth() == 12, "Stable healing works");
        player.getFoodData().setSaturation(0);
        player.getFoodData().setFoodLevel(20);
        ticks(player, 80);
        helper.assertTrue(player.getHealth() == 13, "Zero saturation retains stable natural recovery");
        helper.succeed();
    }

    @GameTest
    public void externalHealingAndGoldenAppleRemainIndependent(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.heal(2);
        helper.assertTrue(player.getHealth() == 12, "Direct external heal is untouched");
        PotionContents.createItemStack(Items.POTION, Potions.HEALING).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getHealth() == 16, "Instant health potion is untouched");
        new ItemStack(Items.GOLDEN_APPLE).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.ABSORPTION),
                "Golden apple keeps its effects");
        helper.assertTrue(PlayerRecovery.state(player).reserve() == 0, "Golden apple and potion invent no food reserve");
        helper.succeed();
    }

    @GameTest
    public void saveLoadAndPlayerReplacement(GameTestHelper helper) {
        ServerPlayer original = player(helper);
        var expected = new RecoveryState(2.5, 5.5, RecoveryState.Mode.FOOD);
        original.setAttached(RecoveryAttachments.RECOVERY, expected);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        ServerPlayer loaded = player(helper);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(PlayerRecovery.state(loaded).equals(expected), "Player NBT saves reserve and partial progress");
        ServerPlayer transfer = player(helper);
        transfer.restoreFrom(loaded, true);
        // Fabric copies attachments in PlayerList's AFTER_RESPAWN event, after restoreFrom.
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(loaded, transfer, true);
        helper.assertTrue(PlayerRecovery.state(transfer).equals(expected), "Living respawn lifecycle preserves state (End return)");
        ServerPlayer respawned = player(helper);
        respawned.restoreFrom(loaded, false);
        ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(loaded, respawned, false);
        helper.assertTrue(PlayerRecovery.state(respawned).equals(RecoveryState.EMPTY), "Death replacement clears attachment");
        helper.succeed();
    }

    @GameTest
    public void fullHealthAndFallbackFood(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setHealth(20);
        for (int i = 0; i < 20; i++) {
            player.getFoodData().setFoodLevel(10);
            new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), player);
        }
        ticks(player, 200);
        helper.assertTrue(PlayerRecovery.state(player).reserve() == 20, "Full health keeps a bounded reserve");
        helper.assertTrue(PlayerRecovery.state(player).progress() == 0, "Full health cannot precharge healing");
        player.removeAttached(RecoveryAttachments.RECOVERY);
        player.getFoodData().setFoodLevel(10);
        new ItemStack(TestFoods.FALLBACK).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 15, "Unknown mod food still feeds player");
        helper.assertTrue(PlayerRecovery.state(player).reserve() == 0, "Fallback adds no reserve");
        helper.succeed();
    }

    @GameTest
    public void naturalGameruleDoesNotDisableFoodOrExternalHealing(GameTestHelper helper) {
        var rules = helper.getLevel().getGameRules();
        boolean old = rules.get(GameRules.NATURAL_HEALTH_REGENERATION);
        try {
            rules.set(GameRules.NATURAL_HEALTH_REGENERATION, false, helper.getLevel().getServer());
            ServerPlayer player = player(helper);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20);
            ticks(player, 200);
            helper.assertTrue(player.getHealth() == 10, "Gamerule disables natural healing");
            new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), player);
            ticks(player, 10);
            helper.assertTrue(player.getHealth() == 11, "Explicit food recovery works with natural regen disabled");
            player.heal(2);
            helper.assertTrue(player.getHealth() == 13, "Independent healing still works");
        } finally {
            rules.set(GameRules.NATURAL_HEALTH_REGENERATION, old, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void starvationAndFoodExhaustionArePreserved(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setHealth(20);
        player.getFoodData().setFoodLevel(0);
        ticks(player, 80);
        helper.assertTrue(player.getHealth() == 19, "Vanilla starvation still damages");
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(2);
        player.getFoodData().addExhaustion(5);
        ticks(player, 1);
        helper.assertTrue(player.getFoodData().getSaturationLevel() == 1 && player.getFoodData().getFoodLevel() == 10,
                "Activity exhaustion still consumes saturation first");
        helper.succeed();
    }
}
