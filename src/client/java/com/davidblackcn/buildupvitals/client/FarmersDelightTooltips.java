package com.davidblackcn.buildupvitals.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

public final class FarmersDelightTooltips {
    private FarmersDelightTooltips() { }
    public static boolean suppress(ItemStack stack, Component line) {
        if (ClientFoodProfiles.find(BuiltInRegistries.ITEM.getKey(stack.getItem())).filter(p -> p.profileId().isPresent()).isEmpty()) return false;
        if (!(line.getContents() instanceof TranslatableContents text)) return false;
        if (text.getKey().equals("effect.farmersdelight.nourishment")) return true;
        if (text.getKey().equals("potion.withDuration") || text.getKey().equals("potion.withAmplifier")) {
            for (var arg : text.getArgs()) if (arg instanceof Component component && suppress(stack, component)) return true;
        }
        return false;
    }
}
