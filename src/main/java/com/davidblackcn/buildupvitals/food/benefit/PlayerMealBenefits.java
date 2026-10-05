package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.food.profile.FoodProfile;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryBalance;
import com.davidblackcn.buildupvitals.player.MealBenefitAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerMealBenefits {
    private PlayerMealBenefits() { }

    public static void register() {
        MealBenefitAttachments.register();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (entity instanceof ServerPlayer player) player.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
        });
    }

    public static MealBenefitState state(ServerPlayer player) {
        return player.getAttachedOrElse(MealBenefitAttachments.MEAL_BENEFIT, MealBenefitState.EMPTY);
    }

    public static void foodConsumed(ServerPlayer player, FoodProfile profile) {
        if (activePlayer(player)) store(player, state(player).grant(profile.mealBenefit(), profile.quality()));
    }

    public static void tick(ServerPlayer player) {
        store(player, player.isAlive() ? state(player).tick() : MealBenefitState.EMPTY);
    }

    public static int foodInterval(ServerPlayer player) {
        return activePlayer(player) && state(player).is(MealBenefitType.RESTORATIVE)
                ? MealBenefitBalance.RESTORATIVE_FOOD_TICKS : RecoveryBalance.FOOD_TICKS;
    }

    /** Called only at verified movement/jump exhaustion sites, never at general exhaustion entry points. */
    public static float activityExhaustion(ServerPlayer player, float amount) {
        return activePlayer(player) && state(player).is(MealBenefitType.INVIGORATED)
                ? amount * MealBenefitBalance.INVIGORATED_EXHAUSTION_MULTIPLIER : amount;
    }

    private static boolean activePlayer(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static void store(ServerPlayer player, MealBenefitState after) {
        if (after.equals(MealBenefitState.EMPTY)) {
            if (player.hasAttached(MealBenefitAttachments.MEAL_BENEFIT)) player.removeAttached(MealBenefitAttachments.MEAL_BENEFIT);
        } else if (!after.equals(state(player))) {
            player.setAttached(MealBenefitAttachments.MEAL_BENEFIT, after);
        }
    }
}
