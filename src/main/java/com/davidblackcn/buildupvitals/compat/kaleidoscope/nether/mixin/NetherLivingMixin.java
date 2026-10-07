package com.davidblackcn.buildupvitals.compat.kaleidoscope.nether.mixin;
import com.bmt.kaleidoscope_nether.init.KNEffects;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Selects the pinned upstream mixins; selectors target their merged handlers, not global hasEffect. */
@Mixin(value=LivingEntity.class,priority=1100)
public abstract class NetherLivingMixin {
    @Dynamic("Merged handlers from Nether StarBlessingMixin / LivingEntityMixin")
    @WrapOperation(method={"@buildup:merged(com.bmt.kaleidoscope_nether.mixins.kaleidoscope_nether.StarBlessingMixin#onAddEffect)","@buildup:merged(com.bmt.kaleidoscope_nether.mixins.kaleidoscope_nether.StarBlessingMixin#onKnockback)","@buildup:merged(com.bmt.kaleidoscope_nether.mixins.kaleidoscope_nether.LivingEntityMixin#onGetDamageAfterArmorAbsorb)"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean buildupVitals$removeLegacyGate(LivingEntity entity,Holder<MobEffect> effect,Operation<Boolean> original) {
        return !effect.equals(KNEffects.STAR_BLESSING) && !effect.equals(KNEffects.CRIMSON) && original.call(entity,effect);
    }
    @WrapOperation(method="getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float buildupVitals$physicalPenetration(LivingEntity target,float damage,DamageSource source,float armor,float toughness,Operation<Float> original) {
        if(source.getEntity() instanceof LivingEntity attacker && attacker.hasEffect(KNEffects.CRIMSON))armor*=.75F;
        return original.call(target,damage,source,armor,toughness);
    }
    @ModifyReturnValue(method="getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",at=@At("RETURN"))
    private float buildupVitals$star(float damage,DamageSource source,float incoming) {
        return ((LivingEntity)(Object)this).hasEffect(KNEffects.STAR_BLESSING) && !source.is(DamageTypeTags.BYPASSES_EFFECTS) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE) ? damage*.8F:damage;
    }
}
