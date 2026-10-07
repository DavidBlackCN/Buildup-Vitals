package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.effect.WarmthEffect")
public abstract class WarmthMixin {
    @Inject(method = "applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void buildupVitals$controllerOwnsRecovery(ServerLevel level, LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayer) cir.setReturnValue(true);
    }
}
