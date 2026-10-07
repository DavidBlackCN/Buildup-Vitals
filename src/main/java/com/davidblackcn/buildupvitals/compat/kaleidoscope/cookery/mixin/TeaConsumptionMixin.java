package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem", "com.github.ysbbbbbb.kaleidoscopecookery.item.ClayPotMilkTeaItem"})
public abstract class TeaConsumptionMixin {
    @Inject(method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void buildupVitals$profile(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        // TWT2 already handles ItemStack.finishUsingItem, including these independent item implementations.
        if (entity instanceof ServerPlayer player) PlayerRecovery.foodConsumed(player, stack);
    }
}
