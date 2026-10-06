package com.davidblackcn.buildupvitals.compat.farmersdelight.mixin;

import com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Consumable.class)
public abstract class BeverageMixin {
    @Inject(method = "onConsume(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void buildupVitals$drinkProfile(Level level, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (user instanceof ServerPlayer player && FarmersDelightCompatibility.beverage(stack)) PlayerRecovery.foodConsumed(player, stack);
    }
}
