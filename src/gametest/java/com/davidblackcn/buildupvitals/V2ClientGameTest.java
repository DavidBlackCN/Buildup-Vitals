package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class V2ClientGameTest implements FabricClientGameTest {
    private static final java.util.concurrent.atomic.AtomicInteger warnings = new java.util.concurrent.atomic.AtomicInteger();
    @Override public void runTest(ClientGameTestContext context) {
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay && message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().equals("message.buildup_vitals.overeat_warning")) {
                check(!message.getString().startsWith("message."), "Warning translated");
                warnings.incrementAndGet();
            }
        });
        try (var world = context.worldBuilder().create()) { exercise(context, world.getServer(), world.getConnection(), "integrated"); }
        try (var server = context.worldBuilder().createServer(); var connection = server.connect()) { exercise(context, server, connection, "dedicated"); }
        BuildupVitals.LOGGER.info("Stage 7.5 client consumption, effect sprites, inventory and HUD verified");
    }
    private static void exercise(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String kind) {
        context.waitFor(client -> ClientFoodProfiles.available());
        exerciseDuration(context, server, connection, GameType.CREATIVE, Items.COOKED_BEEF, 32);
        exerciseDuration(context, server, connection, GameType.CREATIVE, Items.APPLE, 21);
        exerciseDuration(context, server, connection, GameType.CREATIVE, Items.COOKIE, 16);
        exerciseDuration(context, server, connection, GameType.SURVIVAL, Items.APPLE, 21);
        server.runOnServer(instance -> {
            connection.getServerPlayer().removeAttached(PlayerOvereat.STATE);
            connection.getServerPlayer().removeAttached(com.davidblackcn.buildupvitals.player.RecoveryAttachments.RECOVERY);
            connection.getServerPlayer().removeAttached(com.davidblackcn.buildupvitals.player.DietAttachments.DIET);
        });
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer(); player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setHealth(20);
            player.getFoodData().setFoodLevel(20);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE, 2));
            player.inventoryMenu.broadcastChanges();
        });
        context.waitFor(client -> client.player.getMainHandItem().is(Items.COOKIE));
        context.runOnClient(client -> {
            check(client.player.getMainHandItem().getUseDuration(client.player) == 16, "Server fast tier reaches client animation");
            client.options.keyUse.setDown(true);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            check(client.player.isUsingItem() && client.player.getUseItemRemainingTicks() == 16, "Full hunger starts 16 tick client animation");
        });
        server.waitFor(instance -> connection.getServerPlayer().isUsingItem());
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            check(player.getUseItem().getUseDuration(player) == 16 && player.getUseItemRemainingTicks() > 0, "Server runs the same timer");
            check(PlayerOvereat.state(player).load() == 0, "Use start has no consumption side effects");
        });
        server.waitFor(instance -> PlayerOvereat.state(connection.getServerPlayer()).load() >= 2);
        context.runOnClient(client -> { client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); });
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            check(PlayerOvereat.state(player).load() == 2 && PlayerRecovery.state(player).reserve() == .5, "Completed animation consumes once at full hunger");
            player.addEffect(new MobEffectInstance(BuildupEffects.RESTORATIVE, 1200, 0, false, false, true));
            player.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 1200, 0, false, false, true));
        });
        context.waitFor(client -> client.player.hasEffect(BuildupEffects.RESTORATIVE) && client.player.hasEffect(BuildupEffects.OVERFULL));
        context.runOnClient(client -> {
            check(client.player.getMainHandItem().getUseDuration(client.player) == 20, "Synced Overfull slows client animation");
            for (var effect : java.util.List.of(BuildupEffects.RESTORATIVE, BuildupEffects.INVIGORATED, BuildupEffects.STEADY, BuildupEffects.OVERFULL)) {
                var sprite = client.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.GUI).getSprite(net.minecraft.client.gui.Hud.getMobEffectSprite(effect));
                check(!sprite.contents().name().getPath().equals("missingno"), "Effect sprite exists: " + effect);
                check(!effect.value().getDisplayName().getString().startsWith("effect."), "Effect name translated");
            }
            var instance = client.player.getEffect(BuildupEffects.RESTORATIVE);
            check(instance.showIcon() && !instance.isVisible() && instance.getDuration() > 0, "Native synced icon/countdown without particles");
        });
        context.waitTicks(3);
        BuildupVitals.LOGGER.info("V2 HUD screenshot: {}", context.takeScreenshot("stage75-" + kind + "-hud"));
        context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
        context.waitTicks(3);
        BuildupVitals.LOGGER.info("V2 inventory screenshot: {}", context.takeScreenshot("stage75-" + kind + "-effects"));
        context.setScreen(() -> null);
        int before = warnings.get();
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer(); player.removeAllEffects();
            player.setAttached(PlayerOvereat.STATE, new com.davidblackcn.buildupvitals.food.overeating.OvereatState(46, 0, false, false));
            player.getFoodData().setFoodLevel(20);
            new ItemStack(Items.COOKIE).finishUsingItem(player.level(), player);
            new ItemStack(Items.COOKIE).finishUsingItem(player.level(), player);
        });
        context.waitFor(client -> warnings.get() == before + 1);
        context.waitTicks(3);
        check(warnings.get() == before + 1, "Server warning is sent once per episode over a real connection");
        BuildupVitals.LOGGER.info("V2 warning screenshot: {}", context.takeScreenshot("stage75-" + kind + "-warning"));
    }

    private static void exerciseDuration(ClientGameTestContext context, TestServerContext server,
            TestServerConnection connection, GameType mode, net.minecraft.world.item.Item item, int duration) {
        int before = server.computeOnServer(instance -> {
            var player = connection.getServerPlayer();
            player.stopUsingItem(); player.removeAllEffects(); player.setGameMode(mode);
            player.setNoGravity(true); player.setHealth(20); player.getFoodData().setFoodLevel(20);
            // A retained survival effect must not make creative timers disagree across the connection.
            if (mode == GameType.CREATIVE)
                player.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 1200, 0, false, false, true));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            player.inventoryMenu.broadcastChanges();
            return player.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED, item);
        });
        context.waitFor(client -> client.player.getMainHandItem().is(item)
                && client.player.isCreative() == (mode == GameType.CREATIVE)
                && client.player.hasEffect(BuildupEffects.OVERFULL) == (mode == GameType.CREATIVE));
        context.runOnClient(client -> {
            check(client.player.getMainHandItem().getUseDuration(client.player) == duration, "Client duration " + mode + ": " + item);
            client.options.keyUse.setDown(true);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            check(client.player.getUseItemRemainingTicks() == duration, "Client starts matching animation timer");
        });
        server.waitFor(instance -> connection.getServerPlayer().isUsingItem());
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            check(player.getUseItem().getUseDuration(player) == duration && player.getUseItemRemainingTicks() > 0
                    && player.getUseItemRemainingTicks() <= duration, "Server timer matches client");
            check(player.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED, item) == before, "Use has not completed early");
        });
        server.waitFor(instance -> connection.getServerPlayer().getStats().getValue(net.minecraft.stats.Stats.ITEM_USED, item) > before);
        context.runOnClient(client -> { client.options.keyUse.setDown(false); client.gameMode.releaseUsingItem(client.player); });
        server.runOnServer(instance -> {
            var player = connection.getServerPlayer();
            check(player.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED, item) == before + 1, "Timed use completes exactly once");
            check(mode != GameType.CREATIVE || (player.getMainHandItem().getCount() == 1
                    && PlayerOvereat.state(player).load() == 0 && PlayerRecovery.state(player).reserve() == 0),
                    "Creative keeps items and does not grant recovery or overeating");
            player.stopUsingItem();
        });
        context.waitFor(client -> !client.player.isUsingItem());
        BuildupVitals.LOGGER.info("Consumption regression verified: {} {} = {} ticks", mode, item, duration);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
