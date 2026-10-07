package com.davidblackcn.buildupvitals.compat.kaleidoscope.nether.mixin;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="com.bmt.kaleidoscope_nether.effect.StarBlessingEffect")
public abstract class StarEffectMixin extends MobEffect {
    protected StarEffectMixin(MobEffectCategory category,int color){super(category,color);}
    @Inject(method="<init>(I)V",at=@At("TAIL"))
    private void buildupVitals$knockback(int color,CallbackInfo ci) {
        // Override the default amplifier scaling: the bonus remains 0.5 at every level.
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,Identifier.parse("buildup_vitals:star_blessing"),.5,AttributeModifier.Operation.ADD_VALUE);
    }
    @Override public void addAttributeModifiers(AttributeMap attributes,int amplifier){super.addAttributeModifiers(attributes,0);}
    @Override public void createModifiers(int amplifier,java.util.function.BiConsumer<net.minecraft.core.Holder<Attribute>,AttributeModifier> consumer){super.createModifiers(0,consumer);}
    @Inject(method="applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z",at=@At("HEAD"),cancellable=true)
    private void buildupVitals$noPeriodicHeal(ServerLevel level,LivingEntity entity,int amplifier,CallbackInfoReturnable<Boolean> cir){cir.setReturnValue(true);}
}
