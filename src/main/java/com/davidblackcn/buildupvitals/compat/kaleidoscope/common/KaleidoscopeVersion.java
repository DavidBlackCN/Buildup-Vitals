package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;

import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Metadata only: safe before Minecraft/Mixin bootstrap. */
public final class KaleidoscopeVersion {
    public static final Map<String, String> TESTED = Map.of("cookery", "1.6.0.3-fabric+mc26.3", "tavern", "1.2.0.11-fabric+mc26.3");
    private KaleidoscopeVersion() { }
    public static boolean supported(String module) {
        return FabricLoader.getInstance().getModContainer("kaleidoscope_" + module)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals(TESTED.get(module))).orElse(false);
    }
}
