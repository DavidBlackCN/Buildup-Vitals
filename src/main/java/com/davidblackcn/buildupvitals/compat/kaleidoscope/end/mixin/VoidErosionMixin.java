package com.davidblackcn.buildupvitals.compat.kaleidoscope.end.mixin;
import com.bmt.kaleidoscope_end.init.KEEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=LivingEntity.class,priority=1100)
public abstract class VoidErosionMixin {
    @Unique private static boolean buildupVitals$erosion(DamageSource source){return source.getEntity() instanceof LivingEntity attacker&&attacker.hasEffect(KEEffects.VOID_EROSION);}
    @Dynamic("Pinned End LivingEntityMixin merged handler")
    @WrapOperation(method="@buildup:merged(com.bmt.kaleidoscope_end.mixins.kaleidoscope_end.LivingEntityMixin#onGetDamageAfterArmorAbsorb)",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean buildupVitals$removeBypass(LivingEntity entity,Holder<MobEffect> effect,Operation<Boolean> original){return !effect.equals(KEEffects.VOID_EROSION)&&original.call(entity,effect);}
    @WrapOperation(method="getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float buildupVitals$armor(LivingEntity target,float damage,DamageSource source,float armor,float toughness,Operation<Float> original){return original.call(target,damage,source,buildupVitals$erosion(source)?armor*.85F:armor,toughness);}
    @WrapOperation(method="getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",at=@At(value="INVOKE",target="Ljava/lang/Math;max(FF)F",ordinal=0))
    private float buildupVitals$resistance(float value,float floor,Operation<Float> original,DamageSource source,float incoming){
        float reduced=original.call(value,floor);return buildupVitals$erosion(source)?incoming-(incoming-reduced)*.85F:reduced;
    }
    @WrapOperation(method="getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float buildupVitals$protection(float damage,float protection,Operation<Float> original,DamageSource source,float incoming){return original.call(damage,buildupVitals$erosion(source)?Math.min(protection,20)*.85F:protection);}
}
