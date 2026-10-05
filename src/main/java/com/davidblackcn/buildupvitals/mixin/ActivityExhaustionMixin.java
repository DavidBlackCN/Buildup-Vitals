package com.davidblackcn.buildupvitals.mixin;

import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayer.class)
public abstract class ActivityExhaustionMixin {
    // Verified 26.3: six movement sites (water, sprint, zero-cost walk/crouch), two jump sites.
    // Climbing has no vanilla exhaustion call. Never wrap causeFoodExhaustion globally.
    @ModifyArg(method = "checkMovementStatistics(DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"),
            index = 0, require = 6, allow = 6)
    private float buildupVitals$movementExhaustion(float amount) {
        return PlayerMealBenefits.activityExhaustion((ServerPlayer) (Object) this, amount);
    }

    @ModifyArg(method = "jumpFromGround()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"),
            index = 0, require = 2, allow = 2)
    private float buildupVitals$jumpExhaustion(float amount) {
        return PlayerMealBenefits.activityExhaustion((ServerPlayer) (Object) this, amount);
    }
}
