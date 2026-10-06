package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/** Semantic adapters, never replacement effects or separately ticking player state. */
public final class ForeignMealBenefits {
    private static final Map<Identifier, Semantics> FOREIGN = new LinkedHashMap<>();
    private ForeignMealBenefits() { }

    public record Semantics(boolean restorative, boolean invigorated) { }

    public static void register(Identifier id, Semantics semantics) {
        if (FOREIGN.putIfAbsent(id, semantics) != null) throw new IllegalStateException("Duplicate foreign benefit " + id);
    }
    public static boolean registered(Identifier id) { return FOREIGN.containsKey(id); }
    public static Optional<Holder<MobEffect>> holder(Identifier id) {
        return BuiltInRegistries.MOB_EFFECT.getOptional(id).map(effect -> BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
    }
    public static java.util.List<Holder<MobEffect>> mainEffects() {
        return Stream.concat(BuildupEffects.mainEffects().stream(), FOREIGN.keySet().stream().flatMap(id -> holder(id).stream())).toList();
    }
    public static void started(LivingEntity entity, MobEffect incoming) {
        if (!entity.level().isClientSide()) {
            for (var other : mainEffects()) if (other.value() != incoming) entity.removeEffect(other);
        }
    }
    public static boolean restorative(LivingEntity entity) {
        return entity.hasEffect(BuildupEffects.RESTORATIVE) || FOREIGN.entrySet().stream()
                .anyMatch(entry -> entry.getValue().restorative() && holder(entry.getKey()).filter(entity::hasEffect).isPresent());
    }
    public static boolean invigorated(LivingEntity entity) {
        return entity.hasEffect(BuildupEffects.INVIGORATED) || FOREIGN.entrySet().stream()
                .anyMatch(entry -> entry.getValue().invigorated() && holder(entry.getKey()).filter(entity::hasEffect).isPresent());
    }
}
