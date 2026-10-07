package com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.*;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.data.DrinkEffectData;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.resources.DrinkEffectDataReloadListener;
import com.github.ysbbbbbb.kaleidoscopetavern.item.*;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reviewed budgets apply to Tavern sources, never to ordinary potions or global healing. */
public final class TavernEffects {
    public static final Identifier BLOODY_MARY = Identifier.parse("kaleidoscope_tavern:bloody_mary");
    private TavernEffects() { }
    public static void register() {
        for (String name : new String[]{"slightly_tipsy", "high_heels", "grass_stealth", "vision", "bloody_mary", "ardent_heat", "long_reach", "tomb_raider", "xp_drain", "upside_down", "zenith", "shriek_attack"})
            CuisineEffectAdapter.register(Identifier.parse("kaleidoscope_tavern:" + name), new CuisineEffectAdapter.Semantics(false, false, false, false, name.equals("bloody_mary")));
        CuisineEffectAdapter.registerRecovery("kaleidoscope_tavern", TavernEffects::recovery);
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (source.getEntity() instanceof ServerPlayer killer && killer != victim && CuisineEffectAdapter.has(killer, BLOODY_MARY))
                PlayerRecovery.addReserve(killer, CuisineEffectBudget.killRecovery(victim.getMaxHealth()));
        });
    }
    public static boolean healing(Holder<MobEffect> effect) { return effect.equals(MobEffects.REGENERATION) || effect.equals(MobEffects.INSTANT_HEALTH); }
    public static DrinkEffectData.Entry cap(DrinkEffectData.Entry entry) {
        // Costs retain their own probability, amplitude and duration.
        if (entry.effect().value().getCategory() != MobEffectCategory.BENEFICIAL) return entry;
        var limit = CuisineEffectBudget.tavern(entry.effect().unwrapKey().orElseThrow().identifier().toString(), entry.amplifier());
        return new DrinkEffectData.Entry(entry.effect(), entry.duration() < 0 ? limit.seconds() : Math.min(entry.duration(), limit.seconds()),
                Math.clamp(entry.amplifier(), 0, limit.maxAmplifier()), entry.probability());
    }
    public static List<DrinkEffectData.Entry> entries(ItemStack stack) {
        if (stack.getItem() instanceof SignatureCocktailBlockItem) return SignatureCocktailBlockItem.getEffects(stack);
        var data = DrinkEffectDataReloadListener.INSTANCE.get(stack.getItem());
        if (data == null || data.effects().isEmpty()) return List.of();
        int level = stack.getItem() instanceof CocktailBlockItem ? 1 : BottleBlockItem.getBrewLevel(stack);
        return level < 1 ? List.of() : data.effects().get(Math.min(level, data.effects().size()) - 1);
    }
    public static double recovery(ItemStack stack) {
        if (stack.getItem() instanceof DrinkBlockItem) {
            var data = DrinkEffectDataReloadListener.INSTANCE.get(stack.getItem());
            if (data != null && data.effects().stream().flatMap(List::stream).anyMatch(e -> healing(e.effect()) && e.probability() > 0))
                return CuisineEffectBudget.brewRecovery(BottleBlockItem.getBrewLevel(stack));
        } else if (stack.getItem() instanceof CocktailBlockItem && entries(stack).stream().anyMatch(e -> healing(e.effect()) && e.probability() > 0)) return 3;
        return 0;
    }
    public static void apply(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel server)) return;
        for (var raw : entries(stack)) {
            var entry = cap(raw);
            if (healing(entry.effect()) || level.getRandom().nextFloat() >= entry.probability()) continue;
            if (entry.effect().value().isInstantaneous()) entry.effect().value().applyInstantaneousEffect(server, entity, entity, entity, entry.amplifier(), 1);
            else entity.addEffect(new MobEffectInstance(entry.effect(), entry.duration() * 20, entry.amplifier()));
        }
    }
    public static List<DrinkEffectData.Entry> merge(List<DrinkEffectData.Entry> entries) {
        var groups = new LinkedHashMap<Holder<MobEffect>, List<DrinkEffectData.Entry>>();
        for (var e : entries) groups.computeIfAbsent(e.effect(), key -> new ArrayList<>()).add(e);
        var result = new ArrayList<DrinkEffectData.Entry>();
        groups.forEach((effect, group) -> result.add(cap(new DrinkEffectData.Entry(effect,
                (int)Math.min(Integer.MAX_VALUE, CuisineEffectBudget.mergedDuration(group.stream().map(DrinkEffectData.Entry::duration).toList())),
                group.stream().mapToInt(DrinkEffectData.Entry::amplifier).max().orElse(0),
                (float)group.stream().mapToDouble(DrinkEffectData.Entry::probability).max().orElse(0)))));
        return List.copyOf(result);
    }
}
