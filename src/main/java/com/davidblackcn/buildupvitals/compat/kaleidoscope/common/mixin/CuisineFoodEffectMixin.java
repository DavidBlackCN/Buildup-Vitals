package com.davidblackcn.buildupvitals.compat.kaleidoscope.common.mixin;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineConsumption;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ApplyStatusEffectsConsumeEffect.class)
public abstract class CuisineFoodEffectMixin {
    @WrapOperation(method = "apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean buildupVitals$profileOwnsGrant(LivingEntity entity, MobEffectInstance effect, Operation<Boolean> original,
                                                   Level level, ItemStack stack, LivingEntity user) {
        effect = com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter.review(stack, effect);
        if (effect == null) return false;
        if (entity instanceof ServerPlayer player && !CuisineConsumption.allowEffect(player, stack, effect)) return false;
        return original.call(entity, effect);
    }
}
