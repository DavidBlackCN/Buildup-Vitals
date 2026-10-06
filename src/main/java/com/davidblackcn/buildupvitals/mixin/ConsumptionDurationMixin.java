package com.davidblackcn.buildupvitals.mixin;

import com.davidblackcn.buildupvitals.food.consumption.Consumption;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class ConsumptionDurationMixin {
    @ModifyReturnValue(method = "getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), require = 1, allow = 1)
    private int buildupVitals$duration(int original, LivingEntity user) {
        return Consumption.duration((ItemStack) (Object) this, user, original);
    }
}
