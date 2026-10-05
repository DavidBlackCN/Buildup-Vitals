package com.davidblackcn.buildupvitals.mixin;

import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class NaturalRecoveryMixin {
    // This sole Boolean unboxing is the natural-regeneration gate, not the global gamerule.
    // Disable only the two vanilla healing branches, preserving exhaustion and starvation.
    @ModifyExpressionValue(method = "tick(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z"), require = 1, allow = 1)
    private boolean buildupVitals$replaceNaturalRecovery(boolean original) {
        return false;
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("TAIL"))
    private void buildupVitals$tickRecovery(ServerPlayer player, CallbackInfo ci) {
        PlayerRecovery.tick(player);
    }
}
