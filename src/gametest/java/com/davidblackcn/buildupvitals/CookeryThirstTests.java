package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectBudget;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class CookeryThirstTests {
    static void exercise(GameTestHelper helper) {
        FarmersDelightThirstTests.exercise(helper, "kaleidoscope_cookery");
        for (var entry : CookeryGameTests.inventory()) {
            var row = entry.getAsJsonObject(); int count = row.get("bites").getAsInt(); if (count == 0) continue;
            var stack = CookeryGameTests.food(row.get("id").getAsString());
            var block = (com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock) ((BlockItem) stack.getItem()).getBlock();
            var values = FoodProfileLoader.snapshot(helper.getLevel().getServer()).resolve(BuiltInRegistries.ITEM.getKey(stack.getItem())).profile().hydration();
            var p = RecoveryGameTests.player(helper); var pos = helper.absolutePos(new BlockPos(1,2,1));
            for (int bite = 0; bite < count; bite++) {
                ThirstManager.set(p, new ThirstData(10,0,0,true)); p.getFoodData().setFoodLevel(0);
                var state = block.defaultBlockState().setValue(block.getBites(), bite); helper.getLevel().setBlock(pos, state, 2);
                block.useWithoutItem(state, helper.getLevel(), pos, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                helper.assertTrue(ThirstApi.thirst(p) == 10 + CuisineEffectBudget.portion(values.thirst(),bite,count)
                        && ThirstApi.quenched(p) == CuisineEffectBudget.portion(values.quenched(),bite,count), "Hydration per actual bite: " + row.get("id"));
            }
        }
    }
}
