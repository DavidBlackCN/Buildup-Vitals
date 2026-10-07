package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.event.EffectEvent")
public abstract class BloodyMaryMixin {
    @Inject(method = "onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private static void buildupVitals$realDeath(LivingEntity entity, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) { cir.setReturnValue(true); }
}
