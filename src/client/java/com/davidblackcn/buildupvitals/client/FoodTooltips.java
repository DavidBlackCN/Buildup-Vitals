package com.davidblackcn.buildupvitals.client;

import com.davidblackcn.buildupvitals.food.benefit.MealBenefitRegistry;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryBalance;
import com.davidblackcn.buildupvitals.network.TooltipProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public final class FoodTooltips {
    private static final String PREFIX = "tooltip.buildup_vitals.";

    private FoodTooltips() { }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (ClientFoodProfiles.available() && com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()
                    && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("appleskin")) {
                com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge.appendHydration(stack, lines);
            }
            if ((!stack.has(DataComponents.FOOD) && !com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility.beverage(stack)) || !stack.has(DataComponents.CONSUMABLE)) return;
            var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ClientFoodProfiles.find(item).ifPresent(profile -> append(profile, flag.isAdvanced(), lines));
        });
    }

    private static void append(TooltipProfile profile, boolean advanced, List<Component> lines) {
        ChatFormatting qualityColor = switch (profile.quality()) {
            case BASIC -> ChatFormatting.GRAY;
            case PREPARED -> ChatFormatting.GREEN;
            case MEAL -> ChatFormatting.GOLD;
            case FEAST -> ChatFormatting.LIGHT_PURPLE;
        };
        lines.add(Component.translatable(PREFIX + "quality." + profile.quality().id()).withStyle(qualityColor));
        if (profile.recovery() >= RecoveryBalance.MIN_RESERVE) {
            String amount = BigDecimal.valueOf(Math.min(RecoveryBalance.MAX_RESERVE, profile.recovery()))
                    .setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            lines.add(Component.translatable(PREFIX + "recovery", icon("minecraft:hud/heart/full"), amount).withStyle(ChatFormatting.DARK_GREEN));
        }
        profile.benefit().filter(MealBenefitRegistry::available).ifPresent(id ->
                lines.add(Component.translatable(PREFIX + "benefit", icon(id.withPrefix("mob_effect/").toString()), Component.translatable("effect." + id.getNamespace() + "." + id.getPath()))
                        .withStyle(ChatFormatting.AQUA)));
        profile.consumptionSpeed().filter(speed -> speed != com.davidblackcn.buildupvitals.food.profile.ConsumptionSpeed.NORMAL)
                .ifPresent(speed -> lines.add(Component.translatable(PREFIX + "consumption." + speed.id()).withStyle(ChatFormatting.YELLOW)));
        if (!profile.categories().isEmpty()) {
            var categories = Component.empty();
            for (var category : profile.categories()) {
                if (!categories.getSiblings().isEmpty()) categories.append(Component.literal(" · "));
                categories.append(Component.translatable(PREFIX + "category." + category.id()));
            }
            lines.add(categories.withStyle(ChatFormatting.GRAY));
        }
        if (advanced) {
            lines.add(Component.translatable(PREFIX + "debug.profile", profile.profileId().map(Object::toString).orElse("fallback"))
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(PREFIX + "debug.recovery", Double.toString(profile.recovery()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(PREFIX + "debug.group", profile.group().toString()).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(PREFIX + "debug.benefit", profile.benefit().map(Object::toString).orElse("none"),
                    profile.benefit().filter(MealBenefitRegistry::available).isPresent()).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(PREFIX + "debug.base_values").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component icon(String sprite) {
        return Component.literal("*").withStyle(style -> style.withColor(ChatFormatting.WHITE).withFont(
                new net.minecraft.network.chat.FontDescription.AtlasSprite(net.minecraft.data.AtlasIds.GUI,
                        net.minecraft.resources.Identifier.parse(sprite))));
    }
}
