package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

public final class PlayerRecovery {
    private PlayerRecovery() { }

    public static void register() {
        RecoveryAttachments.register();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (entity instanceof ServerPlayer player) {
                player.removeAttached(RecoveryAttachments.RECOVERY);
            }
        });
    }

    public static RecoveryState state(ServerPlayer player) {
        return player.getAttachedOrElse(RecoveryAttachments.RECOVERY, RecoveryState.EMPTY);
    }

    public static void foodConsumed(ServerPlayer player, ItemStack stack) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }
        double amount = FoodProfileLoader.snapshot(player.level().getServer())
                .resolve(BuiltInRegistries.ITEM.getKey(stack.getItem())).profile().recoveryHealth();
        RecoveryState before = state(player);
        store(player, before, before.addFood(amount));
    }

    public static void tick(ServerPlayer player) {
        RecoveryState before = state(player);
        var food = player.getFoodData();
        var step = RecoveryController.tick(before, new RecoveryController.Conditions(player.isAlive(),
                !player.isCreative() && !player.isSpectator(), player.getHealth(), player.getMaxHealth(),
                food.getFoodLevel(), food.getSaturationLevel(),
                player.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION)));
        float health = player.getHealth();
        if (step.healing() > 0) {
            player.heal(step.healing());
        }
        float gained = Math.max(0, player.getHealth() - health);
        float exhaustion = step.exhaustion(gained);
        if (exhaustion > 0) {
            food.addExhaustion(exhaustion);
        }
        store(player, before, step.settle(gained));
    }

    private static void store(ServerPlayer player, RecoveryState before, RecoveryState after) {
        if (!before.equals(after)) {
            player.setAttached(RecoveryAttachments.RECOVERY, after);
        }
    }
}
