package com.davidblackcn.buildupvitals;
import com.bmt.kaleidoscope_nether.init.KNEffects;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.KaleidoscopeVersion;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
public class NetherGameTests {
    static boolean loaded(){return KaleidoscopeVersion.supported("nether");}
    static ItemStack food(String name){return BuiltInRegistries.ITEM.getValue(Identifier.parse("kaleidoscope_nether:"+name)).getDefaultInstance();}
    @GameTest public void inventoryAndAllConsumption(GameTestHelper h) {
        if(!loaded()){h.succeed();return;}
        var rows=new com.google.gson.JsonArray();int count=0;
        for(var item:BuiltInRegistries.ITEM){
            var id=BuiltInRegistries.ITEM.getKey(item);var stack=item.getDefaultInstance();
            if(!id.getNamespace().equals("kaleidoscope_nether") || !stack.has(DataComponents.CONSUMABLE))continue;
            count++;var row=new com.google.gson.JsonObject();row.addProperty("id",id.getPath());row.addProperty("class",item.getClass().getSimpleName());row.addProperty("nutrition",stack.get(DataComponents.FOOD).nutrition());
            row.addProperty("bites",item instanceof BlockItem b && b.getBlock() instanceof com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock block?block.getMaxBites():0);rows.add(row);
            var match=FoodProfileLoader.snapshot(h.getLevel().getServer()).resolve(id);h.assertTrue(!match.fallback(),"Explicit profile "+id);
            var p=RecoveryGameTests.player(h);p.getFoodData().setFoodLevel(0);double hp=match.profile().recoveryHealth();
            h.assertTrue(hp==Math.rint(hp)&&hp>=0&&hp<=6,"Official integer HP "+id);
            stack.finishUsingItem(h.getLevel(),p);h.assertTrue(PlayerRecovery.state(p).reserve()==hp&&PlayerDiet.state(p).entries().size()==1,"One recovery/diet "+id+" actual="+PlayerRecovery.state(p).reserve());
        }
        try(var reader=new java.io.InputStreamReader(NetherGameTests.class.getResourceAsStream("/nether-inventory.json"),java.nio.charset.StandardCharsets.UTF_8)){h.assertTrue(rows.equals(com.google.gson.JsonParser.parseReader(reader)),"Frozen release inventory");}catch(java.io.IOException e){throw new AssertionError(e);}

        h.assertTrue(count==85,"Nether release inventory "+count);
        if(com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled())FarmersDelightThirstTests.exercise(h,"kaleidoscope_nether");h.succeed();
    }
    @GameTest public void starCleansesOnceAndHasFiniteDefense(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        var p=RecoveryGameTests.player(h);p.getFoodData().setFoodLevel(0);p.addEffect(new MobEffectInstance(MobEffects.POISON,600));p.addEffect(new MobEffectInstance(KNEffects.STAR_BLESSING,600,1));
        h.assertTrue(!p.hasEffect(MobEffects.POISON),"Acquisition cleanse");
        h.assertTrue(p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600)),"No persistent harmful immunity");
        for(int i=0;i<60;i++)KNEffects.STAR_BLESSING.value().applyEffectTick(h.getLevel(),p,1);
        h.assertTrue(p.getHealth()==10,"No periodic heal");
        h.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==.5,"Exactly 50% independent of native amplifier II");
        p.hurtServer(h.getLevel(),p.damageSources().mobAttack(new net.minecraft.world.entity.monster.zombie.Zombie(EntityTypes.ZOMBIE,h.getLevel())),4);
        h.assertTrue(Math.abs(p.getHealth()-6.8)<.0001,"20% damage reduction");
        p.setDeltaMovement(Vec3.ZERO);p.knockback(.4,1,0,p.damageSources().generic(),1,false);
        h.assertTrue(p.getDeltaMovement().lengthSqr()>0,"Not fully knockback immune");
        p.removeEffect(KNEffects.STAR_BLESSING);h.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)==0,"Modifier removed");h.succeed();
    }
    @GameTest public void crimsonPreservesVanillaArmorMath(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        for(int amplifier:new int[]{0,1,9}){
            var attacker=RecoveryGameTests.player(h);attacker.addEffect(new MobEffectInstance(KNEffects.CRIMSON,600,amplifier));
            for(boolean armor:new boolean[]{false,true}){
                var p=RecoveryGameTests.player(h);p.setHealth(20);if(armor)p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
                var source=p.damageSources().playerAttack(attacker);float expected=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(p,8,source,p.getArmorValue()*.75F,(float)p.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
                p.hurtServer(h.getLevel(),source,8);h.assertTrue(Math.abs(p.getHealth()-(20-expected))<.0001,"Vanilla armor with 25% penetration only, amplifier="+amplifier);
            }
        }h.succeed();
    }
    @GameTest public void everlastingStillOvereatsAndBlocksReserve(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        var p=RecoveryGameTests.player(h);p.getFoodData().setFoodLevel(20);var stack=food("everlasting_flame_steak");
        stack.finishUsingItem(h.getLevel(),p);h.assertTrue(stack.getCount()==1&&PlayerRecovery.state(p).reserve()==1&&p.getCooldowns().isOnCooldown(stack),"Reusable steak one HP and cooldown");
        for(int i=0;i<8;i++)stack.finishUsingItem(h.getLevel(),p);
        h.assertTrue(PlayerOvereat.overfull(p),"Repeated steak enters Overfull");double before=PlayerRecovery.state(p).reserve();stack.finishUsingItem(h.getLevel(),p);h.assertTrue(PlayerRecovery.state(p).reserve()==before,"No repeat recovery while Overfull");h.succeed();
    }
    @GameTest public void allPlacedDishPortions(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        for(var item:BuiltInRegistries.ITEM){
            var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals("kaleidoscope_nether") || !(item instanceof BlockItem bi) || !(bi.getBlock() instanceof com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock block))continue;
            var p=RecoveryGameTests.player(h);int count=block.getMaxBites();double hp=FoodProfileLoader.snapshot(h.getLevel().getServer()).resolve(id).profile().recoveryHealth();var pos=h.absolutePos(new BlockPos(1,2,1));
            for(int bite=0;bite<count;bite++){p.getFoodData().setFoodLevel(0);var state=block.defaultBlockState().setValue(block.getBites(),bite);h.getLevel().setBlock(pos,state,2);block.useWithoutItem(state,h.getLevel(),pos,p,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));h.assertTrue(Math.abs(PlayerRecovery.state(p).reserve()-hp*(bite+1)/count)<.0001&&PlayerDiet.state(p).entries().size()==bite+1,"Portion once "+id);}
        }
        if(com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled())KaleidoscopeBiteThirstTests.exercise(h,"kaleidoscope_nether");
        h.succeed();
    }
    @GameTest public void uniqueAbilitiesRemain(GameTestHelper h){
        if(!loaded()){h.succeed();return;}
        var p=RecoveryGameTests.player(h);p.horizontalCollision=true;p.setDeltaMovement(Vec3.ZERO);
        KNEffects.GHOST.value().applyEffectTick(h.getLevel(),p,1);h.assertTrue(p.getDeltaMovement().y>=.2,"Ghost wall climbing");
        p.addEffect(new MobEffectInstance(KNEffects.WARPED,200));var blaze=h.spawn(EntityTypes.BLAZE,1,2,1);
        h.assertTrue(com.bmt.kaleidoscope_nether.effect.WarpedEffect.shouldAffectMob(blaze,p),"Warped configured mob pacification");
        p.addEffect(new MobEffectInstance(KNEffects.TROPICAL_STRIDER,200));h.assertTrue(p.hasEffect(KNEffects.TROPICAL_STRIDER),"Tropical Strider coexists with Warped");h.succeed();
    }
}
