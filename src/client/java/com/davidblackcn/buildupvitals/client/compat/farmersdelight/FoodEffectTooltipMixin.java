package com.davidblackcn.buildupvitals.client.compat.farmersdelight;

import com.davidblackcn.buildupvitals.client.FarmersDelightTooltips;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "vectorwing.farmersdelight.common.utility.TextUtils")
public abstract class FoodEffectTooltipMixin {
    @ModifyVariable(method = "addFoodEffectTooltip(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;FF)V",
            at = @At("HEAD"), argsOnly = true, require = 1, allow = 1)
    private static Consumer<Component> buildupVitals$oneBenefitLine(Consumer<Component> original, ItemStack stack,
            Consumer<Component> unused, float durationFactor, float tickRate) {
        return line -> { if (!FarmersDelightTooltips.suppress(stack, line)) original.accept(line); };
    }
}
