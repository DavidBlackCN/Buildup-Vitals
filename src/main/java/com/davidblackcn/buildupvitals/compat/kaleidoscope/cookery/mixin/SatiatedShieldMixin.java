package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace only the upstream shield callback; never recursively call hurt. */
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.event.effect.SatiatedShieldEvent")
public abstract class SatiatedShieldMixin {
    @Inject(method = "register()V", at = @At("HEAD"), cancellable = true)
    private static void buildupVitals$usePostArmorShield(CallbackInfo ci) { ci.cancel(); }
}
