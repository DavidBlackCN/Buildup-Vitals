package com.davidblackcn.buildupvitals.client.compat.farmersdelight;

import com.davidblackcn.buildupvitals.client.FarmersDelightTooltips;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "vectorwing.farmersdelight.client.event.TooltipEvents")
public abstract class VanillaSoupTooltipMixin {
    @Inject(method = "addTooltipToVanillaSoups(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/TooltipFlag;Ljava/util/List;)V",
            at = @At("TAIL"), require = 1, allow = 1)
    private static void buildupVitals$effectiveProfile(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines, CallbackInfo ci) {
        lines.removeIf(line -> FarmersDelightTooltips.suppress(stack, line));
    }
}
