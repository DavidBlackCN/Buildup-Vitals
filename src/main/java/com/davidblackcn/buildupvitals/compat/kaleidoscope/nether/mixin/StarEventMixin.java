package com.davidblackcn.buildupvitals.compat.kaleidoscope.nether.mixin;
import com.bmt.kaleidoscope_nether.api.event.LivingDamageModifyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.bmt.kaleidoscope_nether.event.StarBlessingEffectEvents")
public abstract class StarEventMixin {
    @Inject(method="onLivingDamage(Lcom/bmt/kaleidoscope_nether/api/event/LivingDamageModifyEvent;)V",at=@At("HEAD"),cancellable=true)
    private static void buildupVitals$postArmorOnly(LivingDamageModifyEvent event,CallbackInfo ci){ci.cancel();}
}
