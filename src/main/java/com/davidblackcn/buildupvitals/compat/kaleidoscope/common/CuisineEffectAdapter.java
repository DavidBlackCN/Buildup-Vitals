package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;

import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Special cuisine effects coexist; only explicit main-benefit semantics reserve the main slot. */
public final class CuisineEffectAdapter {
    public record Semantics(boolean mainMealBenefit, boolean restorative, boolean invigorated,
                            boolean damageReduction, boolean recovery) { }
    private static final Map<Identifier, Semantics> EFFECTS = new LinkedHashMap<>();
    private static final Map<Identifier, ToDoubleFunction<ServerPlayer>> STABLE_SPEEDS = new LinkedHashMap<>();
    private static final Map<String, ToDoubleFunction<net.minecraft.world.item.ItemStack>> RECOVERY = new LinkedHashMap<>();
    private CuisineEffectAdapter() { }
    public static void register(Identifier id, Semantics semantics) {
        if (EFFECTS.putIfAbsent(id, semantics) != null) throw new IllegalStateException("Duplicate cuisine effect " + id);
        if (semantics.mainMealBenefit()) ForeignMealBenefits.register(id,
                new ForeignMealBenefits.Semantics(semantics.restorative(), semantics.invigorated()));
    }
    public static boolean isForeignCuisineEffect(Identifier id) { return EFFECTS.containsKey(id); }
    public static Semantics semantics(Identifier id) { return EFFECTS.get(id); }
    public static boolean has(LivingEntity entity, Identifier id) {
        return EFFECTS.containsKey(id) && ForeignMealBenefits.holder(id).filter(entity::hasEffect).isPresent();
    }
    public static void registerStableSpeed(Identifier id, ToDoubleFunction<ServerPlayer> speed) { STABLE_SPEEDS.put(id, speed); }
    public static void registerRecovery(String namespace, ToDoubleFunction<net.minecraft.world.item.ItemStack> recovery) { RECOVERY.put(namespace, recovery); }
    public static double recovery(net.minecraft.world.item.ItemStack stack) {
        var adapter = RECOVERY.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
        return adapter == null ? 0 : adapter.applyAsDouble(stack);
    }
    public static double stableSpeed(ServerPlayer player) {
        // Multiple environmental cuisine bonuses choose the strongest, never multiply each other.
        return STABLE_SPEEDS.entrySet().stream().filter(entry -> has(player, entry.getKey()))
                .mapToDouble(entry -> entry.getValue().applyAsDouble(player)).max().orElse(1);
    }
}
