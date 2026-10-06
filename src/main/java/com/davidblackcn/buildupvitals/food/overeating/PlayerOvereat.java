package com.davidblackcn.buildupvitals.food.overeating;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public final class PlayerOvereat {
    public static final AttachmentType<OvereatState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "overeat"),
            b -> b.initializer(() -> OvereatState.EMPTY).persistent(OvereatState.CODEC));
    private PlayerOvereat() { }
    public static void register() {
        BuildupEffects.register();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (entity instanceof ServerPlayer player) player.removeAttached(STATE);
        });
    }
    public static OvereatState state(ServerPlayer player) { return player.getAttachedOrElse(STATE, OvereatState.EMPTY); }
    public static boolean active(ServerPlayer player) { return player.isAlive() && !player.isCreative() && !player.isSpectator(); }
    public static boolean overfull(ServerPlayer player) {
        return active(player) && (state(player).overfull() || player.hasEffect(BuildupEffects.OVERFULL));
    }
    public static void foodConsumed(ServerPlayer player, int nutrition) {
        if (!active(player)) return;
        var before = state(player);
        var after = before.eat(player.getFoodData().getFoodLevel(), nutrition);
        if (!before.warned() && after.warned())
            player.sendSystemMessage(Component.translatable("message.buildup_vitals.overeat_warning"), true);
        store(player, before, after);
    }
    public static void tick(ServerPlayer player) {
        if (active(player)) store(player, state(player), state(player).tick());
    }
    private static void store(ServerPlayer player, OvereatState before, OvereatState after) {
        if (!before.equals(after)) {
            if (after.equals(OvereatState.EMPTY)) player.removeAttached(STATE);
            else player.setAttached(STATE, after);
        }
        if (after.overfull()) {
            var current = player.getEffect(BuildupEffects.OVERFULL);
            if (current == null || current.getDuration() < after.remainingOverfullTicks() - 2)
                player.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, after.remainingOverfullTicks(), 0, false, false, true));
        } else if (before.overfull()) player.removeEffect(BuildupEffects.OVERFULL);
    }
}
