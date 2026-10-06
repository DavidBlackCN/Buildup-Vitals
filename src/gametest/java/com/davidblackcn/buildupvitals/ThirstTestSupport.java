package com.davidblackcn.buildupvitals;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import java.util.HashMap;
import java.util.HashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Optional test-only helper. Internal setters prepare fixtures; consumption uses real vanilla/TWT2 paths. */
final class ThirstTestSupport {
    private static boolean listenerRegistered;
    private static boolean cancelDrink;
    static void exerciseV2(ServerPlayer player) {
        var config = ThirstConfig.get();
        var foods = config.foods;
        var blacklist = config.itemBlacklist;
        try {
            config.foods = new HashMap<>(foods);
            config.itemBlacklist = new HashSet<>(blacklist);
            config.foods.put("minecraft:mushroom_stew", new int[]{19, 19});
            config.foods.put("minecraft:cookie", new int[]{19, 19});
            ThirstApi.clearCache();
            var profile = com.davidblackcn.buildupvitals.hydration.HydrationAdapter.find(net.minecraft.resources.Identifier.parse("minecraft:mushroom_stew"));
            check(java.util.Arrays.equals(ThirstApi.thirstValues(Items.MUSHROOM_STEW), new int[]{profile.thirst(), profile.quenched()}), "Profile overrides config");
            check(ThirstApi.thirstValues(Items.COOKIE) == null, "Explicit zero overrides config and fallback");
            config.itemBlacklist.add("minecraft:mushroom_stew"); ThirstApi.clearCache();
            check(ThirstApi.thirstValues(Items.MUSHROOM_STEW) == null, "Blacklist overrides profile");
            consume(player, ThirstApi.waterBottle(3), 0, 0, 10, 8);
            consume(player, ThirstApi.waterBottle(3), 10, 8, 20, 16);
            for (int grade = 0; grade < 3; grade++) {
                var water = ThirstApi.waterBottle(grade);
                check(ThirstApi.thirstValues(water)[0] == 6, "Non-pure water keeps upstream base");
                ThirstManager.set(player, new ThirstData(0, 0, 0, true));
                water.finishUsingItem(player.level(), player);
                check(ThirstApi.thirst(player) == 6 && ThirstApi.quenched(player) <= Math.min(6, 8 * config.quenchedPercent[grade] / 100),
                        "Purity and possible sickness still reduce quenched");
                player.removeAllEffects();
            }
            var salt = com.thirstwastaken2.purity.WaterPurity.setQuality(ThirstApi.waterBottle(3), com.thirstwastaken2.purity.WaterQuality.SALT);
            consume(player, salt, 0, 0, 0, 0); player.removeAllEffects();
            config.itemBlacklist.add("minecraft:potion"); ThirstApi.clearCache();
            check(ThirstApi.thirstValues(ThirstApi.waterBottle(3)) == null, "Pure water respects blacklist");
            player.setHealth(10); player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(6);
            ThirstManager.set(player, new ThirstData(20, 6, 0, true));
            for (int i = 0; i < 40; i++) ThirstManager.tickPlayer(player);
            check(player.getHealth() == 10, "No independent quenched healing");
            check(com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery.naturalSpeed(player) == 1.15, "Full thirst grants natural speed");
            for (int i = 0; i < 10; i++) com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery.tick(player);
            check(player.getHealth() == 10, "Fractional interval never heals early");
            com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery.tick(player);
            check(player.getHealth() == 11, "Single Buildup heal on tick 11");
            ThirstManager.set(player, new ThirstData(19, 6, 0, true));
            check(com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery.naturalSpeed(player) == 1, "Requires full thirst");
        } finally { config.foods = foods; config.itemBlacklist = blacklist; ThirstApi.clearCache(); }
    }
    static void prepareRecoveryFixture(ServerPlayer player) {
        // Leave TWT2 gameplay/config unchanged; avoid its independent full-thirst healing in
        // tests that measure only Buildup's recovery clock. Stew leaves thirst below full.
        ThirstManager.set(player, new ThirstData(10, 0, 0, true));
    }

    static void exercise(ServerPlayer player) {
        if (!listenerRegistered) {
            com.thirstwastaken2.api.ThirstEvents.DRINK.register((drinker, stack, amounts) -> {
                if (cancelDrink) amounts.cancel();
            });
            listenerRegistered = true;
        }
        var config = ThirstConfig.get();
        var foods = config.foods;
        var drinks = config.drinks;
        var blacklist = config.itemBlacklist;
        try {
            config.foods = new HashMap<>(foods);
            config.drinks = new HashMap<>(drinks);
            config.itemBlacklist = new HashSet<>(blacklist);
            config.foods.put("minecraft:mushroom_stew", new int[]{1, 1});
            ThirstApi.clearCache();
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 5, 0, 11, 4);
            config.foods.remove("minecraft:mushroom_stew");
            config.drinks.remove("minecraft:mushroom_stew");
            config.foods.remove("minecraft:apple");
            config.drinks.remove("minecraft:apple");
            ThirstApi.clearCache();
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 5, 0, 11, 4);
            consume(player, new ItemStack(Items.APPLE), 5, 0, 9, 2);
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 19, 0, 20, 9);
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 2, 0, 8, 4);
            // Stage 7 official fruits and meals, with upstream config overrides explicitly absent.
            for (var item : new net.minecraft.world.item.Item[]{Items.MELON_SLICE, Items.BEETROOT_SOUP, Items.RABBIT_STEW}) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
                config.foods.remove(id);
                config.drinks.remove(id);
                ThirstApi.clearCache();
                boolean melon = item == Items.MELON_SLICE;
                consume(player, new ItemStack(item), 5, 0, melon ? 10 : item == Items.RABBIT_STEW ? 13 : 11, melon ? 3 : item == Items.RABBIT_STEW ? 6 : 4);
            }
            cancelDrink = true;
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 5, 0, 5, 0);
            cancelDrink = false;
            config.itemBlacklist.add("minecraft:mushroom_stew");
            ThirstApi.clearCache();
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 5, 0, 5, 0);
            config.itemBlacklist.remove("minecraft:mushroom_stew");
            config.foods.put("minecraft:mushroom_stew", new int[]{0, 0});
            ThirstApi.clearCache();
            consume(player, new ItemStack(Items.MUSHROOM_STEW), 5, 0, 11, 4);

            // Pure water follows TWT2's original component/purity pipeline; Buildup has no profile for it.
            var water = ThirstApi.waterBottle(ThirstApi.maxPurity());
            check(ThirstApi.purity(water) == ThirstApi.maxPurity(), "Water grade remains Pure");
            consume(player, water, 5, 0, 15, 8);
            var salt = com.thirstwastaken2.purity.WaterPurity.setQuality(ThirstApi.waterBottle(ThirstApi.maxPurity()),
                    com.thirstwastaken2.purity.WaterQuality.SALT);
            check(ThirstApi.isSalt(salt), "Salt is not converted to fresh water");
            consume(player, salt, 5, 0, 5, 0);
            player.removeAllEffects();
        } finally {
            cancelDrink = false;
            config.foods = foods;
            config.drinks = drinks;
            config.itemBlacklist = blacklist;
            ThirstApi.clearCache();
        }
    }

    static void consume(ServerPlayer player, ItemStack stack, int startThirst, int startQuenched, int endThirst, int endQuenched) {
        ThirstManager.set(player, new ThirstData(startThirst, startQuenched, 0, true));
        player.getFoodData().setFoodLevel(10);
        stack.finishUsingItem(player.level(), player);
        check(ThirstApi.thirst(player) == endThirst && ThirstApi.quenched(player) == endQuenched,
                "Expected hydration " + endThirst + "/" + endQuenched + ", got " + ThirstApi.thirst(player) + "/" + ThirstApi.quenched(player));
    }

    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
