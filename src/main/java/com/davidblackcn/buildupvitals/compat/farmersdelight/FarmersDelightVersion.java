package com.davidblackcn.buildupvitals.compat.farmersdelight;

import net.fabricmc.loader.api.FabricLoader;

/** Mixin-bootstrap safe: use only Loader metadata, never Minecraft or runtime adapter classes. */
public final class FarmersDelightVersion {
    public static final String MOD_ID = "farmersdelight";
    public static final String SUPPORTED_VERSION = "26.3-3.6.27+refabricated";

    private FarmersDelightVersion() { }

    public static boolean supported() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> SUPPORTED_VERSION.equals(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
    }
}
