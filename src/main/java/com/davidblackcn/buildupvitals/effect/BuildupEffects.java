package com.davidblackcn.buildupvitals.effect;

import com.davidblackcn.buildupvitals.BuildupVitals;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class BuildupEffects {
    public static final Holder<MobEffect> RESTORATIVE = main("restorative", 0xD85C64);
    public static final Holder<MobEffect> INVIGORATED = main("invigorated", 0xE5BE49);
    public static final Holder<MobEffect> STEADY = main("steady", 0x68B7B0);

    public static java.util.List<Holder<MobEffect>> mainEffects() { return java.util.List.of(RESTORATIVE, INVIGORATED, STEADY); }
    public static Holder<MobEffect> forType(com.davidblackcn.buildupvitals.food.benefit.MealBenefitType type) {
        return switch (type) { case RESTORATIVE -> RESTORATIVE; case INVIGORATED -> INVIGORATED; case STEADY -> STEADY; };
    }
    private static Holder<MobEffect> main(String name, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, name), new MobEffect(MobEffectCategory.BENEFICIAL, color) {
                    @Override public void onEffectStarted(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
                        if (!entity.level().isClientSide()) {
                            com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits.started(entity, this);
                        }
                    }
                });
    }

    public static final Holder<MobEffect> OVERFULL = register("overfull", MobEffectCategory.HARMFUL, 0xA87542);
    private BuildupEffects() { }
    private static Holder<MobEffect> register(String name, MobEffectCategory category, int color) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, name), new MobEffect(category, color) { });
    }
    public static void register() { }
}
