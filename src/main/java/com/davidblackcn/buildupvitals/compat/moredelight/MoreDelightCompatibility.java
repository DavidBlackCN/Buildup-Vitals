package com.davidblackcn.buildupvitals.compat.moredelight;

import com.davidblackcn.buildupvitals.BuildupVitals;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Data-only addon integration. FD owns Nourishment; no addon classes or extra Mixins are needed. */
public final class MoreDelightCompatibility {
    public static final String MOD_ID = "moredelight";
    // Fabric normalizes the release's numeric 09 component to 9.
    public static final String TESTED_VERSION = "26.9.16-26.3-fabric";

    private MoreDelightCompatibility() { }

    public static void register() {
        var mod = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (mod.isEmpty()) return;
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "more_delight"),
                FabricLoader.getInstance().getModContainer(BuildupVitals.MOD_ID).orElseThrow(),
                Component.literal("Buildup Vitals: More Delight"), PackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("Missing built-in More Delight profile pack");
        }
        if (!TESTED_VERSION.equals(mod.get().getMetadata().getVersion().getFriendlyString())) {
            BuildupVitals.LOGGER.warn("Untested More Delight version: profiles remain available (tested {})", TESTED_VERSION);
        }
    }
}
