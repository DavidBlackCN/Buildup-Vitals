package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.CookeryEffects;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ShieldDamageMixin {
    @ModifyReturnValue(method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"))
    private float buildupVitals$shield(float damage, DamageSource source, float input) {
        return (Object) this instanceof ServerPlayer player ? CookeryEffects.shield(player, source, damage) : damage;
    }
}
