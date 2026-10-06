package com.davidblackcn.buildupvitals.compat.thirst.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Buildup's one recovery clock owns the quenched bonus; never changes the user's config. */
@Mixin(targets = "com.thirstwastaken2.data.HealthRegen", remap = false)
public abstract class QuenchedRecoveryMixin {
    @Inject(method = "healWithQuenched(Lnet/minecraft/server/level/ServerPlayer;Lcom/thirstwastaken2/data/ThirstData;Lcom/thirstwastaken2/data/ExhaustionTracker;)F",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void buildupVitals$oneRecoveryClock(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0F);
    }
}
