package com.davidblackcn.buildupvitals.client;

import com.davidblackcn.buildupvitals.network.FoodProfilesPayload;
import com.davidblackcn.buildupvitals.network.TooltipProfile;
import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

/** Render-thread-only cache, scoped to the current play connection. */
public final class ClientFoodProfiles {
    private static Map<Identifier, TooltipProfile> profiles = Map.of();
    private static boolean available;

    private ClientFoodProfiles() { }

    public static void register() {
        ClientPlayConnectionEvents.INIT.register((listener, client) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> clear());
        ClientPlayNetworking.registerGlobalReceiver(FoodProfilesPayload.TYPE, (payload, context) -> {
            profiles = payload.profiles();
            available = payload.available();
            HydrationAdapter.receive(payload.hydrationEnabled(), profiles);
        });
    }

    public static boolean available() { return available; }

    public static Optional<TooltipProfile> find(Identifier item) {
        return available ? Optional.of(profiles.getOrDefault(item, TooltipProfile.fallback(item))) : Optional.empty();
    }

    private static void clear() {
        available = false;
        profiles = Map.of();
        HydrationAdapter.disconnect();
    }
}
