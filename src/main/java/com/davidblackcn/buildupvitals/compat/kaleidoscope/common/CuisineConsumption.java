package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public final class CuisineConsumption {
    private CuisineConsumption() { }
    public static boolean allowEffect(ServerPlayer player, ItemStack stack, MobEffectInstance effect) {
        var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (KaleidoscopeVersion.supported("cookery") && item.getNamespace().equals("kaleidoscope_cookery")
                && effect.is(net.minecraft.world.effect.MobEffects.REGENERATION)) return false;
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (!CuisineEffectAdapter.isForeignCuisineEffect(id) || !ForeignMealBenefits.registered(id)) return true;
        if (!PlayerOvereat.active(player) || PlayerOvereat.overfull(player)) return false;
        var match = FoodProfileLoader.snapshot(player.level().getServer()).resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return match.fallback() || match.profile().mealBenefit().filter(id::equals).isPresent();
    }
}
