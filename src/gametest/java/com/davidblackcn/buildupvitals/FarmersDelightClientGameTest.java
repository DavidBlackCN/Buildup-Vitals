package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.client.ClientFoodProfiles;
import com.davidblackcn.buildupvitals.compat.farmersdelight.FarmersDelightCompatibility;
import com.davidblackcn.buildupvitals.effect.BuildupEffects;
import com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.player.DietAttachments;
import com.davidblackcn.buildupvitals.player.RecoveryAttachments;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
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

public class FarmersDelightClientGameTest implements FabricClientGameTest {
    private static final Identifier SOUP = Identifier.parse("farmersdelight:chicken_soup");
    private static final Identifier NOURISHMENT = FarmersDelightCompatibility.NOURISHMENT;

    @Override public void runTest(ClientGameTestContext context) {
        if (!FarmersDelightCompatibility.supported()) {
            check(!ForeignMealBenefits.registered(NOURISHMENT), "No FD client registration when absent");
            BuildupVitals.LOGGER.info("FD absent client gate passed");
            return;
        }
        try (var world = context.worldBuilder().create()) {
            exercise(context, world.getServer(), world.getConnection());
            visuals(context);
        }
        try (var server = context.worldBuilder().createServer()) {
            try (var connection = server.connect()) { exercise(context, server, connection); }
            try (var connection = server.connect()) {
                context.waitFor(client -> ClientFoodProfiles.find(SOUP).isPresent());
                server.runOnServer(instance -> check(connection.getServerPlayer().hasEffect(ForeignMealBenefits.holder(NOURISHMENT).orElseThrow()), "Foreign effect persists across TCP reconnect"));
                assertClient(context);
            }
        }
        BuildupVitals.LOGGER.info("FD connected tests passed: integrated + dedicated, profile reload/removal, native duration, icons, beverage tooltips, reconnect; AppleSkin={}",
                net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("appleskin"));
    }

    private static void exercise(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
        context.waitFor(client -> ClientFoodProfiles.find(SOUP).isPresent());
        assertClient(context);
        server.runOnServer(instance -> {
            var p = connection.getServerPlayer();
            prepare(p);
            FarmersDelightGameTests.food("chicken_soup").finishUsingItem(p.level(), p);
            check(p.getEffect(ForeignMealBenefits.holder(NOURISHMENT).orElseThrow()).getDuration() == 6000, "FD native duration unaffected by profile quality/variety");
            check(PlayerRecovery.state(p).reserve() == 3, "Real connected player gets one reserve");
            var advancement = instance.getAdvancements().get(Identifier.parse("farmersdelight:main/eat_nourishing_food"));
            check(advancement != null && p.getAdvancements().getOrStartProgress(advancement).isDone(), "FD advancement still observes actual Nourishment ID");
        });
        context.waitFor(client -> client.player.hasEffect(ForeignMealBenefits.holder(NOURISHMENT).orElseThrow()));
        context.runOnClient(client -> check(!client.player.hasEffect(BuildupEffects.RESTORATIVE) && !client.player.hasEffect(BuildupEffects.INVIGORATED), "Only Nourishment icon synced"));
        Path pack = server.computeOnServer(instance -> instance.getWorldPath(LevelResource.DATAPACK_DIR).resolve("buildup_fd_override"));
        Path profile = pack.resolve("data/buildup_vitals/buildup_vitals/food_profiles/farmersdelight/chicken_soup.json");
        write(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"FD override test\",\"min_format\":121,\"max_format\":121}}");
        for (String benefit : new String[]{"none", "buildup_vitals:restorative", "buildup_vitals:invigorated", "farmersdelight:nourishment"}) {
            write(profile, "{\"schema_version\":1,\"selector\":{\"item\":\"farmersdelight:chicken_soup\"},\"quality\":\"prepared\",\"recovery\":{\"health\":1},\"hydration\":{\"thirst\":3,\"quenched\":1},\"consumption\":{\"speed\":\"quick\"}"
                    + (benefit.equals("none") ? "" : ",\"meal_benefit\":\"" + benefit + "\"") + "}");
            reload(context, server);
            context.waitFor(client -> ClientFoodProfiles.find(SOUP).map(p -> p.recovery() == 1 && p.benefit().map(Object::toString).orElse("none").equals(benefit)).orElse(false));
            server.runOnServer(instance -> {
                var p = connection.getServerPlayer();
                prepare(p);
                var stack = FarmersDelightGameTests.food("chicken_soup");
                check(stack.getUseDuration(p) == 21, "Reloaded server speed wins");
                stack.finishUsingItem(p.level(), p);
                check(PlayerRecovery.state(p).reserve() == 1, "Reloaded reserve wins");
                var active = ForeignMealBenefits.mainEffects().stream().filter(p::hasEffect).toList();
                check(benefit.equals("none") ? active.isEmpty() : active.size() == 1
                        && BuiltInRegistries.MOB_EFFECT.getKey(active.getFirst().value()).toString().equals(benefit), "Effective profile controls final effect: " + benefit);
                if (benefit.equals(NOURISHMENT.toString())) check(p.getEffect(active.getFirst()).getDuration() == 6000, "Foreign native duration is not replaced by Prepared duration");
                if (com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled()) {
                    var h = com.davidblackcn.buildupvitals.hydration.HydrationAdapter.find(SOUP);
                    check(h.thirst() == 3 && h.quenched() == 1, "Reload updates TWT overlay");
                }
            });
            context.runOnClient(client -> {
                var stack = FarmersDelightGameTests.food("chicken_soup");
                check(stack.getUseDuration(client.player) == 21, "Reloaded client speed agrees");
                var lines = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
                check(count(lines, "tooltip.buildup_vitals.benefit") == (benefit.equals("none") ? 0 : 1), "One effective benefit tooltip after reload");
                check(count(lines, "effect.farmersdelight.nourishment") == (benefit.equals(NOURISHMENT.toString()) ? 1 : 0), "No stale FD native tooltip after override");
            });
        }
        try { Files.delete(profile); } catch (java.io.IOException e) { throw new AssertionError(e); }
        reload(context, server);
        context.waitFor(client -> ClientFoodProfiles.find(SOUP).map(p -> p.recovery() == 3).orElse(false));
        server.runOnServer(instance -> {
            var p = connection.getServerPlayer();
            prepare(p);
            FarmersDelightGameTests.food("chicken_soup").finishUsingItem(p.level(), p);
        });
    }

    private static void prepare(net.minecraft.server.level.ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.setNoGravity(true);
        p.setPermanentlyInvulnerable(true);
        p.setHealth(10);
        p.getFoodData().setFoodLevel(0);
        p.getFoodData().setSaturation(0);
        p.removeAllEffects();
        p.removeAttached(PlayerOvereat.STATE);
        p.removeAttached(RecoveryAttachments.RECOVERY);
        p.removeAttached(DietAttachments.DIET);
    }
    private static void assertClient(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (String name : List.of("chicken_soup", "milk_bottle", "hot_cocoa", "melon_juice", "sweet_berry_cookie", "tomato")) {
                var stack = FarmersDelightGameTests.food(name);
                var lines = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
                check(lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents text && text.getKey().startsWith("tooltip.buildup_vitals.quality.")), "Profile tooltip includes beverages: " + name);
                check(lines.stream().noneMatch(line -> line.getString().contains("tooltip.buildup_vitals")), "Translated tooltip: " + name);
                if (name.equals("chicken_soup")) check(count(lines, "effect.farmersdelight.nourishment") == 1 && count(lines, "tooltip.buildup_vitals.benefit") == 1, "Nourishment appears once with native name");
                if (name.equals("tomato")) check(stack.getUseDuration(client.player) == 21, "Synced quick consumption");
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
            var packs = instance.getPackRepository(); packs.reload();
            var selected = new ArrayList<>(packs.getSelectedIds());
            if (!selected.contains("file/buildup_fd_override")) selected.add("file/buildup_fd_override");
            return instance.reloadResources(selected);
        });
        context.waitFor(client -> future.isDone()); future.join();
    }
    private static void write(Path path, String text) {
        try { Files.createDirectories(path.getParent()); Files.writeString(path, text); }
        catch (java.io.IOException e) { throw new AssertionError(e); }
    }
    private static void visuals(ClientGameTestContext context) {
        for (String language : List.of("zh_cn", "en_us")) {
            var future = context.computeOnClient(client -> {
                client.options.languageCode = language; client.getLanguageManager().setSelected(language);
                return client.reloadResourcePacks();
            });
            context.waitFor(client -> future.isDone()); future.join();
            context.waitFor(client -> client.gui.overlay() == null);
            assertClient(context);
            context.setScreen(() -> new Screen(Component.literal("FD tooltip verification")) {
                @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
                    extractTransparentBackground(graphics);
                    graphics.setTooltipForNextFrame(font, FarmersDelightGameTests.food("chicken_soup"), width / 2 - 80, height / 2 - 40);
                }
            });
            context.waitTicks(3);
            BuildupVitals.LOGGER.info("FD screenshot {}", context.takeScreenshot("fd-" + language + "-twt-"
                    + com.davidblackcn.buildupvitals.hydration.HydrationAdapter.enabled() + "-appleskin-"
                    + net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("appleskin")));
            context.setScreen(() -> null);
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
