package com.davidblackcn.buildupvitals.compat.kaleidoscope.end.mixin;
import com.bmt.kaleidoscope_end.init.KEEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.bmt.kaleidoscope_end.event.KEPlayerEvents")
public abstract class EndDamageEventMixin {
    @Inject(method="onAllowDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)Z",at=@At("HEAD"),cancellable=true)
    private static void buildupVitals$noMintShieldOrImmortality(LivingEntity entity,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(!(entity.hasEffect(KEEffects.DREAM)&&source.is(DamageTypeTags.IS_FALL)));
    }
}
