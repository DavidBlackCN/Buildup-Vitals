package com.davidblackcn.buildupvitals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Consumable.class)
public abstract class FullHungerEatingMixin {
    @WrapOperation(method = "startConsuming(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/Consumable;consumeTicks()I"), require = 1, allow = 1)
    private int buildupVitals$timedUse(Consumable consumable, Operation<Integer> original,
            net.minecraft.world.entity.LivingEntity user, net.minecraft.world.item.ItemStack stack, net.minecraft.world.InteractionHand hand) {
        return com.davidblackcn.buildupvitals.food.consumption.Consumption.duration(stack, user, original.call(consumable));
    }

    @WrapOperation(method = "canConsume(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"), require = 1, allow = 1)
    private boolean buildupVitals$foodAtFullHunger(Player player, boolean always, Operation<Boolean> original) {
        return original.call(player, always || !player.isSpectator());
    }
}
