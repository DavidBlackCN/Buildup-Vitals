package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

public class FarmersDelightGameTests {
    @GameTest
    public void registeredConsumableInventory(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) {
            helper.assertTrue(!ForeignMealBenefits.registered(FarmersDelightCompatibility.NOURISHMENT), "Absent FD has no foreign registration");
            helper.succeed(); return;
        }
        var inventory = new JsonArray();
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("farmersdelight")) continue;
            var stack = item.getDefaultInstance();
            var food = stack.get(DataComponents.FOOD);
            var consumable = stack.get(DataComponents.CONSUMABLE);
            if (food == null && consumable == null) continue;
            var match = FoodProfileLoader.snapshot(helper.getLevel().getServer()).resolve(id);
            helper.assertTrue(!match.fallback(), "Explicit profile covers " + id);
            var profile = match.profile();
            helper.assertTrue(profile.recoveryHealth() == 0 || (profile.recoveryHealth() >= 1
                    && profile.recoveryHealth() <= 6 && profile.recoveryHealth() == Math.rint(profile.recoveryHealth())), "Integer 0/1..6 HP: " + id);
            helper.assertTrue(profile.consumptionSpeed().isPresent() && profile.varietyGroup().isPresent(), "Speed and group explicit: " + id);
            var entry = new JsonObject();
            entry.addProperty("id", id.toString());
            entry.addProperty("nutrition", food == null ? 0 : food.nutrition());
            entry.addProperty("saturation", food == null ? 0 : food.saturation());
            entry.addProperty("food", food != null);
            entry.addProperty("consume_ticks", consumable == null ? 0 : consumable.consumeTicks());
            var effects = new JsonArray();
            if (consumable != null) for (var action : consumable.onConsumeEffects()) {
                if (action instanceof ApplyStatusEffectsConsumeEffect applied) for (var effect : applied.effects()) {
                    var e = new JsonObject();
                    e.addProperty("id", BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString());
                    e.addProperty("duration", effect.getDuration());
                    e.addProperty("probability", applied.probability());
                    effects.add(e);
                }
            }
            entry.add("effects", effects);
            inventory.add(entry);
        }
        BuildupVitals.LOGGER.info("FD_CONSUMABLE_INVENTORY {}", inventory);
        helper.assertTrue(inventory.size() == 80, "Exact supported release has 80 Food/Consumable items");
        helper.succeed();
    }

    static ItemStack food(String path) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse("farmersdelight:" + path)).orElseThrow().getDefaultInstance();
    }
    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> nourishment() {
        return ForeignMealBenefits.holder(FarmersDelightCompatibility.NOURISHMENT).orElseThrow();
    }

    @GameTest
    public void allProfilesConsumeOnceAndPreserveNativeDurations(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) { helper.succeed(); return; }
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            var stack = item.getDefaultInstance();
            if (!id.getNamespace().equals("farmersdelight") || !stack.has(DataComponents.CONSUMABLE)) continue;
            var profile = FoodProfileLoader.snapshot(helper.getLevel().getServer()).resolve(id).profile();
            var consumer = player(helper);
            consumer.getFoodData().setFoodLevel(0);
            int nativeDuration = stack.get(DataComponents.CONSUMABLE).onConsumeEffects().stream()
                    .filter(ApplyStatusEffectsConsumeEffect.class::isInstance).map(ApplyStatusEffectsConsumeEffect.class::cast)
                    .flatMap(action -> action.effects().stream()).filter(FarmersDelightCompatibility::nourishment)
                    .mapToInt(MobEffectInstance::getDuration).findFirst().orElse(0);
            int expectedUseTicks = profile.consumptionSpeed().orElseThrow().ticks();
            helper.assertTrue(stack.getUseDuration(consumer) == expectedUseTicks, "Server use time: " + id);
            var nativeFood = stack.get(DataComponents.FOOD);
            var remainder = stack.finishUsingItem(helper.getLevel(), consumer);
            helper.assertTrue(PlayerRecovery.state(consumer).reserve() == profile.recoveryHealth(), "Exactly one Food Recovery budget: " + id);
            helper.assertTrue(PlayerDiet.state(consumer).entries().size() == 1, "Exactly one diet entry, including beverages: " + id);
            helper.assertTrue(consumer.getFoodData().getFoodLevel() == (nativeFood == null ? 0 : Math.min(20, nativeFood.nutrition())), "FD nutrition retained: " + id);
            if (nativeDuration > 0) {
                helper.assertTrue(consumer.hasEffect(nourishment()) && consumer.getEffect(nourishment()).getDuration() == nativeDuration,
                        "Original Nourishment duration: " + id);
                helper.assertTrue(ForeignMealBenefits.mainEffects().stream().filter(consumer::hasEffect).count() == 1,
                        "Single foreign main effect: " + id);
                helper.assertTrue(PlayerMealBenefits.foodInterval(consumer) == 10 && Math.abs(PlayerMealBenefits.activityExhaustion(consumer, 1) - .9F) < .0001,
                        "Both semantics without extra icons: " + id);
                helper.assertTrue(remainder.is(Items.BOWL), "Native bowl remainder: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest
    public void directGrantsAreExclusiveAndDoNotRefundExhaustion(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) { helper.succeed(); return; }
        var p = player(helper);
        p.addEffect(new MobEffectInstance(BuildupEffects.RESTORATIVE, 200));
        p.addEffect(new MobEffectInstance(nourishment(), 6000, 4));
        helper.assertTrue(!p.hasEffect(BuildupEffects.RESTORATIVE) && p.hasEffect(nourishment()), "Incoming foreign wins immediately");
        helper.assertTrue(PlayerMealBenefits.foodInterval(p) == 10 && Math.abs(PlayerMealBenefits.activityExhaustion(p, 1) - .9F) < .0001,
                "High amplifier does not multiply semantic bonuses");
        p.setHealth(20);
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(10);
        p.causeFoodExhaustion(3);
        nourishment().value().applyEffectTick(helper.getLevel(), p, 4);
        helper.assertTrue(exhaustion(p) == 3 && p.getFoodData().getSaturationLevel() == 10, "No refund when healthy");
        p.getFoodData().setFoodLevel(8);
        p.setHealth(10);
        nourishment().value().applyEffectTick(helper.getLevel(), p, 4);
        helper.assertTrue(p.getHealth() == 10 && exhaustion(p) == 3, "No low-hunger healing or refund");
        p.causeFoodExhaustion(1);
        helper.assertTrue(exhaustion(p) == 4, "General exhaustion remains undiscounted");
        p.addEffect(new MobEffectInstance(BuildupEffects.INVIGORATED, 100));
        helper.assertTrue(!p.hasEffect(nourishment()) && p.hasEffect(BuildupEffects.INVIGORATED), "Native wins in reverse order");
        p.addEffect(new MobEffectInstance(nourishment(), 6000));
        p.addEffect(new MobEffectInstance(BuildupEffects.STEADY, 100));
        helper.assertTrue(!p.hasEffect(nourishment()) && p.hasEffect(BuildupEffects.STEADY), "Reserved Steady still occupies main group when commanded");
        helper.succeed();
    }

    private static float exhaustion(net.minecraft.server.level.ServerPlayer player) {
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess());
        player.getFoodData().addAdditionalSaveData(output);
        return output.buildResult().getFloat("foodExhaustionLevel").orElseThrow();
    }

    @GameTest
    public void overfullBlocksRefreshAndReserveButRetainsNutritionAndDiet(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) { helper.succeed(); return; }
        var p = player(helper);
        p.addEffect(new MobEffectInstance(nourishment(), 321));
        p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL, 200));
        food("chicken_soup").finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.getEffect(nourishment()).getDuration() == 321 && PlayerRecovery.state(p).reserve() == 0, "Overfull blocks native refresh and food reserve");
        helper.assertTrue(p.getFoodData().getFoodLevel() == 20 && PlayerDiet.state(p).entries().size() == 1, "Overfull preserves nutrition and diet");
        helper.assertTrue(!p.addEffect(new MobEffectInstance(nourishment(), 6000)) && p.getEffect(nourishment()).getDuration() == 321,
                "Direct grants cannot bypass overfull refresh cap");
        p.removeEffect(nourishment());
        helper.assertTrue(!p.addEffect(new MobEffectInstance(nourishment(), 6000)), "Overfull blocks first grant too");
        p.removeAllEffects();
        p.getFoodData().setFoodLevel(20);
        PlayerOvereat.foodConsumed(p, 63);
        food("chicken_soup").finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(PlayerOvereat.overfull(p) && !p.hasEffect(nourishment()) && PlayerRecovery.state(p).reserve() == 0,
                "Threshold-crossing food is blocked before FD's later consume effect");
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100));
        helper.assertTrue(p.hasEffect(MobEffects.REGENERATION), "Independent effects remain eligible while overfull");
        helper.succeed();
    }

    @GameTest
    public void recoveryDamageNativeSoupAndBeverageEffects(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) { helper.succeed(); return; }
        var p = player(helper);
        p.getFoodData().setFoodLevel(0);
        food("beef_stew").finishUsingItem(helper.getLevel(), p);
        ticks(p, 9);
        helper.assertTrue(p.getHealth() == 10, "No instant healing");
        p.hurtServer(helper.getLevel(), p.damageSources().generic(), 2);
        ticks(p, 1);
        helper.assertTrue(p.getHealth() == 9, "Damage does not pause 10 tick foreign food recovery");
        ticks(p, 20);
        helper.assertTrue(p.getHealth() == 11 && PlayerRecovery.state(p).reserve() == 0, "Exactly 3 HP, no standalone FD healing");
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(BuildupEffects.RESTORATIVE) && !p.hasEffect(nourishment()), "Vanilla explicit profile overrides FD's extra soup effect");
        var drinker = player(helper);
        var remainder = food("melon_juice").finishUsingItem(helper.getLevel(), drinker);
        helper.assertTrue(drinker.getHealth() == 12 && PlayerRecovery.state(drinker).reserve() == 0 && remainder.is(Items.GLASS_BOTTLE),
                "Independent melon juice healing and bottle retained, no additional reserve");
        helper.succeed();
    }

    @GameTest
    public void foreignSaveLoadMilkDeathAndHydration(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) { helper.succeed(); return; }
        var p = player(helper);
        p.addEffect(new MobEffectInstance(nourishment(), 5432));
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, p.registryAccess());
        p.saveWithoutId(output);
        var loaded = player(helper);
        loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, loaded.registryAccess(), output.buildResult()));
        helper.assertTrue(loaded.getEffect(nourishment()).getDuration() == 5432 && PlayerMealBenefits.foodInterval(loaded) == 10, "Native NBT retains exact foreign duration and semantics");
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(helper.getLevel(), loaded);
        helper.assertTrue(!loaded.hasEffect(nourishment()) && PlayerMealBenefits.foodInterval(loaded) == 12, "Milk removal leaves no mirror state");
        p.hurtServer(helper.getLevel(), p.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(!p.hasEffect(nourishment()), "Death clears foreign main benefit");
        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) FarmersDelightThirstTests.exercise(helper);
        helper.succeed();
    }

    @GameTest
    public void varietyQuenchedAndForeignBenefitShareOneHealingClock(GameTestHelper helper) {
        if (!FarmersDelightCompatibility.supported()) { helper.succeed(); return; }
        for (boolean varied : new boolean[]{false, true}) for (boolean quenched : new boolean[]{false, true}) {
            if (quenched && !com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) continue;
            var p = player(helper);
            p.setHealth(20);
            if (varied) DietGameTests.mixedMeals(p);
            p.removeAttached(com.davidblackcn.buildupvitals.player.RecoveryAttachments.RECOVERY);
            p.removeAttached(PlayerOvereat.STATE);
            p.removeAllEffects();
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(20);
            food("roast_chicken").finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(p.getEffect(nourishment()).getDuration() == 6000, "Variety does not multiply native FD duration");
            if (varied) helper.assertTrue(PlayerDiet.state(p).variety().foodMultiplier() > 1, "High variety fixture is active");
            if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) ThirstTestSupport.prepareBalanceFixture(p, quenched);
            p.setHealth(10);
            int elapsed = 0;
            while (p.getHealth() < 20 && elapsed < 200) {
                float before = p.getHealth();
                ticks(p, 1);
                if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) ThirstTestSupport.tickBalanceFixture(p);
                helper.assertTrue(p.getHealth() - before <= 1, "No parallel healing pulse");
                elapsed++;
            }
            helper.assertTrue(elapsed == 100 && p.getHealth() == 20, "Foreign + high saturation + Variety/Quenched retain 10 tick peak");
            BuildupVitals.LOGGER.info("FD recovery matrix variety={} quenched={} half-health ticks={}", varied, quenched, elapsed);
        }
        var p = player(helper);
        p.addEffect(new MobEffectInstance(nourishment(), 1));
        ticks(p, 1);
        helper.assertTrue(!p.hasEffect(nourishment()) && PlayerMealBenefits.foodInterval(p) == 12, "Natural expiry clears foreign semantics");
        helper.succeed();
    }
}
