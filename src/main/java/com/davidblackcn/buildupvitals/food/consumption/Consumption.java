package com.davidblackcn.buildupvitals.food.consumption;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.profile.ConsumptionSpeed;
import com.davidblackcn.buildupvitals.network.TooltipProfile;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Consumption {
    private static volatile Map<Identifier, TooltipProfile> clientProfiles = Map.of();
    private Consumption() { }
    public static void receive(Map<Identifier, TooltipProfile> profiles) { clientProfiles = Map.copyOf(profiles); }
    public static int duration(ItemStack stack, LivingEntity user, int original) {
        if ((!stack.has(DataComponents.FOOD) && !com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility.beverage(stack)) || !stack.has(DataComponents.CONSUMABLE)
                || !(user instanceof Player player) || player.isSpectator()) return original;
        var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Optional<ConsumptionSpeed> speed;
        if (player instanceof ServerPlayer serverPlayer)
            speed = FoodProfileLoader.snapshot(serverPlayer.level().getServer()).resolve(item).profile().consumptionSpeed();
        else if (player.level().isClientSide())
            speed = Optional.ofNullable(clientProfiles.get(item)).flatMap(TooltipProfile::consumptionSpeed);
        else return original;
        boolean overfull = player instanceof ServerPlayer serverPlayer
                ? com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.overfull(serverPlayer)
                : !player.isCreative() && player.hasEffect(BuildupEffects.OVERFULL);
        return ConsumptionSpeed.duration(speed, original, overfull);
    }
}
