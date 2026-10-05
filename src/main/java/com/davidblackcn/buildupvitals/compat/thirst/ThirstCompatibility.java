package com.davidblackcn.buildupvitals.compat.thirst;

import net.fabricmc.loader.api.FabricLoader;

/** No optional API types: safe during Mixin selection and without TWT2 installed. */
public final class ThirstCompatibility {
    public static final String MOD_ID = "thirstwastaken2";
    public static final String SUPPORTED_VERSION = "1.6.2+26.3";

    private ThirstCompatibility() { }

    public static boolean supported() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals(SUPPORTED_VERSION)).orElse(false);
    }
}
