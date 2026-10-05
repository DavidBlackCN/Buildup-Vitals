package com.davidblackcn.buildupvitals.diet;

import com.davidblackcn.buildupvitals.food.profile.FoodProfile;
import com.davidblackcn.buildupvitals.player.DietAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerDiet {
    private PlayerDiet() { }

    public static void register() {
        DietAttachments.register();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (entity instanceof ServerPlayer player) player.removeAttached(DietAttachments.DIET);
        });
    }

    public static DietMemory state(ServerPlayer player) {
        return player.getAttachedOrElse(DietAttachments.DIET, DietMemory.EMPTY);
    }

    /** Called once from completed server-side food consumption, before applying this meal's bonuses. */
    public static VarietyResult foodConsumed(ServerPlayer player, Identifier food, FoodProfile profile) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return VarietyResult.BASELINE;
        var after = state(player).append(DietEntry.consumed(food, profile, player.level().getServer().overworld().getGameTime()));
        player.setAttached(DietAttachments.DIET, after);
        return after.variety();
    }
}
