package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class KaleidoscopeMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.contains(".kaleidoscope.")) return false;
        String module = mixin.substring(mixin.indexOf(".kaleidoscope.") + ".kaleidoscope.".length()).split("\\.")[0];
        return module.equals("common") ? KaleidoscopeVersion.TESTED.keySet().stream().anyMatch(KaleidoscopeVersion::supported)
                : KaleidoscopeVersion.supported(module);
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
