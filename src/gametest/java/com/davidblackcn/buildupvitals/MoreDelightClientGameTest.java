package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.player.DietAttachments;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

public class MoreDelightClientGameTest implements FabricClientGameTest {
    private static final Identifier SOUP = Identifier.parse("moredelight:carrot_soup");

    @Override public void runTest(ClientGameTestContext context) {
        if (!MoreDelightGameTests.loaded()) { return; }
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> ClientFoodProfiles.find(SOUP).isPresent());
            assertTooltips(context);
            for (String language : List.of("zh_cn", "en_us")) {
                var future = context.computeOnClient(client -> {
                    client.options.languageCode = language; client.getLanguageManager().setSelected(language);
                    return client.reloadResourcePacks();
                });
                context.waitFor(client -> future.isDone()); future.join();
                context.waitFor(client -> client.gui.overlay() == null);
                assertTooltips(context);
                context.setScreen(() -> new Screen(Component.literal("More Delight tooltip verification")) {
                    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
                        extractTransparentBackground(graphics);
                        graphics.setTooltipForNextFrame(font, MoreDelightGameTests.food("carrot_soup"), width / 2 - 80, height / 2 - 40);
                    }
                });
                context.waitTicks(3);
                context.takeScreenshot("moredelight-" + language);
                context.setScreen(() -> null);
            }
        }
        try (var server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) {
                context.waitFor(client -> ClientFoodProfiles.find(SOUP).isPresent());
                assertTooltips(context);
                var pack = server.computeOnServer(instance -> instance.getWorldPath(LevelResource.DATAPACK_DIR).resolve("buildup_md_override"));
                var profile = pack.resolve("data/buildup_vitals/buildup_vitals/food_profiles/moredelight/carrot_soup.json");
                try {
                    Files.createDirectories(profile.getParent());
                    Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"More Delight override test\",\"min_format\":121,\"max_format\":121}}");
                    for (String benefit : List.of("none", "buildup_vitals:restorative", "farmersdelight:nourishment")) {
                        Files.writeString(profile, "{\"schema_version\":1,\"selector\":{\"item\":\"moredelight:carrot_soup\"},\"quality\":\"prepared\",\"recovery\":{\"health\":1},\"hydration\":{\"thirst\":3,\"quenched\":1},\"consumption\":{\"speed\":\"quick\"}"
                                + (benefit.equals("none") ? "" : ",\"meal_benefit\":\"" + benefit + "\"") + "}");
                        reload(context, server);
                        context.waitFor(client -> ClientFoodProfiles.find(SOUP).map(p -> p.recovery() == 1 && p.benefit().map(Object::toString).orElse("none").equals(benefit)).orElse(false));
                        server.runOnServer(instance -> {
                            var p = connection.getServerPlayer(); p.setGameMode(GameType.SURVIVAL); p.setHealth(20);
                            p.getFoodData().setFoodLevel(0); p.getFoodData().setSaturation(0); p.removeAllEffects();
                            p.removeAttached(PlayerOvereat.STATE); p.removeAttached(RecoveryAttachments.RECOVERY); p.removeAttached(DietAttachments.DIET);
                            var stack = MoreDelightGameTests.food("carrot_soup");
                            check(stack.getUseDuration(p) == 21, "Server override speed");
                            stack.finishUsingItem(p.level(), p);
                            check(PlayerRecovery.state(p).reserve() == 1, "Override reserve");
                            var effects = ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).toList();
                            check(benefit.equals("none") ? effects.isEmpty() : effects.size() == 1 && BuiltInRegistries.MOB_EFFECT.getKey(effects.getFirst().value()).toString().equals(benefit), "Final profile owns addon effect");
                            if (benefit.equals("farmersdelight:nourishment")) check(p.getEffect(effects.getFirst()).getDuration() == 3600, "Original addon Nourishment duration");
                            if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) {
                                var values = com.davidblackcn.buildupvitals.hydration.HydrationAdapter.find(SOUP);
                                check(values.thirst() == 3 && values.quenched() == 1, "Reloaded TWT overlay");
                            }
                        });
                        context.runOnClient(client -> {
                            var stack = MoreDelightGameTests.food("carrot_soup");
                            check(stack.getUseDuration(client.player) == 21, "Client override speed");
                            var lines = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
                            check(count(lines, "tooltip.buildup_vitals.benefit") == (benefit.equals("none") ? 0 : 1), "Single effective benefit tooltip");
                            check(count(lines, "effect.farmersdelight.nourishment") == (benefit.equals("farmersdelight:nourishment") ? 1 : 0), "No stale native addon tooltip");
                        });
                    }
                    Files.delete(profile); reload(context, server);
                    context.waitFor(client -> ClientFoodProfiles.find(SOUP).map(p -> p.recovery() == 3).orElse(false));
                } catch (java.io.IOException e) { throw new AssertionError(e); }
                assertTooltips(context);
                server.runOnServer(instance -> check(FoodProfileLoader.snapshot(instance).resolve(SOUP).profile().recoveryHealth() == 3, "Deleted override restores builtin"));
            }
            try (var connection = server.connect()) {
                context.waitFor(client -> ClientFoodProfiles.find(SOUP).isPresent()); assertTooltips(context);
            }
        }
        BuildupVitals.LOGGER.info("More Delight client PASS: 31 profiles/tooltips, integrated + dedicated TCP, reload/removal/reconnect; TWT2={} AppleSkin={}",
                com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled(), net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("appleskin"));
    }

    private static void assertTooltips(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (var element : MoreDelightGameTests.inventory()) {
                var row = element.getAsJsonObject(); var stack = MoreDelightGameTests.food(row.get("id").getAsString());
                var profile = ClientFoodProfiles.find(BuiltInRegistries.ITEM.getKey(stack.getItem())).orElseThrow();
                check(profile.profileId().isPresent(), "Explicit synchronized addon profile");
                for (var flag : List.of(TooltipFlag.NORMAL, TooltipFlag.ADVANCED)) {
                    var lines = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, flag);
                    boolean nourishing = row.get("effect").getAsString().equals("farmersdelight:nourishment");
                    check(count(lines, "tooltip.buildup_vitals.benefit") == (nourishing ? 1 : 0), "One benefit line");
                    check(count(lines, "effect.farmersdelight.nourishment") == (nourishing ? 1 : 0), "No duplicate Nourishment");
                    check(lines.stream().noneMatch(line -> line.getString().contains("tooltip.buildup_vitals")), "Translated tooltip");
                    if (row.get("effect").getAsString().equals("minecraft:regeneration")) check(count(lines, "effect.minecraft.regeneration") == 1, "Independent regeneration tooltip retained");
                }
                check(stack.getUseDuration(client.player) == profile.consumptionSpeed().orElseThrow().ticks(), "Synced addon consumption speed");
            }
        });
    }
    private static long count(List<Component> lines, String key) { return lines.stream().mapToLong(line -> count(line, key)).sum(); }
    private static long count(Component line, String key) {
        long result = 0;
        if (line.getContents() instanceof TranslatableContents text) {
            if (text.getKey().equals(key)) result++;
            for (var arg : text.getArgs()) if (arg instanceof Component c) result += count(c, key);
        }
        for (var sibling : line.getSiblings()) result += count(sibling, key);
        return result;
    }
    private static void reload(ClientGameTestContext context, TestServerContext server) {
        var future = server.computeOnServer(instance -> {
            var packs = instance.getPackRepository(); packs.reload(); var selected = new ArrayList<>(packs.getSelectedIds());
            if (!selected.contains("file/buildup_md_override")) selected.add("file/buildup_md_override");
            return instance.reloadResources(selected);
        });
        context.waitFor(client -> future.isDone()); future.join();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
