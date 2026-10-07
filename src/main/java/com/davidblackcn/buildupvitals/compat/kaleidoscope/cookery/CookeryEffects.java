package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectBudget;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CookeryEffects {
    public static final Identifier VIGOR = id("vigor"), WARMTH = id("warmth"), SHIELD = id("satiated_shield");
    private static final TagKey<Block> HEAT = TagKey.create(Registries.BLOCK, id("warmth_heat_source_blocks"));
    private record HeatSample(long tick, boolean near) { }
    private static final Map<ServerPlayer, HeatSample> HEAT_CACHE = new WeakHashMap<>();
    private CookeryEffects() { }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("kaleidoscope_cookery", path); }
    public static void register() {
        CuisineEffectAdapter.register(VIGOR, new CuisineEffectAdapter.Semantics(true, false, true, false, false));
        CuisineEffectAdapter.register(WARMTH, new CuisineEffectAdapter.Semantics(false, false, false, false, true));
        CuisineEffectAdapter.register(SHIELD, new CuisineEffectAdapter.Semantics(false, false, false, true, false));
        for (String effect : new String[]{"preservation", "projectile_dodge", "sulfur", "flatulence", "hinder", "instant_smelting", "vitality", "tundra_strider", "mustard"}) {
            CuisineEffectAdapter.register(id(effect), new CuisineEffectAdapter.Semantics(false, false, false, false, false));
        }
        CuisineEffectAdapter.registerStableSpeed(WARMTH, CookeryEffects::warmthSpeed);
    }
    public static double warmthSpeed(ServerPlayer player) {
        long tick = player.level().getGameTime();
        var sample = HEAT_CACHE.get(player);
        if (sample == null || tick - sample.tick() >= 25 || tick < sample.tick()) {
            var pos = player.blockPosition();
            boolean heat = BlockPos.betweenClosedStream(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))
                    .map(player.level()::getBlockState).anyMatch(state -> state.is(HEAT)
                            || state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT));
            sample = new HeatSample(tick, heat);
            HEAT_CACHE.put(player, sample);
        }
        return CuisineEffectBudget.warmthSpeed(sample.near(), player.level().dimension().equals(Level.NETHER));
    }
    public static float shield(ServerPlayer player, DamageSource source, float damage) {
        if (!CuisineEffectAdapter.has(player, SHIELD) || !PlayerOvereat.active(player) || PlayerOvereat.overfull(player)
                || player.getFoodData().getFoodLevel() < 18 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypeTags.BYPASSES_EFFECTS)
                || source.is(DamageTypeTags.BYPASSES_RESISTANCE)) return damage;
        float saturation = player.getFoodData().getSaturationLevel();
        float prevented = CuisineEffectBudget.shieldPrevented(damage, saturation);
        player.getFoodData().setSaturation(saturation - prevented);
        return damage - prevented;
    }
}
