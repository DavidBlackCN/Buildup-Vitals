package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import java.util.HashMap;
import java.util.HashSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;

/** Loaded only inside the supported TWT2 + FD branch. */
final class FarmersDelightThirstTests {
    static void exercise(GameTestHelper helper) {
        exercise(helper, "farmersdelight");
    }
    static void exercise(GameTestHelper helper, String namespace) {
        var config = ThirstConfig.get();
        var foods = config.foods;
        var blacklist = config.itemBlacklist;
        try {
            config.foods = new HashMap<>(foods);
            config.itemBlacklist = new HashSet<>(blacklist);
            for (var item : BuiltInRegistries.ITEM) {
                var id = BuiltInRegistries.ITEM.getKey(item);
                var stack = item.getDefaultInstance();
                if (!id.getNamespace().equals(namespace) || !stack.has(DataComponents.CONSUMABLE)) continue;
                var profile = FoodProfileLoader.snapshot(helper.getLevel().getServer()).resolve(id).profile();
                config.foods.put(id.toString(), new int[]{19, 19});
                ThirstApi.clearCache();
                var expected = profile.hydration();
                var values = ThirstApi.thirstValues(stack);
                helper.assertTrue(expected.thirst() == 0 && expected.quenched() == 0 ? values == null
                        : java.util.Arrays.equals(values, new int[]{expected.thirst(), expected.quenched()}), "Explicit profile wins over TWT defaults/config: " + id);
                var p = RecoveryGameTests.player(helper);
                p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 200));
                ThirstManager.set(p, new ThirstData(10, 0, 0, true));
                stack.finishUsingItem(helper.getLevel(), p);
                helper.assertTrue(ThirstApi.thirst(p) == Math.min(20, 10 + expected.thirst()) && ThirstApi.quenched(p) == expected.quenched(),
                        "Exactly one hydration grant, retained while Overfull: " + id + " got " + ThirstApi.thirst(p) + "/" + ThirstApi.quenched(p));
                config.itemBlacklist.add(id.toString());
                ThirstApi.clearCache();
                helper.assertTrue(ThirstApi.thirstValues(item.getDefaultInstance()) == null, "Blacklist still wins: " + id);
                config.itemBlacklist.remove(id.toString());
            }
        } finally {
            config.foods = foods;
            config.itemBlacklist = blacklist;
            ThirstApi.clearCache();
        }
    }
}
