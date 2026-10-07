package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
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
        foodConsumed(player, stack, 1);
    }

    /** Actual portion of a placed dish; the official profile remains the whole item's budget. */
    public static void foodConsumed(ServerPlayer player, ItemStack stack, double portion) {
        if (!Double.isFinite(portion) || portion <= 0 || portion > 1) throw new IllegalArgumentException("Invalid portion");
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            return;
        }
        var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var profile = FoodProfileLoader.snapshot(player.level().getServer()).resolve(item).profile();
        var variety = PlayerDiet.foodConsumed(player, item, profile);
        if (com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.overfull(player)) return;
        RecoveryState before = state(player);
        // Clamp before multiplication so even a finite Double.MAX_VALUE profile cannot overflow.
        double recovery = Math.min(RecoveryBalance.MAX_RESERVE, profile.recoveryHealth()) * portion * variety.foodMultiplier();
        store(player, before, before.addFood(recovery));
        PlayerMealBenefits.foodConsumed(player, profile, variety.benefitMultiplier(), stack, portion);
    }

    public static void tick(ServerPlayer player) {
        PlayerMealBenefits.tick(player);
        com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.tick(player);
        RecoveryState before = state(player);
        var food = player.getFoodData();
        var step = RecoveryController.tick(before, new RecoveryController.Conditions(player.isAlive(),
                !player.isCreative() && !player.isSpectator(), player.getHealth(), player.getMaxHealth(),
                food.getFoodLevel(), food.getSaturationLevel(),
                player.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION)), PlayerMealBenefits.foodInterval(player),
                naturalSpeed(player), !com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.overfull(player));
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

    public static double naturalSpeed(ServerPlayer player) {
        var food = player.getFoodData();
        double speed = food.getFoodLevel() == 20 && food.getSaturationLevel() > 0
                ? PlayerDiet.state(player).variety().wellFedMultiplier()
                : com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter.stableSpeed(player);
        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()
                && com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge.quenchedRecovery(player)) {
            speed *= com.davidblackcn.buildupvitals.config.CoreBalance.Recovery.QUENCHED_SPEED;
        }
        return speed;
    }

    private static void store(ServerPlayer player, RecoveryState before, RecoveryState after) {
        if (!before.equals(after)) {
            player.setAttached(RecoveryAttachments.RECOVERY, after);
        }
    }
}
