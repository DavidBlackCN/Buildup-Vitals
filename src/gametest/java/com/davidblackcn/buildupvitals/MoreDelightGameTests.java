package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.GameType;

public class MoreDelightGameTests {
    static boolean loaded() { return FabricLoader.getInstance().isModLoaded("moredelight"); }
    static ItemStack food(String name) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse("moredelight:" + name)).orElseThrow().getDefaultInstance();
    }
    // Frozen from the verified release registration bytecode/source, not from Buildup profiles.
    static JsonArray inventory() {
        try (var reader = new InputStreamReader(MoreDelightGameTests.class.getResourceAsStream("/moredelight-inventory.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    @GameTest
    public void exactInventoryProfilesAndConsumption(GameTestHelper helper) {
        var snapshot = FoodProfileLoader.snapshot(helper.getLevel().getServer());
        if (!loaded()) {
            helper.assertTrue(snapshot.resolve(Identifier.parse("moredelight:carrot_soup")).fallback(), "Absent addon does not enable its pack");
            helper.succeed(); return;
        }
        var actual = new HashSet<String>();
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("moredelight") && (item.components().has(DataComponents.FOOD) || item.components().has(DataComponents.CONSUMABLE))) actual.add(id.getPath());
        }
        var expected = new HashSet<String>();
        for (var element : inventory()) {
            var row = element.getAsJsonObject();
            String name = row.get("id").getAsString(); expected.add(name);
            var stack = food(name);
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            var match = snapshot.resolve(id);
            helper.assertTrue(!match.fallback(), "Explicit profile: " + id);
            var profile = match.profile();
            helper.assertTrue(profile.recoveryHealth() == Math.rint(profile.recoveryHealth()) && profile.recoveryHealth() >= 0 && profile.recoveryHealth() <= 6, "Official HP units: " + id);
            helper.assertTrue(!profile.categories().isEmpty() && profile.varietyGroup().isPresent() && profile.consumptionSpeed().isPresent(), "Complete diet/speed data: " + id);
            var nativeFood = stack.get(DataComponents.FOOD);
            var consumable = stack.get(DataComponents.CONSUMABLE);
            helper.assertTrue(nativeFood.nutrition() == row.get("nutrition").getAsInt() && Math.abs(nativeFood.saturation() - row.get("saturation").getAsDouble()) < .0001, "Native H/S unchanged: " + id);
            helper.assertTrue(consumable.consumeTicks() == row.get("ticks").getAsInt(), "Native duration component retained: " + id);
            var effects = consumable.onConsumeEffects().stream().filter(ApplyStatusEffectsConsumeEffect.class::isInstance)
                    .map(ApplyStatusEffectsConsumeEffect.class::cast).toList();
            String effect = row.get("effect").getAsString();
            helper.assertTrue(effects.size() == (effect.isEmpty() ? 0 : 1), "Native effect count: " + id);
            if (!effect.isEmpty()) {
                var applied = effects.getFirst(); var instance = applied.effects().getFirst();
                helper.assertTrue(applied.probability() == 1 && applied.effects().size() == 1 && instance.getAmplifier() == 0
                        && BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value()).toString().equals(effect)
                        && instance.getDuration() == row.get("duration").getAsInt(), "Native effect provenance: " + id);
            }
            helper.assertTrue(profile.mealBenefit().map(Object::toString).orElse("").equals(effect.equals("farmersdelight:nourishment") ? effect : ""), "Only native Nourishment occupies main slot: " + id);
            var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(0);
            helper.assertTrue(stack.getUseDuration(p) == profile.consumptionSpeed().orElseThrow().ticks(), "Effective server use speed: " + id);
            var remainder = stack.finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(PlayerRecovery.state(p).reserve() == profile.recoveryHealth() && PlayerDiet.state(p).entries().size() == 1, "Single reserve and diet grant: " + id);
            helper.assertTrue(p.getFoodData().getFoodLevel() == row.get("nutrition").getAsInt() && p.getHealth() == 10, "Nutrition retained, no instant recovery: " + id);
            helper.assertTrue(row.get("bowl").getAsBoolean() ? remainder.is(Items.BOWL) : remainder.isEmpty(), "Native remainder: " + id);
            if (!effect.isEmpty()) {
                var holder = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effect)).orElseThrow();
                helper.assertTrue(p.getEffect(holder).getDuration() == row.get("duration").getAsInt(), "Actual duration unchanged: " + id);
            }
            if (effect.equals("farmersdelight:nourishment")) {
                helper.assertTrue(ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).count() == 1 && PlayerMealBenefits.foodInterval(p) == 10
                        && Math.abs(PlayerMealBenefits.activityExhaustion(p, 1) - .9F) < .0001, "Single icon with both semantics: " + id);
            }
        }
        helper.assertTrue(actual.equals(expected) && actual.size() == 31, "Exact registered Food/Consumable set: " + actual);
        helper.succeed();
    }

    @GameTest
    public void overfullCreativeIndependentEffectsAndHydration(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        var nourishment = ForeignMealBenefits.holder(FarmersDelightCompatibility.NOURISHMENT).orElseThrow();
        for (var element : inventory()) {
            String name = element.getAsJsonObject().get("id").getAsString();
            var p = RecoveryGameTests.player(helper);
            p.addEffect(new MobEffectInstance(nourishment, 321));
            p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 200));
            food(name).finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(PlayerRecovery.state(p).reserve() == 0 && p.getEffect(nourishment).getDuration() == 321, "Overfull blocks reserve and refresh: " + name);
            helper.assertTrue(PlayerDiet.state(p).entries().size() == 1, "Overfull preserves diet: " + name);
            if (name.endsWith("_salad")) helper.assertTrue(p.getEffect(MobEffects.REGENERATION).getDuration() == 100, "Independent regeneration retained even Overfull");
            var creative = RecoveryGameTests.player(helper, GameType.CREATIVE);
            food(name).finishUsingItem(helper.getLevel(), creative);
            helper.assertTrue(PlayerRecovery.state(creative).reserve() == 0 && !creative.hasEffect(nourishment), "Creative cannot farm reserve/meal benefits: " + name);
        }
        var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(20); PlayerOvereat.foodConsumed(p, 63);
        food("carrot_soup").finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(PlayerOvereat.overfull(p) && !p.hasEffect(nourishment) && PlayerRecovery.state(p).reserve() == 0, "Threshold-crossing soup blocked");
        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) FarmersDelightThirstTests.exercise(helper, "moredelight");
        helper.succeed();
    }

    @GameTest
    public void recoveryAndSharedVarietyGroups(GameTestHelper helper) {
        if (!loaded()) { helper.succeed(); return; }
        var p = RecoveryGameTests.player(helper); p.getFoodData().setFoodLevel(0);
        p.addEffect(new MobEffectInstance(BuildupEffects.INVIGORATED, 100));
        food("carrot_soup").finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(!p.hasEffect(BuildupEffects.INVIGORATED), "Incoming Nourishment replaces native main benefit");
        RecoveryGameTests.ticks(p, 9); p.hurtServer(helper.getLevel(), p.damageSources().generic(), 2);
        RecoveryGameTests.ticks(p, 21);
        helper.assertTrue(p.getHealth() == 11 && PlayerRecovery.state(p).reserve() == 0, "Exactly 3 HP in 30 ticks; damage does not pause");
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(BuildupEffects.RESTORATIVE) && !p.hasEffect(ForeignMealBenefits.holder(FarmersDelightCompatibility.NOURISHMENT).orElseThrow()), "Native soup replaces addon Nourishment");
        var snapshot = FoodProfileLoader.snapshot(helper.getLevel().getServer());
        for (var group : new String[][]{
                {"minecraft:bread", "moredelight:bread_slice", "moredelight:toast"},
                {"farmersdelight:hamburger", "moredelight:simple_hamburger", "moredelight:hamburger_with_egg", "moredelight:loaded_hamburger"},
                {"moredelight:cooked_rice_with_beef", "moredelight:cooked_rice_with_porkchop", "moredelight:cooked_rice_with_chicken_cuts"},
                {"moredelight:toast_with_egg", "moredelight:toast_with_honey", "moredelight:toast_with_glow_berries", "moredelight:toast_with_sweet_berries", "moredelight:toast_with_chocolate"}}) {
            var first = snapshot.resolve(Identifier.parse(group[0])).profile().varietyGroupFor(Identifier.parse(group[0]));
            for (String id : group) helper.assertTrue(snapshot.resolve(Identifier.parse(id)).profile().varietyGroupFor(Identifier.parse(id)).equals(first), "Shared variety family: " + id);
        }
        helper.succeed();
    }
}
