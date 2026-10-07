package com.davidblackcn.buildupvitals.compat.thirst;

import com.thirstwastaken2.api.ThirstApi;

/** Loaded only behind the supported-version gate. No third-party implementation is copied. */
public final class ThirstBridge {
    private ThirstBridge() { }

    public static void invalidate() { ThirstApi.clearCache(); }
    public static void drinkPortion(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack, int bite, int count) {
        int[] values = ThirstApi.thirstValues(stack); // Includes blacklist and explicit-profile priority.
        if (values != null && ThirstApi.isEnabled(player)) ThirstApi.drink(player,
                com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectBudget.portion(values[0], bite, count),
                com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectBudget.portion(values[1], bite, count));
    }
    public static boolean quenchedRecovery(net.minecraft.world.entity.player.Player player) {
        return ThirstApi.isEnabled(player) && ThirstApi.thirst(player) == ThirstApi.maxThirst() && ThirstApi.quenched(player) > 0;
    }
    public static int thirst(net.minecraft.world.entity.player.Player player) { return ThirstApi.thirst(player); }
    public static int quenched(net.minecraft.world.entity.player.Player player) { return ThirstApi.quenched(player); }
    public static void appendHydration(net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> lines) {
        if (ThirstApi.isSalt(stack)) return;
        int[] values = ThirstApi.thirstValues(stack);
        if (values == null) return;
        int quenched = ThirstApi.isWaterContainer(stack)
                ? com.thirstwastaken2.purity.WaterPurity.quenched(com.thirstwastaken2.purity.WaterPurity.quality(stack), values[1]) : values[1];
        var thirstLine = com.thirstwastaken2.tooltip.ThirstTooltip.thirst(values[0]);
        var quenchedLine = com.thirstwastaken2.tooltip.ThirstTooltip.quenched(quenched, com.thirstwastaken2.config.QuenchedOverlay.OFF);
        if (thirstLine != null) lines.add(thirstLine);
        if (quenchedLine != null) lines.add(quenchedLine);
    }
}
