package com.davidblackcn.buildupvitals.compat.kaleidoscope.nether.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.bmt.kaleidoscope_nether.effect.CrimsonEffect")
public abstract class CrimsonEffectMixin {
    @Inject(method="calculateDamageBonus(Lnet/minecraft/world/entity/LivingEntity;F)F",at=@At("HEAD"),cancellable=true)
    private static void buildupVitals$noMultiplier(LivingEntity attacker,float damage,CallbackInfoReturnable<Float> cir){cir.setReturnValue(damage);}
    @Inject(method="calculateArmorPenetration(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)F",at=@At("HEAD"),cancellable=true)
    private static void buildupVitals$armor(LivingEntity attacker,LivingEntity target,CallbackInfoReturnable<Float> cir){cir.setReturnValue(target.getArmorValue()*(attacker.hasEffect(com.bmt.kaleidoscope_nether.init.KNEffects.CRIMSON)?.75F:1));}
    @Inject(method="getDamageMultiplier(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)F",at=@At("HEAD"),cancellable=true)
    private static void buildupVitals$legacy(LivingEntity attacker,LivingEntity target,CallbackInfoReturnable<Float> cir){cir.setReturnValue(1F);}
}
