package com.davidblackcn.buildupvitals.compat;

import java.net.URLClassLoader;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MixinBootstrapTest {
    @Test
    void optionalMixinSelectionMustNotLoadMinecraftClasses() throws Exception {
        var classes = com.davidblackcn.buildupvitals.compat.thirst.ThirstMixinPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        try (var isolated = new URLClassLoader(new java.net.URL[]{classes}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.") || name.startsWith("vectorwing.farmersdelight.")
                        || name.startsWith("com.thirstwastaken2.")) {
                    throw new ClassNotFoundException("Game/optional classes must not load during Mixin selection: " + name);
                }
                if (name.startsWith("com.davidblackcn.buildupvitals.")) {
                    var loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
                return super.loadClass(name, resolve);
            }
        }) {
            for (String plugin : new String[]{"farmersdelight.FarmersDelightMixinPlugin", "thirst.ThirstMixinPlugin"}) {
                var instance = (IMixinConfigPlugin) isolated.loadClass("com.davidblackcn.buildupvitals.compat." + plugin)
                        .getConstructor().newInstance();
                // Unit tests have no installed mods; selection still must not initialize game classes.
                assertFalse(assertDoesNotThrow(() -> instance.shouldApplyMixin("unused.Target", "unused.Mixin"), plugin));
            }
        }
    }
}
