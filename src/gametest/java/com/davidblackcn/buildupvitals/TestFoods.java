package com.davidblackcn.buildupvitals;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.Foods;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/** Test-only foods keep fallback and unavailable-benefit fixtures independent of official profiles. */
public final class TestFoods implements ModInitializer {
    public static final Item FALLBACK = register("fallback_food", Foods.BAKED_POTATO);
    public static final Item EXPERIMENTAL = register("experimental_food", Foods.BEETROOT);
    public static final Item UNKNOWN = register("unknown_benefit_food", Foods.CARROT);

    private static Item register(String name, FoodProperties food) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("buildup_vitals_test", name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).food(food)));
    }

    @Override
    public void onInitialize() { }
}
