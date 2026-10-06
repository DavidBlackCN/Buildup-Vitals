package com.davidblackcn.buildupvitals.compat.farmersdelight;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

/** No FD class references: safe to load with FD absent or a different version installed. */
public final class FarmersDelightCompatibility {
    public static final Identifier NOURISHMENT = Identifier.parse("farmersdelight:nourishment");
    public static final String SUPPORTED_VERSION = "26.3-3.6.27+refabricated";
    private FarmersDelightCompatibility() { }

    public static boolean supported() {
        return FabricLoader.getInstance().getModContainer("farmersdelight")
                .map(mod -> SUPPORTED_VERSION.equals(mod.getMetadata().getVersion().getFriendlyString())).orElse(false);
    }
    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("farmersdelight")) return;
        if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "farmers_delight"),
                FabricLoader.getInstance().getModContainer(BuildupVitals.MOD_ID).orElseThrow(),
                Component.literal("Buildup Vitals: Farmer's Delight"), PackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("Missing built-in Farmer's Delight profile pack");
        }
        if (supported()) {
            ForeignMealBenefits.register(NOURISHMENT, new ForeignMealBenefits.Semantics(true, true));
            BuildupVitals.LOGGER.info("Farmer's Delight {} semantic bridge enabled", SUPPORTED_VERSION);
        } else {
            BuildupVitals.LOGGER.warn("Unsupported Farmer's Delight version: profiles remain available, Nourishment semantic bridge disabled (tested {})", SUPPORTED_VERSION);
        }
    }
    public static boolean nourishment(MobEffectInstance effect) {
        return NOURISHMENT.equals(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()));
    }
    public static boolean allowFoodEffect(ServerPlayer player, ItemStack stack) {
        if (!PlayerOvereat.active(player) || PlayerOvereat.overfull(player)) return false;
        var match = FoodProfileLoader.snapshot(player.level().getServer()).resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        // Explicit profiles own the slot; unprofiled third-party foods retain their native grant.
        return match.fallback() || match.profile().mealBenefit().filter(NOURISHMENT::equals).isPresent();
    }
    public static boolean beverage(ItemStack stack) {
        return supported() && !stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE)
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("farmersdelight");
    }
}
