package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.data.DrinkEffectData;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(DrinkEffectData.class)
public abstract class DrinkDataMixin {
    @ModifyReturnValue(method = "effects()Ljava/util/List;", at = @At("RETURN"))
    private List<List<DrinkEffectData.Entry>> buildupVitals$budget(List<List<DrinkEffectData.Entry>> original) {
        return original.stream().map(row -> row.stream().map(TavernEffects::cap).toList()).toList();
    }
}
