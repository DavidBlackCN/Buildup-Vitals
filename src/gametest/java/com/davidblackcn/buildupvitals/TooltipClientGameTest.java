package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.network.FoodProfilesPayload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.LevelResource;

public class TooltipClientGameTest implements FabricClientGameTest {
    private static final Identifier STEW = Identifier.parse("minecraft:mushroom_stew");
    private static final Identifier FALLBACK_ID = Identifier.parse("buildup_vitals_test:fallback_food");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            exercise(context, world.getServer(), world.getConnection());
            visuals(context);
        }
        assertCleared(context);
        try (var server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) { exercise(context, server, connection); }
            assertCleared(context);
            try (var connection = server.connect()) {
                context.waitFor(client -> ClientFoodProfiles.available());
                context.runOnClient(client -> check(ClientFoodProfiles.find(STEW).orElseThrow().recovery() == 3, "Reconnect receives fresh snapshot"));
            }
        }
        assertCleared(context);
        BuildupVitals.LOGGER.info("Stage 5 tooltip tests passed: join, reload, rollback, removal, disconnect, reconnect, translations; AppleSkin={}",
                FabricLoader.getInstance().isModLoaded("appleskin"));
    }

    private static void exercise(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
        context.waitFor(client -> ClientFoodProfiles.available());
        context.runOnClient(client -> {
            check(ClientFoodProfiles.find(STEW).orElseThrow().recovery() == 3, "Server default profile received");
            check(ClientFoodProfiles.find(FALLBACK_ID).orElseThrow().profileId().isEmpty(), "Fallback is distinct from unavailable sync");
            assertTooltips(client);
        });
        Path pack = server.computeOnServer(instance -> instance.getWorldPath(LevelResource.DATAPACK_DIR).resolve("buildup_tooltip_test"));
        Path stew = pack.resolve("data/buildup_vitals/buildup_vitals/food_profiles/mushroom_stew.json");
        Path potato = pack.resolve("data/buildup_vitals_test/buildup_vitals/food_profiles/potato.json");
        write(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Tooltip sync test\",\"min_format\":121,\"max_format\":121}}");
        write(stew, profile("minecraft:mushroom_stew", 7));
        write(potato, profile("buildup_vitals_test:fallback_food", 2));
        reload(context, server, false);
        context.waitFor(client -> ClientFoodProfiles.find(STEW).map(p -> p.recovery() == 7).orElse(false));
        context.runOnClient(client -> {
            check(ClientFoodProfiles.find(FALLBACK_ID).orElseThrow().recovery() == 2, "Reload adds previously fallback food");
            var lines = tooltip(client, Items.MUSHROOM_STEW, false);
            check(keyCount(lines, "quality.feast") == 1, "Server override changes tooltip quality");
            check(lines.stream().anyMatch(line -> line.getString().contains("7")), "Server recovery override appears in tooltip");
        });
        // Invalid higher-priority files must fall back consistently on both sides, then recover.
        for (var malformed : List.of(profile("minecraft:mushroom_stew", -1), "{\"schema_version\":1,")) {
            write(stew, malformed);
            reload(context, server, false);
            context.waitFor(client -> ClientFoodProfiles.find(STEW).map(p -> p.profileId().isEmpty()).orElse(false));
            context.runOnClient(client -> check(ClientFoodProfiles.find(FALLBACK_ID).orElseThrow().recovery() == 2,
                    "A malformed profile does not discard other valid files"));
            server.runOnServer(instance -> check(com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader.snapshot(instance)
                    .resolve(STEW).fallback(), "Server and client agree on malformed-file fallback"));
            write(stew, profile("minecraft:mushroom_stew", 7));
            reload(context, server, false);
            context.waitFor(client -> ClientFoodProfiles.find(STEW).map(p -> p.recovery() == 7).orElse(false));
        }
        write(stew, profile("minecraft:mushroom_stew", 9));
        TooltipReloadFailure.FAIL_NEXT.set(true);
        reload(context, server, true);
        context.runOnClient(client -> check(ClientFoodProfiles.find(STEW).orElseThrow().recovery() == 7, "Failed reload retains last successful snapshot"));
        try { Files.delete(stew); Files.delete(potato); } catch (IOException exception) { throw new AssertionError(exception); }
        reload(context, server, false);
        context.waitFor(client -> ClientFoodProfiles.find(STEW).map(p -> p.recovery() == 3).orElse(false));
        context.runOnClient(client -> check(ClientFoodProfiles.find(FALLBACK_ID).orElseThrow().profileId().isEmpty(), "Complete replacement removes stale item entries"));
        server.runOnServer(instance -> ServerPlayNetworking.send(connection.getServerPlayer(), new FoodProfilesPayload(false, Map.of())));
        context.waitFor(client -> !ClientFoodProfiles.available());
        context.runOnClient(client -> check(keyCount(tooltip(client, Items.MUSHROOM_STEW, false), "quality.meal") == 0, "Unavailable sync shows no invented local gameplay"));
        reload(context, server, false);
        context.waitFor(client -> ClientFoodProfiles.available());
    }

    private static void reload(ClientGameTestContext context, TestServerContext server, boolean expectFailure) {
        CompletableFuture<Void> future = server.computeOnServer(instance -> {
            var packs = instance.getPackRepository();
            packs.reload();
            var selected = new ArrayList<>(packs.getSelectedIds());
            if (!selected.contains("file/buildup_tooltip_test")) selected.add("file/buildup_tooltip_test");
            return instance.reloadResources(selected);
        });
        context.waitFor(client -> future.isDone());
        check(future.isCompletedExceptionally() == expectFailure, "Reload outcome matches fixture");
        if (!expectFailure) future.join();
    }

    private static String profile(String item, int recovery) {
        return "{\"schema_version\":1,\"selector\":{\"item\":\"" + item + "\"},\"quality\":\"feast\",\"recovery\":{\"health\":" + recovery
                + "},\"diet\":{\"categories\":[\"protein\",\"grain\",\"vegetable\"]},\"meal_benefit\":\"buildup_vitals:invigorated\"}";
    }

    private static void write(Path file, String text) {
        try { Files.createDirectories(file.getParent()); Files.writeString(file, text); }
        catch (IOException exception) { throw new AssertionError(exception); }
    }

    private static List<Component> tooltip(Minecraft client, Item item, boolean advanced) {
        return new ItemStack(item).getTooltipLines(Item.TooltipContext.of(client.level), client.player, advanced ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
    }

    private static long keyCount(List<Component> lines, String suffix) {
        return lines.stream().filter(line -> line.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("tooltip.buildup_vitals." + suffix)).count();
    }

    private static void assertTooltips(Minecraft client) {
        var normal = tooltip(client, Items.MUSHROOM_STEW, false);
        check(keyCount(normal, "quality.meal") == 1 && keyCount(normal, "recovery") == 1 && keyCount(normal, "benefit") == 1, "Normal tooltip contains one copy of each feature");
        check(keyCount(normal, "debug.profile") == 0, "Normal tooltip hides debug data");
        check(keyCount(tooltip(client, Items.COOKIE, false), "consumption.fast") == 1, "Fast snack advertised");
        check(keyCount(tooltip(client, Items.APPLE, false), "consumption.quick") == 1, "Quick food advertised");
        var advanced = tooltip(client, Items.MUSHROOM_STEW, true);
        check(keyCount(advanced, "debug.profile") == 1 && keyCount(advanced, "debug.group") == 1, "F3+H includes detailed server data");
        check(keyCount(tooltip(client, TestFoods.FALLBACK, false), "quality.basic") == 1, "Fallback food remains basic");
        check(keyCount(tooltip(client, Items.STICK, false), "quality.basic") == 0, "Non-food receives no food tooltip");
        check(keyCount(tooltip(client, TestFoods.EXPERIMENTAL, false), "benefit") == 0, "Experimental Steady is not advertised as active");
        check(keyCount(tooltip(client, Items.BAKED_POTATO, false), "quality.basic") == 1, "Official staple remains Basic");
        check(keyCount(tooltip(client, Items.RABBIT_STEW, false), "quality.meal") == 1
                && keyCount(tooltip(client, Items.RABBIT_STEW, false), "benefit") == 1, "New official meal is synchronized and displayed");
        check(normal.stream().noneMatch(line -> line.getString().contains("tooltip.buildup_vitals")), "Translation keys are resolved");
        if (FabricLoader.getInstance().isModLoaded("appleskin")) {
            check(normal.stream().filter(line -> line.getClass().getName().equals("squeek.appleskin.client.TooltipOverlayHandler$FoodOverlayTextComponent")).count() == 1,
                    "AppleSkin food overlay retained exactly once");
        }
    }

    private static void visuals(ClientGameTestContext context) {
        String suffix = FabricLoader.getInstance().isModLoaded("appleskin") ? "appleskin" : "standalone";
        for (String language : new String[]{"en_us", "zh_cn"}) {
            var reload = context.computeOnClient(client -> {
                client.options.languageCode = language;
                client.getLanguageManager().setSelected(language);
                return client.reloadResourcePacks();
            });
            context.waitFor(client -> reload.isDone());
            reload.join();
            context.waitFor(client -> client.gui.overlay() == null);
            for (boolean advanced : new boolean[]{false, true}) {
                context.runOnClient(client -> client.options.advancedItemTooltips = advanced);
                context.setScreen(() -> new PreviewScreen());
                context.waitTicks(3);
                BuildupVitals.LOGGER.info("Tooltip screenshot: {}", context.takeScreenshot("stage5-" + suffix + "-" + language + (advanced ? "-advanced" : "-normal")));
            }
            context.setScreen(() -> null);
        }
        var reload = context.computeOnClient(client -> {
            client.options.advancedItemTooltips = false;
            client.options.languageCode = "en_us";
            client.getLanguageManager().setSelected("en_us");
            return client.reloadResourcePacks();
        });
        context.waitFor(client -> reload.isDone());
        reload.join();
        context.waitFor(client -> client.gui.overlay() == null);
    }

    private static void assertCleared(ClientGameTestContext context) {
        context.runOnClient(client -> check(!ClientFoodProfiles.available() && ClientFoodProfiles.find(STEW).isEmpty(), "Disconnected clients retain no server profile data"));
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    /** Uses the real item tooltip renderer and both mods' normal hooks, not a recreated tooltip. */
    private static class PreviewScreen extends Screen {
        PreviewScreen() { super(Component.literal("Tooltip verification")); }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            extractTransparentBackground(graphics);
            graphics.setTooltipForNextFrame(font, new ItemStack(Items.MUSHROOM_STEW), width / 2 - 90, height / 2 - 45);
        }
    }
}
