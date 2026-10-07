package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.*;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.tavern.TavernEffects;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.github.ysbbbbbb.kaleidoscopetavern.item.*;
import com.github.ysbbbbbb.kaleidoscopetavern.datamap.data.DrinkEffectData.Entry;
import com.github.ysbbbbbb.kaleidoscopetavern.util.CocktailEffectHelper;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;

public class TavernGameTests {
    static boolean loaded() { return KaleidoscopeVersion.supported("tavern"); }
    static ItemStack drink(String name) { return BuiltInRegistries.ITEM.getValue(Identifier.parse("kaleidoscope_tavern:" + name)).getDefaultInstance(); }
    @GameTest public void completeInventoryAndHydration(GameTestHelper h) {
        if (!loaded()) { h.succeed(); return; }
        var rows = new com.google.gson.JsonArray(); int count = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item); var stack = item.getDefaultInstance();
            if (!id.getNamespace().equals("kaleidoscope_tavern") || !stack.has(DataComponents.CONSUMABLE)) continue;
            count++; var row = new com.google.gson.JsonObject(); row.addProperty("id",id.getPath()); row.addProperty("class",item.getClass().getSimpleName()); rows.add(row);
            var match = FoodProfileLoader.snapshot(h.getLevel().getServer()).resolve(id);
            h.assertTrue(!match.fallback(), "Explicit profile " + id);
            var p = RecoveryGameTests.player(h); p.getFoodData().setFoodLevel(0);
            h.assertTrue(stack.getUseDuration(p) == match.profile().consumptionSpeed().orElseThrow().ticks(), "Drink speed " + id);
            stack.finishUsingItem(h.getLevel(),p);
            h.assertTrue(PlayerRecovery.state(p).reserve() == match.profile().recoveryHealth(), "One recovery grant " + id);
            if (!id.getPath().equals("empty_glassware")) h.assertTrue(PlayerDiet.state(p).entries().size() == 1, "One diet grant " + id);
            h.assertTrue(!p.hasEffect(MobEffects.REGENERATION), "No independent healing " + id);
        }
        h.assertTrue(count == 50, "Exact release consumable inventory " + count);
        try (var reader = new java.io.InputStreamReader(TavernGameTests.class.getResourceAsStream("/tavern-inventory.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            h.assertTrue(rows.equals(com.google.gson.JsonParser.parseReader(reader)), "Frozen release IDs and implementation paths");
        } catch (java.io.IOException e) { throw new AssertionError(e); }

        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) FarmersDelightThirstTests.exercise(h,"kaleidoscope_tavern");
        h.succeed();
    }
    @GameTest public void sevenBrewLevelsAndRecoveryGates(GameTestHelper h) {
        if (!loaded()) { h.succeed(); return; }
        for (String name : List.of("wine","sakura_wine")) for (int level = 0; level <= 6; level++) {
            var p = RecoveryGameTests.player(h); var stack = drink(name); BottleBlockItem.setBrewLevel(stack,level);
            var result = stack.finishUsingItem(h.getLevel(),p);
            h.assertTrue(PlayerRecovery.state(p).reserve() == CuisineEffectBudget.brewRecovery(level) && p.getHealth() == 10, "Brew reserve only " + name + "/" + level);
            h.assertTrue(!p.hasEffect(MobEffects.REGENERATION) && PlayerDiet.state(p).entries().size() == 1, "One diet, no regeneration");
            h.assertTrue(result.is(com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems.EMPTY_BOTTLE), "Bottle returned");
            h.assertTrue(level != 1 || p.hasEffect(MobEffects.NAUSEA), "Undrinkable cost remains");
            if (level >= 2) h.assertTrue(p.hasEffect(ForeignMealBenefits.holder(Identifier.parse("kaleidoscope_tavern:slightly_tipsy")).orElseThrow()), "Tipsy remains");
            p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL,200)); double before=PlayerRecovery.state(p).reserve();
            var again=drink(name); BottleBlockItem.setBrewLevel(again,6);again.finishUsingItem(h.getLevel(),p);
            h.assertTrue(PlayerRecovery.state(p).reserve()==before,"Overfull blocks wine reserve");
        }
        var creative=RecoveryGameTests.player(h,net.minecraft.world.level.GameType.CREATIVE);var stack=drink("wine");BottleBlockItem.setBrewLevel(stack,6);stack.finishUsingItem(h.getLevel(),creative);
        h.assertTrue(PlayerRecovery.state(creative).reserve()==0 && stack.getCount()==1,"Creative no survival reserve, stack retained");
        h.succeed();
    }
    @GameTest public void cocktailMergeBudgetAndPotionIsolation(GameTestHelper h) {
        if (!loaded()) { h.succeed(); return; }
        var merged=CocktailEffectHelper.mergeEffects(List.of(new Entry(MobEffects.NIGHT_VISION,200,0,1),new Entry(MobEffects.NIGHT_VISION,100,0,1),new Entry(MobEffects.STRENGTH,99999,9,1),new Entry(MobEffects.RESISTANCE,99999,9,1)));
        h.assertTrue(merged.get(0).duration()==250,"Longest plus half others");
        h.assertTrue(merged.get(1).duration()==60 && merged.get(1).amplifier()==1 && merged.get(2).duration()==300 && merged.get(2).amplifier()==0,"Combat budget");
        var stack=drink("signature_cocktail"); var effects=new ArrayList<>(merged);effects.add(new Entry(MobEffects.REGENERATION,99999,9,1));
        SignatureCocktailBlockItem.setEffects(stack,effects);var copy=stack.copy();var p=RecoveryGameTests.player(h);
        stack.finishUsingItem(h.getLevel(),p);
        h.assertTrue(PlayerRecovery.state(p).reserve()==3 && !p.hasEffect(MobEffects.REGENERATION),"Cocktail healing one bounded reserve");
        h.assertTrue(p.getEffect(MobEffects.STRENGTH).getDuration()==1200 && p.getEffect(MobEffects.RESISTANCE).getAmplifier()==0,"Applied budget");
        h.assertTrue(SignatureCocktailBlockItem.getEffects(copy).size()==4,"Component payload preserved");
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,1));p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,99999,2));
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier()==1 && p.getEffect(MobEffects.RESISTANCE).getAmplifier()==2,"Ordinary potion effects untouched");
        h.succeed();
    }
    @GameTest public void bloodyMaryRequiresRealKill(GameTestHelper h) {
        if (!loaded()) { h.succeed(); return; }
        var p=RecoveryGameTests.player(h);p.addEffect(new MobEffectInstance(ForeignMealBenefits.holder(TavernEffects.BLOODY_MARY).orElseThrow(),6000));
        var victim=h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,1,2,1);
        victim.hurtServer(h.getLevel(),p.damageSources().playerAttack(p),1);
        h.assertTrue(PlayerRecovery.state(p).reserve()==0,"Nonfatal hit grants nothing");
        victim.hurtServer(h.getLevel(),p.damageSources().playerAttack(p),100);
        h.assertTrue(victim.isDeadOrDying() && p.getHealth()==10 && PlayerRecovery.state(p).reserve()==2,"Real death, reserve instead of instant heal");
        PlayerRecovery.addReserve(p,100);h.assertTrue(PlayerRecovery.state(p).reserve()==20,"Global reserve cap");
        p.removeAttached(com.davidblackcn.buildupvitals.player.RecoveryAttachments.RECOVERY);p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL,200));var another=h.spawn(net.minecraft.world.entity.EntityTypes.CHICKEN,2,2,1);
        another.hurtServer(h.getLevel(),p.damageSources().playerAttack(p),100);
        h.assertTrue(another.isDeadOrDying() && PlayerRecovery.state(p).reserve()==0,"Overfull blocks reserve and does not cancel death");
        h.succeed();
    }
    @GameTest public void placedBottlePreservesBrewAndConsumesOnce(GameTestHelper h) {
        if (!loaded()) { h.succeed(); return; }
        var p=RecoveryGameTests.player(h);var stack=drink("wine");BottleBlockItem.setBrewLevel(stack,6);
        var block=((BlockItem)stack.getItem()).getBlock();var pos=h.absolutePos(new BlockPos(1,1,1));var state=block.defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);block.setPlacedBy(h.getLevel(),pos,state,p,stack);
        state.useItemOn(ItemStack.EMPTY,h.getLevel(),p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        h.assertTrue(PlayerRecovery.state(p).reserve()==0 && PlayerDiet.state(p).entries().isEmpty(),"Taking block is not drinking");
        ItemStack taken=ItemStack.EMPTY;for(var candidate:p.getInventory().getNonEquipmentItems())if(candidate.is(stack.getItem()))taken=candidate;
        h.assertTrue(!taken.isEmpty() && BottleBlockItem.getBrewLevel(taken)==6,"Placed brew survives retrieval");
        taken.finishUsingItem(h.getLevel(),p);h.assertTrue(PlayerRecovery.state(p).reserve()==5 && PlayerDiet.state(p).entries().size()==1,"Retrieved drink exactly once");h.succeed();
    }
}
