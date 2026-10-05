package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import com.thirstwastaken2.api.ThirstApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

final class ThirstClientScenario {
    private static final Identifier FALLBACK_ID = Identifier.parse("buildup_vitals_test:fallback_food");

    static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var profile = new com.davidblackcn.buildupvitals.network.TooltipProfile(
                    com.davidblackcn.buildupvitals.food.profile.FoodQuality.BASIC, 0, java.util.List.of(),
                    java.util.Optional.empty(), FALLBACK_ID, java.util.Optional.of(FALLBACK_ID),
                    new com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration(6, 8));
            HydrationAdapter.receive(true, Map.of(FALLBACK_ID, profile));
            check(values(6, 8), "Pure remote-client cache accepts server values without a local server");
            HydrationAdapter.receive(false, Map.of(FALLBACK_ID, profile));
            check(ThirstApi.thirstValues(TestFoods.FALLBACK) == null, "Server-disabled capability removes prior values");
            HydrationAdapter.receive(true, Map.of(FALLBACK_ID, profile));
            HydrationAdapter.disconnect();
            check(ThirstApi.thirstValues(TestFoods.FALLBACK) == null, "Remote disconnect invalidates TWT2 cache");
        });
        try (var world = context.worldBuilder().create()) {
            exercise(context, world.getServer(), world.getConnection(), true);
        }
        check(HydrationAdapter.find(FALLBACK_ID) == null, "Stopped world discards hydration overlay");
        try (var server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) { exercise(context, server, connection, false); }
            try (var connection = server.connect()) {
                context.waitFor(client -> ClientFoodProfiles.available());
                context.runOnClient(client -> check(ThirstApi.thirstValues(TestFoods.FALLBACK) == null, "Reconnect has no stale profile"));
            }
        }
        check(HydrationAdapter.find(FALLBACK_ID) == null, "Dedicated server shutdown clears hydration overlay");
        BuildupVitals.LOGGER.info("Stage 6 connected hydration tests passed: integrated + dedicated, priority, single consumption, reload, rollback, removal, reconnect; AppleSkin={}",
                FabricLoader.getInstance().isModLoaded("appleskin"));
    }

    private static void exercise(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, boolean screenshot) {
        context.waitFor(client -> ClientFoodProfiles.available());
        server.runOnServer(instance -> ThirstTestSupport.exercise(connection.getServerPlayer()));
        Path pack = server.computeOnServer(instance -> instance.getWorldPath(LevelResource.DATAPACK_DIR).resolve("buildup_hydration_test"));
        Path profile = pack.resolve("data/buildup_vitals_test/buildup_vitals/food_profiles/hydration.json");
        Path drink = pack.resolve("data/buildup_vitals_test/thirstwastaken2/drinks/hydration.json");
        write(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Hydration test\",\"min_format\":121,\"max_format\":121}}");
        write(profile, profile(6, 8));
        reload(context, server, false);
        awaitProfile(context, 6);
        context.runOnClient(client -> {
            check(values(6, 8), "Client API uses synchronized Profile hydration");
            HydrationAdapter.receive(true, Map.of());
            check(values(6, 8), "Client display packets cannot replace a running server's gameplay overlay");
        });
        server.runOnServer(instance -> ThirstTestSupport.consume(connection.getServerPlayer(), new ItemStack(TestFoods.FALLBACK), 5, 0, 11, 8));
        if (screenshot) {
            context.waitFor(client -> client.gui.overlay() == null);
            context.setScreen(PreviewScreen::new);
            context.waitTicks(3);
            context.takeScreenshot("stage6-hydration-" + (FabricLoader.getInstance().isModLoaded("appleskin") ? "appleskin" : "standalone"));
            context.setScreen(() -> null);
        }

        write(profile, profile(Integer.MAX_VALUE, Integer.MAX_VALUE));
        reload(context, server, false);
        awaitProfile(context, Integer.MAX_VALUE);
        context.runOnClient(client -> check(values(20, 20), "Extreme finite profile amounts are bounded before TWT2 addition"));
        server.runOnServer(instance -> ThirstTestSupport.consume(connection.getServerPlayer(), new ItemStack(TestFoods.FALLBACK), 5, 0, 20, 20));
        write(profile, profile(2, 1));
        reload(context, server, false);
        awaitProfile(context, 2);
        context.runOnClient(client -> check(values(2, 1), "Reload invalidates TWT2 value cache"));
        write(drink, drink(1, 4));
        reload(context, server, false);
        context.waitFor(client -> values(1, 4));
        server.runOnServer(instance -> ThirstTestSupport.consume(connection.getServerPlayer(), new ItemStack(TestFoods.FALLBACK), 5, 0, 6, 4));
        write(drink, drink(0, 0));
        reload(context, server, false);
        context.waitFor(client -> ThirstApi.thirstValues(TestFoods.FALLBACK) == null);
        server.runOnServer(instance -> ThirstTestSupport.consume(connection.getServerPlayer(), new ItemStack(TestFoods.FALLBACK), 5, 0, 5, 0));
        delete(drink);
        reload(context, server, false);
        context.waitFor(client -> values(2, 1));

        write(profile, profile(9, 2));
        TooltipReloadFailure.FAIL_NEXT.set(true);
        reload(context, server, true);
        context.runOnClient(client -> check(values(2, 1), "Failed reload retains successful hydration snapshot"));
        server.runOnServer(instance -> ThirstTestSupport.consume(connection.getServerPlayer(), new ItemStack(TestFoods.FALLBACK), 5, 0, 7, 1));
        delete(profile);
        reload(context, server, false);
        context.waitFor(client -> ClientFoodProfiles.find(FALLBACK_ID).map(p -> p.profileId().isEmpty()).orElse(false));
        context.runOnClient(client -> check(ThirstApi.thirstValues(TestFoods.FALLBACK) == null, "Removed profile does not leak hydration"));
    }

    private static void awaitProfile(ClientGameTestContext context, int thirst) {
        context.waitFor(client -> ClientFoodProfiles.find(FALLBACK_ID).map(p -> p.hydration().thirst() == thirst).orElse(false));
    }

    private static boolean values(int thirst, int quenched) {
        var value = ThirstApi.thirstValues(TestFoods.FALLBACK);
        return value != null && value[0] == thirst && value[1] == quenched;
    }

    private static String profile(int thirst, int quenched) {
        return "{\"schema_version\":1,\"selector\":{\"item\":\"buildup_vitals_test:fallback_food\"},\"hydration\":{\"thirst\":" + thirst + ",\"quenched\":" + quenched + "}}";
    }

    private static String drink(int thirst, int quenched) {
        return "{\"values\":{\"buildup_vitals_test:fallback_food\":{\"thirst\":" + thirst + ",\"quenched\":" + quenched + "}}}";
    }

    private static void reload(ClientGameTestContext context, TestServerContext server, boolean failure) {
        CompletableFuture<Void> future = server.computeOnServer(instance -> {
            var repository = instance.getPackRepository();
            repository.reload();
            var selected = new ArrayList<>(repository.getSelectedIds());
            if (!selected.contains("file/buildup_hydration_test")) selected.add("file/buildup_hydration_test");
            return instance.reloadResources(selected);
        });
        context.waitFor(client -> future.isDone());
        if (!failure) future.join();
        else {
            check(future.isCompletedExceptionally(), "Intentional reload failure occurs");
            try { future.join(); } catch (java.util.concurrent.CompletionException exception) {
                check(exception.getCause().getMessage().contains("Intentional Stage 5 reload failure"), "Failure comes from test fixture");
            }
        }
    }

    private static void write(Path path, String text) {
        try { Files.createDirectories(path.getParent()); Files.writeString(path, text); }
        catch (IOException exception) { throw new AssertionError(exception); }
    }

    private static void delete(Path path) {
        try { Files.delete(path); } catch (IOException exception) { throw new AssertionError(exception); }
    }

    private static void check(boolean condition, String message) { ThirstTestSupport.check(condition, message); }

    private static final class PreviewScreen extends Screen {
        private PreviewScreen() { super(Component.literal("Hydration verification")); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
            extractTransparentBackground(graphics);
            graphics.setTooltipForNextFrame(font, new ItemStack(TestFoods.FALLBACK), width / 2 - 50, height / 2 - 45);
        }
    }
}
