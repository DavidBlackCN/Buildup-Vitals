package com.davidblackcn.buildupvitals;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/** Connected-player tests: real ticks, dimensions, death, save/reopen and TCP reconnect. */
public class RecoveryClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        try (var singleplayer = context.worldBuilder().create()) {
            exercise(context, singleplayer.getServer(), singleplayer.getConnection());
            save = singleplayer.getWorldSave();
            preparePersistence(singleplayer.getServer(), singleplayer.getConnection());
        }
        try (var reopened = save.open()) {
            assertPersistence(reopened.getServer(), reopened.getConnection());
        }
        try (var dedicated = context.worldBuilder().createServer()) {
            try (var connection = dedicated.connect()) {
                exercise(context, dedicated, connection);
                preparePersistence(dedicated, connection);
            }
            try (var reconnected = dedicated.connect()) {
                assertPersistence(dedicated, reconnected);
            }
        }
        BuildupVitals.LOGGER.info("Stage 2 connected-player tests passed: integrated + dedicated, dimensions, death, save/reopen, reconnect");
    }

    private static void exercise(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
        server.runOnServer(instance -> {
            instance.setDifficulty(Difficulty.NORMAL, true);
            var player = connection.getServerPlayer();
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            player.setHealth(10);
            player.getFoodData().setFoodLevel(10);
            player.getFoodData().setSaturation(0);
            player.level().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, false, instance);
            new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(player.level(), player);
            check(player.getHealth() == 10 && PlayerRecovery.state(player).reserve() == 3, "Food must be delayed");
        });
        server.waitFor(instance -> PlayerRecovery.state(connection.getServerPlayer()).progress() >= 20);
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            check(player.getHealth() == 10, "No early healing");
            player.hurtServer(player.level(), player.damageSources().generic(), 2);
            check(player.getHealth() == 8, "Real player takes damage");
        });
        server.waitFor(instance -> PlayerRecovery.state(connection.getServerPlayer()).reserve() < 3);
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            check(player.getHealth() == 9 && PlayerRecovery.state(player).reserve() == 2, "Food heals through damage");
            player.heal(2);
            check(player.getHealth() == 11, "Independent healing works");
            player.removeAttached(RecoveryAttachments.RECOVERY);
            player.setHealth(10);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(10);
            player.level().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, true, instance);
        });
        server.waitFor(instance -> PlayerRecovery.state(connection.getServerPlayer()).progress() >= 60);
        server.runOnServer(instance -> check(connection.getServerPlayer().getHealth() == 10, "No vanilla high-saturation burst"));
        server.waitFor(instance -> connection.getServerPlayer().getHealth() == 11);

        server.runOnServer(instance -> {
            instance.setDifficulty(Difficulty.PEACEFUL, true);
            var player = connection.getServerPlayer();
            player.removeAttached(RecoveryAttachments.RECOVERY);
            player.setHealth(10);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(10);
        });
        server.waitFor(instance -> PlayerRecovery.state(connection.getServerPlayer()).progress() >= 60);
        server.runOnServer(instance -> check(connection.getServerPlayer().getHealth() == 10, "Peaceful health is also coordinated"));
        server.waitFor(instance -> connection.getServerPlayer().getHealth() == 11);

        preparePersistence(server, connection);
        server.runCommand("execute in minecraft:the_nether run tp @a 0 100 0");
        context.waitFor(client -> client.level != null && client.level.dimension() == Level.NETHER);
        assertPersistence(server, connection);
        server.runCommand("execute in minecraft:overworld run tp @a 0 100 0");
        context.waitFor(client -> client.level != null && client.level.dimension() == Level.OVERWORLD);
        assertPersistence(server, connection);
        server.waitFor(instance -> connection.getServerPlayer().connection.hasClientLoaded()
                && !connection.getServerPlayer().isChangingDimension());
        server.runOnServer(instance -> connection.getServerPlayer().setPermanentlyInvulnerable(false));
        server.runCommand("kill @a");
        context.waitFor(client -> client.player != null && client.player.isDeadOrDying());
        server.runOnServer(instance -> check(PlayerRecovery.state(connection.getServerPlayer()).reserve() == 0, "Death clears reserve immediately"));
        context.runOnClient(client -> client.player.respawn());
        server.waitFor(instance -> connection.getServerPlayer().isAlive());
        server.runOnServer(instance -> check(PlayerRecovery.state(connection.getServerPlayer()).reserve() == 0, "Respawn does not copy reserve"));
        server.runOnServer(instance -> {
            try {
                check(instance.getCommands().getDispatcher().execute("buildupvitals recovery @a[limit=1]",
                        instance.createCommandSourceStack()) == 1, "Recovery query succeeds");
            } catch (CommandSyntaxException exception) {
                throw new AssertionError("Recovery query failed", exception);
            }
        });
        BuildupVitals.LOGGER.info("Stage 2 connected recovery scenario passed");
    }

    private static void preparePersistence(TestServerContext server, TestServerConnection connection) {
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            player.setHealth(player.getMaxHealth());
            player.setPermanentlyInvulnerable(true);
            player.setNoGravity(true);
            player.setAttached(RecoveryAttachments.RECOVERY, new RecoveryState(2.5, 0, RecoveryState.Mode.NONE));
        });
    }

    private static void assertPersistence(TestServerContext server, TestServerConnection connection) {
        server.runOnServer(instance -> check(PlayerRecovery.state(connection.getServerPlayer()).reserve() == 2.5,
                "Reserve must survive save, reconnect and dimension transfer"));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
