package com.davidblackcn.buildupvitals;
import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
public class NetherClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        if(!NetherGameTests.loaded())return;
        try(var world=context.worldBuilder().create()){check(context);}
        try(var server=context.worldBuilder().createServer();var connection=server.connect()){check(context);}
        BuildupVitals.LOGGER.info("Nether client PASS: 85 profiles, integrated/dedicated TCP, no duplicate Vigor");
    }
    private static void check(ClientGameTestContext context){
        context.waitFor(c->ClientFoodProfiles.find(Identifier.parse("kaleidoscope_nether:star_stew")).filter(p->p.profileId().isPresent()).isPresent());
        context.runOnClient(c->{int count=0;for(var item:BuiltInRegistries.ITEM){
            var id=BuiltInRegistries.ITEM.getKey(item);var stack=item.getDefaultInstance();if(!id.getNamespace().equals("kaleidoscope_nether")||!stack.has(DataComponents.CONSUMABLE))continue;count++;
            var profile=ClientFoodProfiles.find(id).orElseThrow();var lines=stack.getTooltipLines(Item.TooltipContext.of(c.level),c.player,TooltipFlag.NORMAL);
            KaleidoscopeClientGameTest.check(profile.profileId().isPresent(),"Nether profile "+id);
            KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"tooltip.buildup_vitals.recovery")== (profile.recovery()>0?1:0),"One recovery tooltip "+id);
            KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"effect.kaleidoscope_cookery.vigor")== (profile.benefit().isPresent()?1:0),"Single main benefit "+id);
        }KaleidoscopeClientGameTest.check(count==85,"Complete Nether client inventory");});
    }
}
