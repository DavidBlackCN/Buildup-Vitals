package com.davidblackcn.buildupvitals;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.KaleidoscopeVersion;
import com.davidblackcn.buildupvitals.food.benefit.*;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.recovery.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
public class KaleidoscopeJointGameTests {
    @GameTest public void foreignMainBenefitsRemainExclusive(GameTestHelper h){
        if(!CookeryGameTests.loaded()){h.succeed();return;}
        var p=RecoveryGameTests.player(h);p.getFoodData().setFoodLevel(0);
        p.addEffect(new MobEffectInstance(BuildupEffects.RESTORATIVE,600));CookeryGameTests.food("pork_bone_soup").finishUsingItem(h.getLevel(),p);
        var vigor=ForeignMealBenefits.holder(Identifier.parse("kaleidoscope_cookery:vigor")).orElseThrow();
        h.assertTrue(p.hasEffect(vigor)&&ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).count()==1,"Cookery owns exactly one main slot");
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("farmersdelight")){
            p.getFoodData().setFoodLevel(0);FarmersDelightGameTests.food("chicken_soup").finishUsingItem(h.getLevel(),p);
            var nourishment=ForeignMealBenefits.holder(Identifier.parse("farmersdelight:nourishment")).orElseThrow();
            h.assertTrue(p.hasEffect(nourishment)&&!p.hasEffect(vigor)&&!p.hasEffect(BuildupEffects.RESTORATIVE)&&!p.hasEffect(BuildupEffects.INVIGORATED),"Nourishment retains only its own icon");
            h.assertTrue(PlayerMealBenefits.foodInterval(p)==10&&Math.abs(PlayerMealBenefits.activityExhaustion(p,1)-.9)<.0001,"Foreign semantics preserved with cuisine layer");
            p.getFoodData().setFoodLevel(0);CookeryGameTests.food("pork_bone_soup").finishUsingItem(h.getLevel(),p);h.assertTrue(!p.hasEffect(nourishment)&&p.hasEffect(vigor),"Vigor replaces Nourishment");
            p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL,200));double reserve=PlayerRecovery.state(p).reserve();FarmersDelightGameTests.food("chicken_soup").finishUsingItem(h.getLevel(),p);h.assertTrue(!p.hasEffect(nourishment)&&PlayerRecovery.state(p).reserve()==reserve,"Overfull blocks foreign refresh and recovery");
        }h.succeed();
    }
    @GameTest public void combinedPenetrationRemainsFinite(GameTestHelper h){
        if(!KaleidoscopeVersion.supported("nether")||!KaleidoscopeVersion.supported("end")){h.succeed();return;}
        var attacker=RecoveryGameTests.player(h);
        attacker.addEffect(new MobEffectInstance(com.bmt.kaleidoscope_nether.init.KNEffects.CRIMSON,600,9));attacker.addEffect(new MobEffectInstance(com.bmt.kaleidoscope_end.init.KEEffects.VOID_EROSION,600,9));
        var p=RecoveryGameTests.player(h);p.setHealth(20);p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,600));p.addEffect(new MobEffectInstance(com.bmt.kaleidoscope_nether.init.KNEffects.STAR_BLESSING,600));
        var source=p.damageSources().playerAttack(attacker);float armor=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p,8,source,p.getArmorValue()*.75F*.85F,(float)p.getAttributeValue(Attributes.ARMOR_TOUGHNESS));float expected=armor*.83F*.8F;
        p.hurtServer(h.getLevel(),source,8);h.assertTrue(Math.abs(p.getHealth()-(20-expected))<.0001,"Crimson + Void + Resistance + Star preserve bounded vanilla math");h.succeed();
    }
    @GameTest public void allPacksAreIndependentAndComplete(GameTestHelper h){
        var snapshot=com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader.snapshot(h.getLevel().getServer());
        for(var entry:java.util.Map.of("cookery",120,"tavern",50,"nether",85,"end",44).entrySet()){
            if(!KaleidoscopeVersion.supported(entry.getKey()))continue;
            int count=0;for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals("kaleidoscope_"+entry.getKey()))continue;
                var match=snapshot.resolve(id);if(!match.fallback()){count++;double hp=match.profile().recoveryHealth();h.assertTrue(hp==0||hp>=1&&hp<=6&&hp==Math.rint(hp),"Official nonzero recovery unit "+id);}
            }h.assertTrue(count==entry.getValue(),"Independent pack count "+entry.getKey()+" = "+count);
        }h.succeed();
    }
}
