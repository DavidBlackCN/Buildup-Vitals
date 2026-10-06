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
                for (var effect : BuildupEffects.mainEffects()) player.removeEffect(effect);
                player.removeEffect(BuildupEffects.OVERFULL);
            }
        });
    }
    public static void migrate(ServerPlayer player) {
        if (!player.hasAttached(MealBenefitAttachments.MEAL_BENEFIT)) return;
        var old = player.getAttachedOrElse(MealBenefitAttachments.MEAL_BENEFIT, MealBenefitState.EMPTY);
        boolean modern = BuildupEffects.mainEffects().stream().anyMatch(player::hasEffect);
        if (!modern && player.isAlive() && old.remainingTicks() > 0) {
            old.type().flatMap(MealBenefitRegistry::find).filter(MealBenefitType::implemented).ifPresent(type ->
                    player.addEffect(new MobEffectInstance(BuildupEffects.forType(type), old.remainingTicks(), 0, false, false, true)));
        }
        player.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
    }
    public static MealBenefitState state(ServerPlayer player) {
        migrate(player);
        for (var type : MealBenefitType.values()) {
            var effect = player.getEffect(BuildupEffects.forType(type));
            if (effect != null) return new MealBenefitState(Optional.of(type.id()), effect.isInfiniteDuration()
                    ? MealBenefitBalance.MAX_DURATION : Math.clamp(effect.getDuration(), 1, MealBenefitBalance.MAX_DURATION));
        }
        return MealBenefitState.EMPTY;
    }
    public static void foodConsumed(ServerPlayer player, FoodProfile profile) { foodConsumed(player, profile, 1); }
    public static void foodConsumed(ServerPlayer player, FoodProfile profile, double durationMultiplier) {
        migrate(player);
        if (!PlayerOvereat.active(player) || PlayerOvereat.overfull(player) || profile.mealBenefit().filter(MealBenefitRegistry::available).isEmpty()) return;
        var granted = state(player).grant(profile.mealBenefit(), profile.quality(), durationMultiplier);
        var type = MealBenefitRegistry.find(granted.type().orElseThrow()).orElseThrow();
        var holder = BuildupEffects.forType(type);
        var current = player.getEffect(holder);
        // Food never shortens an operator-supplied longer/infinite effect of the same kind.
        if (current != null && current.isInfiniteDuration()) return;
        int duration = current == null ? granted.remainingTicks() : Math.max(current.getDuration(), granted.remainingTicks());
        for (var other : BuildupEffects.mainEffects()) if (!other.equals(holder)) player.removeEffect(other);
        player.addEffect(new MobEffectInstance(holder, duration, 0, false, false, true));
    }
    public static void tick(ServerPlayer player) {
        migrate(player);
        // Normalize externally loaded/force-added conflicting effects; ordinary additions enforce exclusivity immediately.
        boolean found = false;
        for (var holder : BuildupEffects.mainEffects()) {
            if (player.hasEffect(holder)) {
                if (found) player.removeEffect(holder);
                found = true;
            }
        }
    }
    public static int foodInterval(ServerPlayer player) {
        return PlayerOvereat.active(player) && player.hasEffect(BuildupEffects.RESTORATIVE)
                ? MealBenefitBalance.RESTORATIVE_FOOD_TICKS : RecoveryBalance.FOOD_TICKS;
    }
    public static float activityExhaustion(ServerPlayer player, float amount) {
        return PlayerOvereat.active(player) && player.hasEffect(BuildupEffects.INVIGORATED)
                ? amount * MealBenefitBalance.INVIGORATED_EXHAUSTION_MULTIPLIER : amount;
    }
}
