package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = {"com.github.ysbbbbbb.kaleidoscopetavern.item.DrinkBlockItem", "com.github.ysbbbbbb.kaleidoscopetavern.item.CocktailBlockItem", "com.github.ysbbbbbb.kaleidoscopetavern.item.JuiceBucketItem"})
public abstract class DrinkConsumptionMixin {
    @Inject(method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void buildupVitals$consume(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (entity instanceof ServerPlayer player) {
            var food = stack.get(DataComponents.FOOD);
            if (food != null) food.onConsume(level, entity, stack, stack.get(DataComponents.CONSUMABLE));
            else PlayerRecovery.foodConsumed(player, stack);
        }
    }
}
