package com.davidblackcn.buildupvitals.mixin;

import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodProperties.class)
public abstract class FoodRecoveryMixin {
    @Shadow public abstract int nutrition();

    @Inject(method = "onConsume(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/component/Consumable;)V",
            at = @At("HEAD"))
    private void buildupVitals$beforeNutrition(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (user instanceof ServerPlayer player) PlayerOvereat.foodConsumed(player, nutrition());
    }

    // 26.3 has no public completed-food event. Called once by Consumable, before stack shrink.
    @Inject(method = "onConsume(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/component/Consumable;)V",
            at = @At("TAIL"))
    private void buildupVitals$foodConsumed(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (user instanceof ServerPlayer player) {
            PlayerRecovery.foodConsumed(player, stack);
        }
    }
}
