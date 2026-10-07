package com.davidblackcn.buildupvitals;
import com.bmt.kaleidoscope_end.init.KEEffects;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.KaleidoscopeVersion;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.*;
public class EndGameTests {
    static boolean loaded(){return KaleidoscopeVersion.supported("end");}
    static ItemStack food(String name){return BuiltInRegistries.ITEM.getValue(Identifier.parse("kaleidoscope_end:"+name)).getDefaultInstance();}
    @GameTest public void allFoodAndTeaInventory(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        var rows=new com.google.gson.JsonArray();int count=0;
        for(var item:BuiltInRegistries.ITEM){
            var id=BuiltInRegistries.ITEM.getKey(item);var stack=item.getDefaultInstance();
            if(!id.getNamespace().equals("kaleidoscope_end") || !stack.has(DataComponents.CONSUMABLE))continue;
            count++;var row=new com.google.gson.JsonObject();row.addProperty("id",id.getPath());row.addProperty("class",item.getClass().getSimpleName());row.addProperty("nutrition",stack.has(DataComponents.FOOD)?stack.get(DataComponents.FOOD).nutrition():0);row.addProperty("bites",item instanceof BlockItem b && b.getBlock() instanceof com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock block?block.getMaxBites():0);rows.add(row);
            var match=FoodProfileLoader.snapshot(h.getLevel().getServer()).resolve(id);h.assertTrue(!match.fallback(),"Explicit profile "+id);
            var p=RecoveryGameTests.player(h);p.getFoodData().setFoodLevel(0);double hp=match.profile().recoveryHealth();h.assertTrue(hp==Math.rint(hp)&&hp>=0&&hp<=6,"Integer official HP "+id);
            h.assertTrue(stack.getUseDuration(p)==match.profile().consumptionSpeed().orElseThrow().ticks(),"Explicit speed "+id);
            stack.finishUsingItem(h.getLevel(),p);h.assertTrue(PlayerRecovery.state(p).reserve()==hp&&PlayerDiet.state(p).entries().size()==1,"Exactly once reserve/diet "+id);
            h.assertTrue(!p.hasEffect(MobEffects.REGENERATION),"End food regeneration removed "+id);
        }
        try(var reader=new java.io.InputStreamReader(EndGameTests.class.getResourceAsStream("/end-inventory.json"),java.nio.charset.StandardCharsets.UTF_8)){h.assertTrue(rows.equals(com.google.gson.JsonParser.parseReader(reader)),"Frozen release inventory");}catch(java.io.IOException e){throw new AssertionError(e);}

        h.assertTrue(count==44,"Complete release inventory "+count);
        if(com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled())FarmersDelightThirstTests.exercise(h,"kaleidoscope_end");h.succeed();
    }
    @GameTest public void mintAndDreamKeepIdentityWithoutImmortality(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        var p=RecoveryGameTests.player(h);p.addEffect(new MobEffectInstance(KEEffects.MINT,600));
        var enderman=new net.minecraft.world.entity.monster.Enderman(EntityTypes.ENDERMAN,h.getLevel());var event=new com.bmt.kaleidoscope_end.api.event.EnderManAngerEvent(enderman,p);com.bmt.kaleidoscope_end.init.KEEvents.STARE_ENDERMAN.invoker().onStare(event);h.assertTrue(event.isCanceled(),"Mint pacification");
        p.hurtServer(h.getLevel(),p.damageSources().generic(),4);h.assertTrue(p.getHealth()==6,"Mint no general reduction");
        var dream=RecoveryGameTests.player(h);dream.addEffect(new MobEffectInstance(KEEffects.DREAM,600));dream.hurtServer(h.getLevel(),dream.damageSources().fall(),4);h.assertTrue(dream.getHealth()==10,"Dream fall identity");dream.hurtServer(h.getLevel(),dream.damageSources().generic(),4);h.assertTrue(dream.getHealth()==6,"Dream ordinary damage still applies");
        var erosion=RecoveryGameTests.player(h);erosion.addEffect(new MobEffectInstance(KEEffects.VOID_EROSION,600));erosion.hurtServer(h.getLevel(),erosion.damageSources().generic(),100);h.assertTrue(erosion.isDeadOrDying(),"Void no fatal-hit immortality");h.succeed();
    }
    @GameTest public void voidWeakensArmorResistanceAndProtectionByFifteenPercent(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        for(int amp:new int[]{0,1,9})for(boolean enchanted:new boolean[]{false,true}){
            var attacker=RecoveryGameTests.player(h);attacker.addEffect(new MobEffectInstance(KEEffects.VOID_EROSION,600,amp));var p=RecoveryGameTests.player(h);p.setHealth(20);
            var chest=new ItemStack(Items.DIAMOND_CHESTPLATE);if(enchanted)chest.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),4);p.setItemSlot(EquipmentSlot.CHEST,chest);p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,600));
            var source=p.damageSources().playerAttack(attacker);float afterArmor=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p,8,source,p.getArmorValue()*.85F,(float)p.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            float expected=afterArmor*(1-.2F*.85F);if(enchanted)expected*=1-4*.85F/25;
            p.hurtServer(h.getLevel(),source,8);h.assertTrue(Math.abs(p.getHealth()-(20-expected))<.0001,"15% weakness, no bypass or amplifier growth actual="+p.getHealth()+" expected="+(20-expected));
        }h.succeed();
    }
    @GameTest public void mythicFoodBudgetsAndOverfull(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        for(String name:List.of("dragon_souffle","fried_dragon_egg","dark_dragon_steak","dragon_egg_custard","dragon_egg_ice_cream","dark_dragon_egg_stew","dragon_head_with_sauce")){
            var p=RecoveryGameTests.player(h);p.getFoodData().setFoodLevel(0);food(name).finishUsingItem(h.getLevel(),p);
            int hp=name.equals("dark_dragon_steak")||name.equals("dragon_egg_ice_cream")?5:6;
            h.assertTrue(PlayerRecovery.state(p).reserve()==hp&&!p.hasEffect(MobEffects.REGENERATION),"Mythic recovery "+name);
            if(name.equals("dragon_egg_custard")){h.assertTrue(p.hasEffect(MobEffects.HEALTH_BOOST)&&p.getEffect(MobEffects.HEALTH_BOOST).getAmplifier()==1&&p.getEffect(MobEffects.HEALTH_BOOST).getDuration()==6000&&!p.hasEffect(MobEffects.STRENGTH)&&!p.hasEffect(KEEffects.VOID_EROSION),"Custard health boost identity");}
            else{
                h.assertTrue(p.getEffect(MobEffects.STRENGTH).getAmplifier()==(name.equals("dragon_souffle")?1:0)&&p.getEffect(MobEffects.STRENGTH).getDuration()==(name.equals("dragon_souffle")?1200:3600),"Strength cap "+name);
                if(name.equals("dragon_souffle")||name.equals("fried_dragon_egg"))h.assertTrue(p.getEffect(MobEffects.RESISTANCE).getAmplifier()==0&&p.getEffect(MobEffects.RESISTANCE).getDuration()==2400,"Resistance I 120s");
                else h.assertTrue(p.getEffect(KEEffects.VOID_EROSION).getDuration()==900,"Limited Void identity");
            }
            h.assertTrue(p.getActiveEffects().size()<=2&&p.getActiveEffects().stream().noneMatch(MobEffectInstance::isInfiniteDuration),"At most two finite major effects "+name);
            p.addEffect(new MobEffectInstance(BuildupEffects.OVERFULL,200));double before=PlayerRecovery.state(p).reserve();food(name).finishUsingItem(h.getLevel(),p);h.assertTrue(PlayerRecovery.state(p).reserve()==before,"Overfull blocks mythic recovery");
        }h.succeed();
    }
    @GameTest public void allBlockPortionsAndSecondaryBudgets(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        int dishes=0;
        for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals("kaleidoscope_end")||!(item instanceof BlockItem bi)||!(bi.getBlock() instanceof com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock block))continue;dishes++;
            var p=RecoveryGameTests.player(h);int count=block.getMaxBites();double hp=FoodProfileLoader.snapshot(h.getLevel().getServer()).resolve(id).profile().recoveryHealth();var pos=h.absolutePos(new BlockPos(1,2,1));
            for(int bite=0;bite<count;bite++){p.getFoodData().setFoodLevel(0);var state=block.defaultBlockState().setValue(block.getBites(),bite);h.getLevel().setBlock(pos,state,2);block.useWithoutItem(state,h.getLevel(),pos,p,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));h.assertTrue(Math.abs(PlayerRecovery.state(p).reserve()-hp*(bite+1)/count)<.0001&&PlayerDiet.state(p).entries().size()==bite+1,"Portion once "+id);h.assertTrue(!p.hasEffect(MobEffects.REGENERATION),"No block regeneration");}
            if(id.getPath().equals("dark_dragon_steak"))h.assertTrue(p.getEffect(KEEffects.VOID_EROSION).getDuration()==900&&p.getEffect(MobEffects.STRENGTH).getAmplifier()==0,"Secondary reviewed effect granted by block path");
        }h.assertTrue(dishes==9,"Nine actual block dishes");
        if(com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled())KaleidoscopeBiteThirstTests.exercise(h,"kaleidoscope_end");h.succeed();
    }
}
