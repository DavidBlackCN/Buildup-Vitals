package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.*;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.CookeryEffects;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.benefit.*;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CookeryGameTests {
    static boolean loaded() { return KaleidoscopeVersion.supported("cookery"); }
    static ItemStack food(String name) { return BuiltInRegistries.ITEM.getValue(Identifier.parse("kaleidoscope_cookery:" + name)).getDefaultInstance(); }
    static JsonArray inventory() {
        try (var reader = new InputStreamReader(CookeryGameTests.class.getResourceAsStream("/cookery-inventory.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }
    @GameTest
    public void inventoryProfilesAndConsumption(GameTestHelper helper) {
        var snapshot = FoodProfileLoader.snapshot(helper.getLevel().getServer());
        if (!loaded()) {
            helper.assertTrue(snapshot.resolve(Identifier.parse("kaleidoscope_cookery:tomato")).fallback(), "Absent Cookery: no pack");
            helper.succeed(); return;
        }
        var actual = new HashSet<String>();
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("kaleidoscope_cookery")) continue;
            if (item.components().has(DataComponents.FOOD) || item.components().has(DataComponents.CONSUMABLE)
                    || item instanceof BlockItem block && block.getBlock() instanceof com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock) actual.add(id.getPath());
        }
        var expected = new HashSet<String>();
        for (var entry : inventory()) {
            var row = entry.getAsJsonObject(); String name = row.get("id").getAsString(); expected.add(name);
            var stack = food(name); var match = snapshot.resolve(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            helper.assertTrue(!match.fallback(), "Explicit profile: " + name);
            var profile = match.profile();
            helper.assertTrue(profile.recoveryHealth() >= 0 && profile.recoveryHealth() <= 6 && profile.recoveryHealth() == Math.rint(profile.recoveryHealth()), "Official HP budget: " + name);
            helper.assertTrue(profile.varietyGroup().isPresent() && profile.consumptionSpeed().isPresent(), "Complete grouping/speed: " + name);
            if (name.equals("cold_cut_ham_slices") || name.equals("transmutation_lunch_bag")) continue;
            var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(0);
            var nativeFood = stack.get(DataComponents.FOOD);
            helper.assertTrue((nativeFood == null ? 0 : nativeFood.nutrition()) == row.get("nutrition").getAsInt(), "Native nutrition: " + name);
            helper.assertTrue(stack.getUseDuration(p) == profile.consumptionSpeed().orElseThrow().ticks(), "Effective speed: " + name);
            stack.finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(Math.abs(PlayerRecovery.state(p).reserve() - profile.recoveryHealth()) < .0001 && PlayerDiet.state(p).entries().size() == 1, "Exactly one reserve/diet: " + name + " reserve=" + PlayerRecovery.state(p).reserve());
            helper.assertTrue(!p.hasEffect(MobEffects.REGENERATION), "Cuisine regeneration becomes food budget: " + name);
            if (profile.mealBenefit().isPresent()) {
                helper.assertTrue(p.hasEffect(ForeignMealBenefits.holder(CookeryEffects.VIGOR).orElseThrow()) && ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).count() == 1, "Native Vigor occupies main slot: " + name);
            }
        }
        helper.assertTrue(actual.equals(expected) && actual.size() == 120, "Exact release inventory: " + actual);
        helper.succeed();
    }
    @GameTest
    public void vigorExclusivityAndOverfull(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        var p = RecoveryGameTests.player(helper); var vigor = ForeignMealBenefits.holder(CookeryEffects.VIGOR).orElseThrow();
        p.addEffect(new MobEffectInstance(BuildupEffects.RESTORATIVE, 100));
        food("pork_bone_soup").finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(vigor) && !p.hasEffect(BuildupEffects.RESTORATIVE) && p.getEffect(vigor).getDuration() == 3600, "Vigor replaces native main, original duration");
        helper.assertTrue(Math.abs(PlayerMealBenefits.activityExhaustion(p, 1) - .9) < .0001 && PlayerMealBenefits.foodInterval(p) == 12, "Invigorated only");
        p.addEffect(new MobEffectInstance(BuildupEffects.STEADY, 100));
        helper.assertTrue(!p.hasEffect(vigor), "Native main replaces Vigor");
        p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 200));
        double reserve = PlayerRecovery.state(p).reserve(); food("pork_bone_soup").finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(!p.hasEffect(vigor) && PlayerRecovery.state(p).reserve() == reserve, "Overfull blocks refresh and reserve");
        helper.assertTrue(!p.addEffect(new MobEffectInstance(vigor, 8000)), "Direct Overfull grant blocked");
        var creative = RecoveryGameTests.player(helper, GameType.CREATIVE); food("pork_bone_soup").finishUsingItem(helper.getLevel(), creative);
        helper.assertTrue(!creative.hasEffect(vigor) && PlayerRecovery.state(creative).reserve() == 0, "Creative does not build survival economy");
        helper.succeed();
    }
    @GameTest
    public void shieldDamageAndSaturationBudget(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        for (float damage : new float[]{4, 10, 20, 40}) for (float saturation : new float[]{0, .5F, 2, 20}) {
            var p = RecoveryGameTests.player(helper); p.setHealth(20); p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(saturation);
            p.addEffect(new MobEffectInstance(ForeignMealBenefits.holder(CookeryEffects.SHIELD).orElseThrow(), 200));
            float prevented = CuisineEffectBudget.shieldPrevented(damage, saturation);
            p.hurtServer(helper.getLevel(), p.damageSources().mobAttack(new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, helper.getLevel())), damage);
            helper.assertTrue(Math.abs(p.getHealth() - Math.max(0, 20 - damage + prevented)) < .0001, "Actual shield damage " + damage + "/" + saturation + " got " + p.getHealth());
            helper.assertTrue(Math.abs(p.getFoodData().getSaturationLevel() - saturation + prevented) < .0001 && p.getFoodData().getFoodLevel() == 20, "Only saturation pays prevented HP");
        }
        for (boolean overfull : new boolean[]{false,true}) {
            var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(overfull ? 20 : 17); p.getFoodData().setSaturation(10);
            p.addEffect(new MobEffectInstance(ForeignMealBenefits.holder(CookeryEffects.SHIELD).orElseThrow(), 200));
            if (overfull) p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 200));
            p.hurtServer(helper.getLevel(), p.damageSources().mobAttack(new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, helper.getLevel())), 4);
            helper.assertTrue(p.getHealth() == 6 && p.getFoodData().getSaturationLevel() == 10, "Hunger/Overfull gate");
        }
        helper.succeed();
    }
    @GameTest
    public void warmthControllerAndIndependentPotion(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(18);
        var pos = helper.absolutePos(new BlockPos(1,1,1)); p.setPos(Vec3.atCenterOf(pos)); helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        p.addEffect(new MobEffectInstance(ForeignMealBenefits.holder(CookeryEffects.WARMTH).orElseThrow(), 300));
        helper.assertTrue(Math.abs(PlayerRecovery.naturalSpeed(p) - 1.25) < .0001, "Near heat: 64 tick stable");
        RecoveryGameTests.ticks(p,63); helper.assertTrue(p.getHealth() == 10, "Warmth has no independent pulse");
        RecoveryGameTests.ticks(p,1); helper.assertTrue(p.getHealth() == 11, "Stable pulse at 64");
        p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(20);
        helper.assertTrue(PlayerRecovery.naturalSpeed(p) == 1, "Warmth never boosts high saturation");
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100)); RecoveryGameTests.ticks(p,1);
        helper.assertTrue(p.getHealth() == 12, "Independent potion regeneration retained");
        helper.succeed();
    }
    @GameTest
    public void shieldAfterArmorAndBypassDamage(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        var source = helper.getLevel().damageSources().mobAttack(new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, helper.getLevel()));
        var baseline = RecoveryGameTests.player(helper); var shielded = RecoveryGameTests.player(helper);
        for (var p : java.util.List.of(baseline, shielded)) {
            p.setHealth(20); p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(10);
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
            p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200));
        }
        shielded.addEffect(new MobEffectInstance(ForeignMealBenefits.holder(CookeryEffects.SHIELD).orElseThrow(), 200));
        baseline.hurtServer(helper.getLevel(), source, 10); shielded.hurtServer(helper.getLevel(), source, 10);
        float prevented = (20 - baseline.getHealth()) * .2F;
        helper.assertTrue(Math.abs(shielded.getHealth() - baseline.getHealth() - prevented) < .0001
                && Math.abs(shielded.getFoodData().getSaturationLevel() - 10 + prevented) < .0001, "Shield pays only post-armor/post-resistance prevented HP");
        for (var damage : java.util.List.of(source, helper.getLevel().damageSources().fellOutOfWorld(), helper.getLevel().damageSources().fall())) {
            var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(10);
            p.addEffect(new MobEffectInstance(ForeignMealBenefits.holder(CookeryEffects.SHIELD).orElseThrow(), 200));
            p.hurtServer(helper.getLevel(), damage, 4);
            helper.assertTrue(damage == source ? p.getHealth() > 6 : p.getHealth() == 6 && p.getFoodData().getSaturationLevel() == 10, "Unblockable/void damage bypasses cuisine shield");
        }
        helper.succeed();
    }
    @GameTest
    public void blockPortionsAndHydration(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        for (var entry : inventory()) {
            var row = entry.getAsJsonObject(); int count = row.get("bites").getAsInt(); if (count == 0) continue;
            var stack = food(row.get("id").getAsString());
            var block = (com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock) ((BlockItem) stack.getItem()).getBlock();
            var p = RecoveryGameTests.player(helper); var profile = FoodProfileLoader.snapshot(helper.getLevel().getServer()).resolve(BuiltInRegistries.ITEM.getKey(stack.getItem())).profile();
            var pos = helper.absolutePos(new BlockPos(1,2,1));
            for (int bite = 0; bite < count; bite++) {
                p.getFoodData().setFoodLevel(0); var state = block.defaultBlockState().setValue(block.getBites(), bite);
                helper.getLevel().setBlock(pos, state, 2);
                block.useWithoutItem(state, helper.getLevel(), pos, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                helper.assertTrue(Math.abs(PlayerRecovery.state(p).reserve() - profile.recoveryHealth() * (bite + 1) / count) < .0001, "Per-bite budget: " + row.get("id") + " bite " + bite);
                helper.assertTrue(PlayerDiet.state(p).entries().size() == bite + 1, "One actual diet event per bite");
                if (profile.mealBenefit().isPresent()) helper.assertTrue(p.hasEffect(ForeignMealBenefits.holder(CookeryEffects.VIGOR).orElseThrow()), "Vigor works even when secondary in block effect list");
            }
        }
        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) CookeryThirstTests.exercise(helper);
        helper.succeed();
    }
}
