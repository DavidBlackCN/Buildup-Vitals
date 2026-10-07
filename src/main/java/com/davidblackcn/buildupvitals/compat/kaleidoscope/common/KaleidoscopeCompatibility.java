package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.CookeryEffects;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class KaleidoscopeCompatibility {
    private KaleidoscopeCompatibility() { }
    public static void register() {
        for (var entry : KaleidoscopeVersion.TESTED.entrySet()) {
            String id = "kaleidoscope_" + entry.getKey();
            if (!FabricLoader.getInstance().isModLoaded(id)) continue;
            if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, id),
                    FabricLoader.getInstance().getModContainer(BuildupVitals.MOD_ID).orElseThrow(),
                    Component.literal("Buildup Vitals: " + id), PackActivationType.ALWAYS_ENABLED)) {
                throw new IllegalStateException("Missing profile pack " + id);
            }
            if (!KaleidoscopeVersion.supported(entry.getKey())) BuildupVitals.LOGGER.warn(
                    "Unsupported {} version: profiles available, deep cuisine bridge DISABLED (tested {})", id, entry.getValue());
        }
        if (KaleidoscopeVersion.supported("cookery")) CookeryEffects.register();
        if (KaleidoscopeVersion.supported("end")) com.davidblackcn.buildupvitals.compat.kaleidoscope.end.EndEffects.register();
        if (KaleidoscopeVersion.supported("nether")) com.davidblackcn.buildupvitals.compat.kaleidoscope.nether.NetherEffects.register();
        if (KaleidoscopeVersion.supported("tavern")) com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects.register();
    }
}
