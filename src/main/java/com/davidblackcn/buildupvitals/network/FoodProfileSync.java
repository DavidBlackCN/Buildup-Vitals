package com.davidblackcn.buildupvitals.network;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FoodProfileSync {
    private FoodProfileSync() { }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(FoodProfilesPayload.TYPE, FoodProfilesPayload.CODEC, FoodProfilesPayload.MAX_BYTES);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, FoodProfilesPayload.TYPE)) send(handler.player, snapshot(server));
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) {
                var payload = snapshot(server);
                for (var player : server.getPlayerList().getPlayers()) send(player, payload);
            }
        });
    }

    private static FoodProfilesPayload snapshot(MinecraftServer server) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var snapshot = FoodProfileLoader.snapshot(server);
            var entries = new HashMap<Identifier, TooltipProfile>();
            for (var item : snapshot.items()) entries.put(item, TooltipProfile.from(item, snapshot.resolve(item)));
            var payload = new FoodProfilesPayload(true, HydrationAdapter.enabled(), entries);
            // Validate once before sending; oversized display data must not prevent joining/reloading.
            FoodProfilesPayload.CODEC.encode(buffer, payload);
            return payload;
        } catch (IllegalArgumentException | io.netty.handler.codec.EncoderException exception) {
            BuildupVitals.LOGGER.warn("Food tooltip sync unavailable: {}", exception.getMessage());
            return new FoodProfilesPayload(false, Map.of());
        } finally {
            buffer.release();
        }
    }

    private static void send(ServerPlayer player, FoodProfilesPayload payload) {
        if (ServerPlayNetworking.canSend(player, FoodProfilesPayload.TYPE)) ServerPlayNetworking.send(player, payload);
    }
}
