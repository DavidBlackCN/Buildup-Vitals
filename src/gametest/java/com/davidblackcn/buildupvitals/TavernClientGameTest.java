package com.davidblackcn.buildupvitals;
import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
public class TavernClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!TavernGameTests.loaded()) return;
        try(var world=context.worldBuilder().create()) { check(context); }
        try(var server=context.worldBuilder().createServer();var connection=server.connect()) { check(context); }
        BuildupVitals.LOGGER.info("Tavern client PASS: integrated and dedicated TCP, 50 profiles, brew-aware recovery tooltips");
    }
    private static void check(ClientGameTestContext context) {
        context.waitFor(client -> ClientFoodProfiles.find(Identifier.parse("kaleidoscope_tavern:wine")).filter(p -> p.profileId().isPresent()).isPresent());
        context.runOnClient(client -> {
            int count=0;
            for(var item:BuiltInRegistries.ITEM) {
                var id=BuiltInRegistries.ITEM.getKey(item);var stack=item.getDefaultInstance();
                if(!id.getNamespace().equals("kaleidoscope_tavern") || !stack.has(DataComponents.CONSUMABLE))continue;
                count++;KaleidoscopeClientGameTest.check(ClientFoodProfiles.find(id).orElseThrow().profileId().isPresent(),"Tavern synchronized profile "+id);
                if(id.getPath().equals("empty_glassware"))continue;
                for(int brew=0;brew<=6;brew++) {
                    BottleBlockItem.setBrewLevel(stack,brew);
                    var lines=stack.getTooltipLines(Item.TooltipContext.of(client.level),client.player,TooltipFlag.NORMAL);
                    double recovery=ClientFoodProfiles.find(id).orElseThrow().recovery()+CuisineEffectAdapter.recovery(stack);
                    KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"tooltip.buildup_vitals.recovery")== (recovery>0?1:0),"Brew-aware recovery line "+id+"/"+brew);
                    KaleidoscopeClientGameTest.check(KaleidoscopeClientGameTest.count(lines,"effect.minecraft.regeneration")==0,"No stale regeneration "+id);
                }
            }
            KaleidoscopeClientGameTest.check(count==50,"All Tavern profiles synced");
        });
    }
}
