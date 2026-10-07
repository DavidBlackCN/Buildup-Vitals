package com.davidblackcn.buildupvitals.client;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.KaleidoscopeVersion;
import java.util.Optional;
import net.minecraft.locale.Language;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class KaleidoscopeTooltips {
    private KaleidoscopeTooltips() { }
    public static boolean hasProfile(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return KaleidoscopeVersion.TESTED.keySet().stream().anyMatch(module -> id.getNamespace().equals("kaleidoscope_" + module))
                && !id.getPath().equals("transmutation_lunch_bag") && !id.getPath().equals("empty_glassware")
                && ClientFoodProfiles.find(id).filter(profile -> profile.profileId().isPresent()).isPresent();
    }
    public static boolean suppress(ItemStack stack, Component line) {
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        boolean cookery = namespace.equals("kaleidoscope_cookery") && KaleidoscopeVersion.supported("cookery");
        boolean end = namespace.equals("kaleidoscope_end") && KaleidoscopeVersion.supported("end");
        boolean nether = namespace.equals("kaleidoscope_nether") && KaleidoscopeVersion.supported("nether");
        boolean tavern = namespace.equals("kaleidoscope_tavern") && KaleidoscopeVersion.supported("tavern");
        if (!hasProfile(stack) || (!cookery && !tavern && !nether && !end)) return false;
        if (!(line.getContents() instanceof TranslatableContents text)) return false;
        if ((cookery || nether || end) && text.getKey().equals("effect.kaleidoscope_cookery.vigor") || text.getKey().equals("effect.minecraft.regeneration") || tavern && text.getKey().equals("effect.minecraft.instant_health")) return true;
        if (text.getKey().equals("potion.withDuration") || text.getKey().equals("potion.withAmplifier")) {
            for (var arg : text.getArgs()) if (arg instanceof Component component && suppress(stack, component)) return true;
        }
        return false;
    }
    public static Optional<String> description(String translation) {
        if (!translation.startsWith("effect.kaleidoscope_")) return Optional.empty();
        String[] parts = translation.split("\\.", 3);
        if (parts.length != 3 || !CuisineEffectAdapter.isForeignCuisineEffect(Identifier.fromNamespaceAndPath(parts[1], parts[2]))) return Optional.empty();
        String key = "effect.buildup_vitals.foreign." + parts[1] + "." + parts[2];
        return Language.getInstance().has(key) ? Optional.of(key) : Optional.empty();
    }
}
