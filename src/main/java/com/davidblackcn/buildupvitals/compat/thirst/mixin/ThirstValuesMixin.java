package com.davidblackcn.buildupvitals.compat.thirst.mixin;

import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Verified 1.6.2+26.3: blacklist returns before the first config Map lookup. */
@Mixin(targets = "com.thirstwastaken2.api.ThirstApi", remap = false)
public abstract class ThirstValuesMixin {
    @Shadow @Final private static int[] NONE;

    @Inject(method = "resolve(Lnet/minecraft/world/item/Item;)[I",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0),
            cancellable = true, require = 1, allow = 1)
    private static void buildupVitals$explicitProfile(Item item, CallbackInfoReturnable<int[]> cir) {
        var hydration = HydrationAdapter.find(BuiltInRegistries.ITEM.getKey(item));
        if (hydration != null) cir.setReturnValue(hydration.thirst() == 0 && hydration.quenched() == 0
                ? NONE : new int[]{hydration.thirst(), hydration.quenched()});
    }

    @Inject(method = "thirstValues(Lnet/minecraft/world/item/ItemStack;)[I", at = @At("RETURN"), cancellable = true)
    private static void buildupVitals$pureBottle(ItemStack stack, CallbackInfoReturnable<int[]> cir) {
        if (stack.is(Items.POTION) && !stack.isEmpty() && ThirstApi.purity(stack) == ThirstApi.maxPurity()
                && !ThirstConfig.get().itemBlacklist.contains("minecraft:potion")
                && HydrationAdapter.find(BuiltInRegistries.ITEM.getKey(stack.getItem())) == null) {
            cir.setReturnValue(new int[]{10, 8});
        }
    }
}
