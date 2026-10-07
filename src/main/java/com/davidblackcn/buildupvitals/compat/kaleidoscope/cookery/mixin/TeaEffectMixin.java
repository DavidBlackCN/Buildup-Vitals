package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineConsumption;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem")
public abstract class TeaEffectMixin {
    @WrapOperation(method = "addTeaEffect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean buildupVitals$budget(LivingEntity entity, MobEffectInstance effect, Operation<Boolean> original) {
        if (entity instanceof ServerPlayer player && !CuisineConsumption.allowEffect(player, ((Item) (Object) this).getDefaultInstance(), effect)) return false;
        return original.call(entity, effect);
    }
}
