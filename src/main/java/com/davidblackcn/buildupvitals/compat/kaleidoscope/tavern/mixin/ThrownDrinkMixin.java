package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.mixin;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.item.DrinkBlockItem")
public abstract class ThrownDrinkMixin {
    @ModifyReturnValue(method = "getEffectInstances(Lnet/minecraft/world/level/Level;I)Ljava/util/List;", at = @At("RETURN"))
    private List<MobEffectInstance> buildupVitals$noHealingPotion(List<MobEffectInstance> effects) { return effects.stream().filter(e -> !TavernEffects.healing(e.getEffect())).toList(); }
}
