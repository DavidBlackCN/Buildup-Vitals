package com.davidblackcn.buildupvitals.hydration;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge;
import com.davidblackcn.buildupvitals.compat.thirst.ThirstCompatibility;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration;
import com.davidblackcn.buildupvitals.network.TooltipProfile;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

/** An immutable item-value overlay; TWT2 owns consumption, player state, purity and rendering. */
public final class HydrationAdapter {
    public static final int MAX = 20; // Verified TWT2 1.6.2 drink limit; never pass overflowing ints.
    private static volatile Map<Identifier, Hydration> values = Map.of();
    private static volatile boolean localServer;
    private static boolean enabled;

    private HydrationAdapter() { }

    public static void register() {
        enabled = ThirstCompatibility.supported();
        if (!enabled) {
            if (FabricLoader.getInstance().isModLoaded(ThirstCompatibility.MOD_ID)) {
                BuildupVitals.LOGGER.warn("Hydration adapter disabled: only TWT2 {} has been verified", ThirstCompatibility.SUPPORTED_VERSION);
            }
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            localServer = true;
            refresh(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) refresh(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            localServer = false;
            replace(Map.of());
        });
        BuildupVitals.LOGGER.info("Hydration adapter enabled for TWT2 {}", ThirstCompatibility.SUPPORTED_VERSION);
    }

    public static boolean enabled() { return enabled; }

    public static Hydration find(Identifier item) { return values.get(item); }

    public static Hydration bounded(Hydration input) {
        return new Hydration(Math.clamp(input.thirst(), 0, MAX), Math.clamp(input.quenched(), 0, MAX));
    }

    private static void refresh(MinecraftServer server) {
        var snapshot = FoodProfileLoader.snapshot(server);
        var next = new HashMap<Identifier, Hydration>();
        for (var item : snapshot.items()) next.put(item, bounded(snapshot.resolve(item).profile().hydration()));
        replace(next);
    }

    public static void receive(boolean serverEnabled, Map<Identifier, TooltipProfile> profiles) {
        // TWT2 shares its API cache between integrated server and client. Keep the authoritative
        // server overlay while a local server exists; a client packet can never replace gameplay data.
        if (!enabled || localServer) return;
        var next = new HashMap<Identifier, Hydration>();
        if (serverEnabled) profiles.forEach((item, profile) -> next.put(item, bounded(profile.hydration())));
        replace(next);
    }

    public static void disconnect() {
        if (enabled && !localServer) replace(Map.of());
    }

    private static void replace(Map<Identifier, Hydration> next) {
        values = Map.copyOf(next);
        ThirstBridge.invalidate();
    }
}
