package com.davidblackcn.buildupvitals.compat.thirst.mixin;

import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Verified against the 1.6.2+26.3 release bytecode; disabled for every other version. */
@Mixin(targets = "com.thirstwastaken2.api.ThirstApi", remap = false)
public abstract class ThirstValuesMixin {
    @ModifyExpressionValue(method = "resolve(Lnet/minecraft/world/item/Item;)[I",
            at = @At(value = "INVOKE", target = "Lcom/thirstwastaken2/data/DataPackDrinks;get(Lnet/minecraft/world/item/Item;)[I"),
            require = 1, allow = 1)
    private static int[] buildupVitals$profileFallback(int[] dataPackValue, Item item) {
        // Config/blacklist already returned; an explicit data-pack zero must also win.
        if (dataPackValue != null) return dataPackValue;
        var hydration = HydrationAdapter.find(BuiltInRegistries.ITEM.getKey(item));
        return hydration == null ? null : new int[]{hydration.thirst(), hydration.quenched()};
    }
}
