package com.davidblackcn.buildupvitals.compat.kaleidoscope.common.mixin;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class CuisineGrantMixin {
    @Inject(method = "canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void buildupVitals$overfull(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (CuisineEffectAdapter.isForeignCuisineEffect(id) && ForeignMealBenefits.registered(id)
                && (Object) this instanceof ServerPlayer player && PlayerOvereat.overfull(player)) cir.setReturnValue(false);
    }
}
