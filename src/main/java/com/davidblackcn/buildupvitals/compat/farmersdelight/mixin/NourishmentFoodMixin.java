package com.davidblackcn.buildupvitals.compat.farmersdelight.mixin;

import com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ApplyStatusEffectsConsumeEffect.class)
public abstract class NourishmentFoodMixin {
    @Redirect(method = "apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), require = 1, allow = 1)
    private boolean buildupVitals$profileOwnsGrant(LivingEntity entity, MobEffectInstance effect, Level level, ItemStack stack, LivingEntity user) {
        if (entity instanceof ServerPlayer player && FarmersDelightCompatibility.nourishment(effect)
                && !FarmersDelightCompatibility.allowFoodEffect(player, stack)) return false;
        return entity.addEffect(effect);
    }
}
