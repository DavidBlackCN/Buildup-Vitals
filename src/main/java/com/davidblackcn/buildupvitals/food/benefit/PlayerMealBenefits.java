package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.profile.FoodProfile;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryBalance;
import com.davidblackcn.buildupvitals.player.MealBenefitAttachments;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/** Vanilla MobEffects own saving, ticking and synchronization. The legacy attachment is read once. */
public final class PlayerMealBenefits {
    private PlayerMealBenefits() { }
    public static void register() {
        MealBenefitAttachments.register();
        BuildupEffects.register();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> migrate(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (entity instanceof ServerPlayer player) {
                player.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
                for (var effect : ForeignMealBenefits.mainEffects()) player.removeEffect(effect);
                player.removeEffect(BuildupEffects.OVERFULL);
            }
        });
    }
    public static void migrate(ServerPlayer player) {
        if (!player.hasAttached(MealBenefitAttachments.MEAL_BENEFIT)) return;
        var old = player.getAttachedOrElse(MealBenefitAttachments.MEAL_BENEFIT, MealBenefitState.EMPTY);
        boolean modern = ForeignMealBenefits.mainEffects().stream().anyMatch(player::hasEffect);
        if (!modern && player.isAlive() && old.remainingTicks() > 0) {
            old.type().flatMap(MealBenefitRegistry::find).filter(MealBenefitType::implemented).ifPresent(type ->
                    player.addEffect(new MobEffectInstance(BuildupEffects.forType(type), old.remainingTicks(), 0, false, false, true)));
        }
        player.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
    }
    public static MealBenefitState state(ServerPlayer player) {
        migrate(player);
        for (var holder : ForeignMealBenefits.mainEffects()) {
            var effect = player.getEffect(holder);
            if (effect != null) return new MealBenefitState(Optional.of(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(holder.value())), effect.isInfiniteDuration()
                    ? MealBenefitBalance.MAX_DURATION : Math.clamp(effect.getDuration(), 1, MealBenefitBalance.MAX_DURATION));
        }
        return MealBenefitState.EMPTY;
    }
    public static void foodConsumed(ServerPlayer player, FoodProfile profile) { foodConsumed(player, profile, 1); }
    public static void foodConsumed(ServerPlayer player, FoodProfile profile, double durationMultiplier) {
        foodConsumed(player, profile, durationMultiplier, net.minecraft.world.item.ItemStack.EMPTY);
    }
    public static void foodConsumed(ServerPlayer player, FoodProfile profile, double durationMultiplier, net.minecraft.world.item.ItemStack stack) {
        foodConsumed(player, profile, durationMultiplier, stack, 1);
    }
    public static void foodConsumed(ServerPlayer player, FoodProfile profile, double durationMultiplier, net.minecraft.world.item.ItemStack stack, double portion) {
        migrate(player);
        if (!PlayerOvereat.active(player) || PlayerOvereat.overfull(player) || profile.mealBenefit().filter(MealBenefitRegistry::available).isEmpty()) return;
        var incoming = profile.mealBenefit().orElseThrow();
        if (ForeignMealBenefits.registered(incoming)) {
            var consumable = stack.get(net.minecraft.core.component.DataComponents.CONSUMABLE);
            if (consumable != null && consumable.onConsumeEffects().stream()
                    .filter(net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect.class::isInstance)
                    .map(net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect.class::cast)
                    .flatMap(action -> action.effects().stream())
                    .anyMatch(effect -> incoming.equals(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value())))) {
                // Vanilla applies this after FoodProperties callbacks; preserve its duration and probability exactly once.
                return;
            }
        }
        var granted = state(player).grant(profile.mealBenefit(), profile.quality(), durationMultiplier);
        var holder = ForeignMealBenefits.holder(incoming).orElseThrow();
        var current = player.getEffect(holder);
        // Food never shortens an operator-supplied longer/infinite effect of the same kind.
        if (current != null && current.isInfiniteDuration()) return;
        int portionDuration = Math.max(1, (int) Math.floor(granted.remainingTicks() * portion));
        int duration = current == null ? portionDuration : Math.max(current.getDuration(), portionDuration);
        for (var other : ForeignMealBenefits.mainEffects()) if (!other.equals(holder)) player.removeEffect(other);
        player.addEffect(new MobEffectInstance(holder, duration, 0, false, false, true));
    }
    public static void tick(ServerPlayer player) {
        migrate(player);
        // Normalize externally loaded/force-added conflicting effects; ordinary additions enforce exclusivity immediately.
        boolean found = false;
        for (var holder : ForeignMealBenefits.mainEffects()) {
            if (player.hasEffect(holder)) {
                if (found) player.removeEffect(holder);
                found = true;
            }
        }
    }
    public static int foodInterval(ServerPlayer player) {
        return PlayerOvereat.active(player) && ForeignMealBenefits.restorative(player)
                ? MealBenefitBalance.RESTORATIVE_FOOD_TICKS : RecoveryBalance.FOOD_TICKS;
    }
    public static float activityExhaustion(ServerPlayer player, float amount) {
        return PlayerOvereat.active(player) && ForeignMealBenefits.invigorated(player)
                ? amount * MealBenefitBalance.INVIGORATED_EXHAUSTION_MULTIPLIER : amount;
    }
}
