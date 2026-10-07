package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets = {"com.github.ysbbbbbb.kaleidoscopetavern.item.DrinkBlockItem", "com.github.ysbbbbbb.kaleidoscopetavern.item.CocktailBlockItem", "com.github.ysbbbbbb.kaleidoscopetavern.item.SignatureCocktailBlockItem"})
public abstract class DrinkEffectsMixin {
    @Inject(method = "addDrinkEffect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void buildupVitals$budget(ItemStack stack, Level level, LivingEntity entity, CallbackInfo ci) {
        TavernEffects.apply(stack, level, entity); ci.cancel();
    }
}
