package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;

public class KaleidoscopeClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!CookeryGameTests.loaded()) return;
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> ClientFoodProfiles.find(Identifier.parse("kaleidoscope_cookery:pork_bone_soup")).filter(p -> p.profileId().isPresent()).isPresent());
            for (String language : List.of("zh_cn", "en_us")) {
                var future = context.computeOnClient(client -> {
                    client.options.languageCode = language; client.getLanguageManager().setSelected(language);
                    return client.reloadResourcePacks();
                });
                context.waitFor(client -> future.isDone()); future.join(); context.waitFor(client -> client.gui.overlay() == null);
                assertCookeryTooltips(context);
                context.setScreen(() -> new Screen(Component.literal("Kaleidoscope Cookery tooltip verification")) {
                    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
                        extractTransparentBackground(graphics);
                        graphics.setTooltipForNextFrame(font, CookeryGameTests.food("pork_bone_soup"), width / 2 - 80, height / 2 - 40);
                    }
                });
                context.waitTicks(3); context.takeScreenshot("cookery-" + language); context.setScreen(() -> null);
            }
        }
        try (var server = context.worldBuilder().createServer(); var connection = server.connect()) {
            context.waitFor(client -> ClientFoodProfiles.find(Identifier.parse("kaleidoscope_cookery:flower_tea")).filter(p -> p.profileId().isPresent()).isPresent());
            assertCookeryTooltips(context);
            server.runOnServer(instance -> {
                var p = connection.getServerPlayer(); p.setGameMode(GameType.SURVIVAL); p.getFoodData().setFoodLevel(0);
                CookeryGameTests.food("pork_bone_soup").finishUsingItem(p.level(), p);
                check(ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).count() == 1, "TCP server grants one main effect");
            });
            var pack = server.computeOnServer(instance -> instance.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR).resolve("buildup_cookery_override"));
            var file = pack.resolve("data/buildup_vitals/buildup_vitals/food_profiles/kaleidoscope_cookery/pork_bone_soup.json");
            try {
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Cookery override test\",\"min_format\":121,\"max_format\":121}}");
                for (String benefit : List.of("none", "buildup_vitals:restorative", "kaleidoscope_cookery:vigor")) {
                    java.nio.file.Files.writeString(file, "{\"schema_version\":1,\"selector\":{\"item\":\"kaleidoscope_cookery:pork_bone_soup\"},\"quality\":\"prepared\",\"recovery\":{\"health\":1},\"hydration\":{\"thirst\":0,\"quenched\":0},\"consumption\":{\"speed\":\"quick\"}"
                            + (benefit.equals("none") ? "" : ",\"meal_benefit\":\"" + benefit + "\"") + "}");
                    reload(context,server);
                    context.waitFor(client -> ClientFoodProfiles.find(Identifier.parse("kaleidoscope_cookery:pork_bone_soup"))
                            .map(p -> p.recovery() == 1 && p.benefit().map(Object::toString).orElse("none").equals(benefit)).orElse(false));
                    server.runOnServer(instance -> {
                        var p = connection.getServerPlayer(); p.removeAllEffects(); p.getFoodData().setFoodLevel(0);
                        CookeryGameTests.food("pork_bone_soup").finishUsingItem(p.level(),p);
                        var effects = ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).toList();
                        check(benefit.equals("none") ? effects.isEmpty() : effects.size() == 1 && BuiltInRegistries.MOB_EFFECT.getKey(effects.getFirst().value()).toString().equals(benefit), "Final Profile owns Vigor grant");
                        if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) check(com.thirstwastaken2.api.ThirstApi.thirstValues(CookeryGameTests.food("pork_bone_soup")) == null, "Explicit zero overrides TWT defaults after reload");
                    });
                    context.runOnClient(client -> check(CookeryGameTests.food("pork_bone_soup").getUseDuration(client.player) == 21, "Reloaded client use speed"));
                }
                java.nio.file.Files.delete(file); reload(context,server);
                context.waitFor(client -> ClientFoodProfiles.find(Identifier.parse("kaleidoscope_cookery:pork_bone_soup")).map(p -> p.recovery() == 3).orElse(false));
                assertCookeryTooltips(context);
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        BuildupVitals.LOGGER.info("Cookery client PASS: 120 synchronized profiles, 119 food/drink tooltips, integrated + dedicated TCP, zh/en; TWT2={} AppleSkin={}",
                com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled(), net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("appleskin"));
    }
    private static void assertCookeryTooltips(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (var entry : CookeryGameTests.inventory()) {
                var name = entry.getAsJsonObject().get("id").getAsString(); var stack = CookeryGameTests.food(name);
                var profile = ClientFoodProfiles.find(BuiltInRegistries.ITEM.getKey(stack.getItem())).orElseThrow();
                check(profile.profileId().isPresent(), "Explicit synced profile: " + name);
                if (name.equals("transmutation_lunch_bag")) continue;
                var lines = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
                check(count(lines, "tooltip.buildup_vitals.quality." + profile.quality().id()) == 1, "Single Buildup quality: " + name);
                check(count(lines, "tooltip.buildup_vitals.recovery") == (profile.recovery() > 0 ? 1 : 0), "Single recovery line: " + name);
                check(count(lines, "effect.minecraft.regeneration") == 0, "No stale cuisine regeneration tooltip: " + name);
                check(count(lines, "effect.kaleidoscope_cookery.vigor") == (profile.benefit().isPresent() ? 1 : 0), "No duplicate Vigor: " + name);
                check(lines.stream().noneMatch(line -> line.getString().contains("tooltip.buildup_vitals")), "Translations resolved: " + name);
            }
        });
    }
    static long count(List<Component> lines, String key) { return lines.stream().mapToLong(line -> count(line,key)).sum(); }
    static long count(Component line, String key) {
        long result = 0;
        if (line.getContents() instanceof TranslatableContents text) {
            if (text.getKey().equals(key)) result++;
            for (var arg : text.getArgs()) if (arg instanceof Component c) result += count(c,key);
        }
        for (var sibling : line.getSiblings()) result += count(sibling,key);
        return result;
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void reload(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        var future = server.computeOnServer(instance -> {
            var packs = instance.getPackRepository(); packs.reload(); var selected = new java.util.ArrayList<>(packs.getSelectedIds());
            if (!selected.contains("file/buildup_cookery_override")) selected.add("file/buildup_cookery_override");
            return instance.reloadResources(selected);
        });
        context.waitFor(client -> future.isDone()); future.join();
    }
}
