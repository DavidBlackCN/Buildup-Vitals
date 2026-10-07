package com.davidblackcn.buildupvitals.compat.kaleidoscope.end;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;

public final class EndEffects {
    public static final Set<String> MYTHIC=Set.of("dragon_souffle","fried_dragon_egg","dark_dragon_steak","dragon_egg_custard","dragon_egg_ice_cream","dark_dragon_egg_stew","dragon_head_with_sauce");
    private EndEffects() { }
    public static void register(){
        for(String name:new String[]{"mint","dream","void_erosion"}) CuisineEffectAdapter.register(Identifier.parse("kaleidoscope_end:"+name),new CuisineEffectAdapter.Semantics(false,false,false,false,false));
        CuisineEffectAdapter.registerReview("kaleidoscope_end",EndEffects::review);
    }
    public static MobEffectInstance review(ItemStack stack,MobEffectInstance effect){
        var item=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!item.getNamespace().equals("kaleidoscope_end"))return effect;
        if(effect.is(MobEffects.REGENERATION)||effect.is(MobEffects.INSTANT_HEALTH))return null;
        String name=item.getPath();
        if(!MYTHIC.contains(name))return effect;
        String id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
        // Explicit major-effect slots per dish; component edits cannot add a third major combat effect.
        if(effect.is(MobEffects.STRENGTH)) {
            if(name.equals("dragon_egg_custard"))return new MobEffectInstance(MobEffects.HEALTH_BOOST,300*20,1);
            return new MobEffectInstance(MobEffects.STRENGTH,(name.equals("dragon_souffle")?60:180)*20,name.equals("dragon_souffle")?1:0);
        }
        if(effect.is(MobEffects.RESISTANCE))return name.equals("dragon_souffle")||name.equals("fried_dragon_egg")?new MobEffectInstance(MobEffects.RESISTANCE,120*20,0):null;
        if(effect.is(MobEffects.HEALTH_BOOST))return name.equals("dragon_egg_custard")?new MobEffectInstance(MobEffects.HEALTH_BOOST,300*20,1):null;
        if(id.equals("kaleidoscope_end:void_erosion"))return name.equals("dragon_souffle")||name.equals("fried_dragon_egg")||name.equals("dragon_egg_custard")?null:new MobEffectInstance(effect.getEffect(),45*20,0);
        // No unrelated major cuisine defense/attack effects can be smuggled into a mythic dish.
        var semantics=CuisineEffectAdapter.semantics(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()));
        if(semantics!=null && (semantics.damageReduction()||id.endsWith(":crimson")))return null;
        return effect;
    }
}
