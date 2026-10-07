package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.data.DrinkEffectData;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.util.CocktailEffectHelper")
public abstract class CocktailMergeMixin {
    @Inject(method = "mergeEffects(Ljava/util/List;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private static void buildupVitals$merge(List<DrinkEffectData.Entry> entries, CallbackInfoReturnable<List<DrinkEffectData.Entry>> cir) { cir.setReturnValue(TavernEffects.merge(entries)); }
}
