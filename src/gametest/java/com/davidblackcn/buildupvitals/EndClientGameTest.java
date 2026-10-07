package com.davidblackcn.buildupvitals;
import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
public class EndClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        if(!EndGameTests.loaded())return;
        try(var world=context.worldBuilder().create()){check(context);}
        try(var server=context.worldBuilder().createServer();var connection=server.connect()){check(context);}
        BuildupVitals.LOGGER.info("End client PASS: 44 profiles, integrated/dedicated TCP, no duplicate Vigor");
    }
    private static void check(ClientGameTestContext context){
        context.waitFor(c->ClientFoodProfiles.find(Identifier.parse("kaleidoscope_end:dragon_souffle")).filter(p->p.profileId().isPresent()).isPresent());
        context.runOnClient(c->{int count=0;for(var item:BuiltInRegistries.ITEM){
            var id=BuiltInRegistries.ITEM.getKey(item);var stack=item.getDefaultInstance();if(!id.getNamespace().equals("kaleidoscope_end")||!stack.has(DataComponents.CONSUMABLE))continue;count++;
            var profile=ClientFoodProfiles.find(id).orElseThrow();var lines=stack.getTooltipLines(Item.TooltipContext.of(c.level),c.player,TooltipFlag.NORMAL);
            KaleidoscopeClientGameTest.check(profile.profileId().isPresent(),"End profile "+id);
            KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"tooltip.buildup_vitals.recovery")== (profile.recovery()>0?1:0),"One recovery tooltip "+id);
            KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"effect.minecraft.regeneration")==0,"No stale regeneration "+id);
            KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"effect.duration.infinite")==0,"No infinite duration tooltip "+id);
            if(id.getPath().equals("dragon_egg_custard"))KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"effect.minecraft.health_boost")==1&&KaleidoscopeClientGameTest.count(lines,"effect.minecraft.strength")==0,"Custard displays reviewed health boost");
            KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"effect.kaleidoscope_cookery.vigor")== (profile.benefit().isPresent()?1:0),"Single main benefit "+id);
        }KaleidoscopeClientGameTest.check(count==44,"Complete End client inventory");});
    }
}
