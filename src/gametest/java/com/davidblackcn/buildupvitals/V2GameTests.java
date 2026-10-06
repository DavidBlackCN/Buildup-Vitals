package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.recovery.RecoveryState;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.player;
import static com.davidblackcn.buildupvitals.RecoveryGameTests.ticks;

public class V2GameTests {
    @GameTest public void specialFoodsKeepNativeEffects(GameTestHelper helper) {
        var p = player(helper);
        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)
                && p.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)
                && PlayerRecovery.state(p).reserve() == 0, "Enchanted apple keeps native effects without extra recovery");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        var bottle = new ItemStack(Items.HONEY_BOTTLE).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.POISON) && bottle.is(Items.GLASS_BOTTLE), "Honey clears poison and returns bottle");
        var stew = new ItemStack(Items.SUSPICIOUS_STEW);
        stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new net.minecraft.world.item.component.SuspiciousStewEffects(java.util.List.of(
                new net.minecraft.world.item.component.SuspiciousStewEffects.Entry(net.minecraft.world.effect.MobEffects.BLINDNESS, 120))));
        helper.assertTrue(stew.finishUsingItem(helper.getLevel(), p).is(Items.BOWL)
                && p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Flower stew effect and bowl retained");
        new ItemStack(Items.PUFFERFISH).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.POISON) && p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER), "Pufferfish hazards retained");
        for (var item : new net.minecraft.world.item.Item[]{Items.CHICKEN, Items.ROTTEN_FLESH}) {
            p.removeAllEffects(); p.getRandom().setSeed(1234567);
            for (int i = 0; i < 100 && !p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER); i++) new ItemStack(item).finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER), "Risky food still applies native hunger: " + item);
        }
        var chorus = new ItemStack(Items.CHORUS_FRUIT);
        helper.assertTrue(chorus.get(DataComponents.CONSUMABLE).onConsumeEffects().stream().anyMatch(effect ->
                effect instanceof net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect), "Chorus retains its native teleport operation");
        chorus.finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(chorus.isEmpty(), "Chorus consumption completes");
        helper.succeed();
    }

    @GameTest public void overfullBlocksBenefitRefreshAndClearsAfterDigestion(GameTestHelper helper) {
        var p = player(helper);
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), p);
        var effect = p.getEffect(com.davidblackcn.buildupvitals.effect.BuildupEffects.RESTORATIVE);
        p.setAttached(PlayerOvereat.STATE, new com.davidblackcn.buildupvitals.food.overeating.OvereatState(32, 39, true, true));
        helper.assertTrue(new ItemStack(Items.COOKIE).getUseDuration(p) == 20,
                "Server duration respects persisted Overfull latch before effect resynchronization");
        p.getFoodData().setFoodLevel(10);
        new ItemStack(Items.PUMPKIN_PIE).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(p.getFoodData().getFoodLevel() == 18 && p.getEffect(com.davidblackcn.buildupvitals.effect.BuildupEffects.RESTORATIVE) == effect,
                "Overfull retains nutrition and blocks main benefit replacement");
        int duration = effect.getDuration();
        p.getFoodData().setFoodLevel(10);
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(effect.getDuration() == duration, "Overfull does not refresh same benefit");
        PlayerOvereat.tick(p);
        helper.assertTrue(PlayerOvereat.state(p).load() == 31 && !PlayerOvereat.overfull(p), "Effect clears strictly below 32");
        helper.succeed();
    }

    @GameTest public void thirstV2PriorityWaterAndRecovery(GameTestHelper helper) {
        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) ThirstTestSupport.exerciseV2(player(helper));
        helper.succeed();
    }
    @GameTest public void legacyBenefitMigratesOnceAndEffectsAreExclusive(GameTestHelper helper) {
        var p=player(helper);
        p.setAttached(com.davidblackcn.buildupvitals.player.MealBenefitAttachments.MEAL_BENEFIT,
                new com.davidblackcn.buildupvitals.food.benefit.MealBenefitState(java.util.Optional.of(com.davidblackcn.buildupvitals.food.benefit.MealBenefitType.RESTORATIVE.id()),1234));
        com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits.migrate(p);
        var effects=com.davidblackcn.buildupvitals.effect.BuildupEffects.mainEffects();
        var restored=p.getEffect(effects.getFirst());
        helper.assertTrue(restored!=null && restored.getDuration()==1234 && restored.showIcon() && !restored.isVisible(),"Migration preserves time and icon without particles");
        helper.assertTrue(!p.hasAttached(com.davidblackcn.buildupvitals.player.MealBenefitAttachments.MEAL_BENEFIT),"Legacy attachment consumed once");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.davidblackcn.buildupvitals.effect.BuildupEffects.STEADY,200,0,false,false,true));
        helper.assertTrue(!p.hasEffect(effects.getFirst()) && p.hasEffect(effects.getLast()),"Command-style Steady addition respects main slot");
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(p.hasEffect(effects.getFirst()) && !p.hasEffect(effects.getLast()),"Food replaces main slot");
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess());
        p.saveWithoutId(output);
        var loaded=player(helper);loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess(),output.buildResult()));
        helper.assertTrue(loaded.hasEffect(effects.getFirst()),"MobEffect persists in native player NBT");
        var replacement=player(helper);replacement.restoreFrom(loaded,true);
        helper.assertTrue(replacement.hasEffect(effects.getFirst()),"Living player replacement keeps effect");
        var dead=player(helper);dead.restoreFrom(loaded,false);
        helper.assertTrue(!dead.hasEffect(effects.getFirst()),"Death replacement clears effect");helper.succeed();
    }

    @GameTest public void consumptionDurationAndCancellation(GameTestHelper helper) {
        var p=player(helper);
        helper.assertTrue(new ItemStack(TestFoods.FALLBACK).getUseDuration(p)==32,"Unprofiled original duration retained");
        helper.assertTrue(new ItemStack(TestFoods.EXPERIMENTAL).getUseDuration(p)==21,"Quick duration");
        var fast=new ItemStack(TestFoods.UNKNOWN);
        helper.assertTrue(fast.getUseDuration(p)==16,"Fast duration");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.davidblackcn.buildupvitals.effect.BuildupEffects.OVERFULL,200,0,false,false,true));
        helper.assertTrue(fast.getUseDuration(p)==20,"Overfull fast duration");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,fast);
        p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getUseItemRemainingTicks()==20,"Server starts the modified timer");
        p.stopUsingItem();
        helper.assertTrue(PlayerOvereat.state(p).load()==0 && PlayerRecovery.state(p).reserve()==0,"Cancelled use grants nothing");
        helper.succeed();
    }

    @GameTest public void recoveryMathAndLegacyProgress(GameTestHelper helper) {
        var p=player(helper);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(3);
        ticks(p,11);helper.assertTrue(p.getHealth()==10,"No early heal");ticks(p,1);
        helper.assertTrue(p.getHealth()==10.5,"Continuous saturation heal after 12 ticks");
        var old=RecoveryAttachments.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"reserve\":3,\"progress\":119,\"mode\":\"stable\"}")).getOrThrow();
        helper.assertTrue(old.reserve()==3 && old.progress()==79,"Legacy progress safely clamped");helper.succeed();
    }
    @GameTest public void fullHungerAndOverflowUseBeforeNutrition(GameTestHelper helper) {
        var p=player(helper);p.getFoodData().setFoodLevel(12);
        new ItemStack(Items.COOKED_BEEF).finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(PlayerOvereat.state(p).load()==0,"Normal meal has no overflow");
        var steak=new ItemStack(Items.COOKED_BEEF);
        helper.assertTrue(steak.get(DataComponents.CONSUMABLE).canConsume(p,steak),"Full hunger permits food");
        steak.finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(PlayerOvereat.state(p).load()==8,"Before-eat hunger produces exactly 8 overflow");
        new ItemStack(Items.COOKIE).finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(PlayerOvereat.state(p).load()==10,"Cookie adds only two");
        var creative=player(helper,GameType.CREATIVE);creative.getFoodData().setFoodLevel(20);
        new ItemStack(Items.COOKED_BEEF).finishUsingItem(helper.getLevel(),creative);
        helper.assertTrue(PlayerOvereat.state(creative).load()==0,"Creative excluded");helper.succeed();
    }
    @GameTest public void overfullPausesOnlyFoodExtras(GameTestHelper helper) {
        var p=player(helper);p.getFoodData().setFoodLevel(20);
        p.setAttached(RecoveryAttachments.RECOVERY,new RecoveryState(3,0,RecoveryState.Mode.FOOD));
        for (int i=0;i<8;i++) new ItemStack(Items.COOKED_BEEF).finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(PlayerOvereat.overfull(p),"Eight full-hunger steaks cause Overfull");
        double reserve=PlayerRecovery.state(p).reserve();
        new ItemStack(Items.MUSHROOM_STEW).finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(PlayerRecovery.state(p).reserve()==reserve,"Overfull blocks new reserve");
        ticks(p,12);
        helper.assertTrue(p.getHealth()>=11 && PlayerRecovery.state(p).reserve()==reserve,"Natural heals while reserve paused");
        float health=p.getHealth();
        PotionContents.createItemStack(Items.POTION,Potions.HEALING).finishUsingItem(helper.getLevel(),p);
        helper.assertTrue(p.getHealth()==health+4,"Potion remains independent");helper.succeed();
    }
}
