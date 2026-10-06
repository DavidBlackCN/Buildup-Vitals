package com.davidblackcn.buildupvitals.compat.farmersdelight.mixin;

import com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also covers direct command/addon grants and forceAddEffect, without touching independent healing. */
@Mixin(LivingEntity.class)
public abstract class NourishmentGrantMixin {
    @Inject(method = "canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"),
            cancellable = true, require = 1, allow = 1)
    private void buildupVitals$overfull(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && FarmersDelightCompatibility.nourishment(effect)
                && PlayerOvereat.overfull(player)) cir.setReturnValue(false);
    }
}
