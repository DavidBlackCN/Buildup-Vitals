package com.davidblackcn.buildupvitals.compat.farmersdelight.mixin;

import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "vectorwing.farmersdelight.common.effect.NourishmentEffect")
public abstract class NourishmentMixin extends MobEffect {
    protected NourishmentMixin(MobEffectCategory category, int color) { super(category, color); }

    // 3.6.27 refunds exhaustion here; it has no direct heal call. Keep the effect alive, but disable that economy.
    @Inject(method = "applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z",
            at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void buildupVitals$economy(ServerLevel level, LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayer) cir.setReturnValue(true);
    }

    @Override public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        if (entity instanceof ServerPlayer) ForeignMealBenefits.started(entity, this);
    }
}
