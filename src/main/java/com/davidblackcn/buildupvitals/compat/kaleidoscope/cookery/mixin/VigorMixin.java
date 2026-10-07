package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.effect.VigorEffect")
public abstract class VigorMixin extends MobEffect {
    protected VigorMixin(MobEffectCategory category, int color) { super(category, color); }
    @Override public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        ForeignMealBenefits.started(entity, this);
    }
}
