package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.data.DrinkEffectData;
import com.github.ysbbbbbb.kaleidoscopetavern.item.SignatureCocktailBlockItem;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(SignatureCocktailBlockItem.class)
public abstract class SignatureDataMixin {
    @ModifyReturnValue(method = "getEffects(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"))
    private static List<DrinkEffectData.Entry> buildupVitals$budget(List<DrinkEffectData.Entry> original) { return original.stream().map(TavernEffects::cap).toList(); }
}
