package com.davidblackcn.buildupvitals.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public abstract class PeacefulRecoveryMixin {
    // Peaceful hunger/saturation replenishment stays vanilla; health uses the same controller.
    // A call-site guard never intercepts potions, beacons, golden apples or external heal calls.
    @WrapWithCondition(method = "tickRegeneration()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"), require = 1, allow = 1)
    private boolean buildupVitals$coordinatePeacefulHealing(ServerPlayer player, float amount) {
        return false;
    }
}
