package com.davidblackcn.buildupvitals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.minecraft.resources.Identifier;

/** Test-only failure after profile preparation; proves failed reloads cannot publish display data. */
public class TooltipReloadFailure implements ModInitializer {
    static final AtomicBoolean FAIL_NEXT = new AtomicBoolean();

    @Override
    public void onInitialize() {
        DataResourceLoader.get().registerReloadListener(Identifier.parse("buildup_vitals_test:tooltip_reload_failure"),
                (state, prepare, barrier, apply) -> FAIL_NEXT.getAndSet(false)
                        ? CompletableFuture.failedFuture(new IllegalStateException("Intentional Stage 5 reload failure"))
                        : barrier.<Void>wait(null));
    }
}
