package com.davidblackcn.buildupvitals.data.loader;

import com.davidblackcn.buildupvitals.BuildupVitals;
import com.davidblackcn.buildupvitals.food.profile.ProfileDefinition;
import com.davidblackcn.buildupvitals.food.profile.ProfileSnapshot;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Item;

public final class FoodProfileLoader implements PreparableReloadListener {
    public static final String DIRECTORY = "buildup_vitals/food_profiles";
    private static final DataResourceStore.Key<ProfileSnapshot> SNAPSHOT = new DataResourceStore.Key<>();

    public static void register() {
        DataResourceLoader.get().registerReloadListener(
                Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, "food_profiles"), new FoodProfileLoader());
    }

    public static ProfileSnapshot snapshot(MinecraftServer server) {
        return ((DataResourceStore) server).getOrThrow(SNAPSHOT);
    }

    @Override
    public CompletableFuture<Void> reload(SharedState state, Executor preparationExecutor,
                                          PreparationBarrier barrier, Executor applyExecutor) {
        // This lookup is safe for item keys, but its pending tag holders are not bound yet.
        var items = state.get(ResourceLoader.REGISTRY_LOOKUP_KEY).lookupOrThrow(Registries.ITEM);
        return CompletableFuture.supplyAsync(() -> load(state.resourceManager(), catalog(state.resourceManager(), items)), preparationExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(snapshot -> {
                    // This store belongs to the new ReloadableServerResources. Minecraft publishes it
                    // only when the entire reload succeeds; a later listener failure cannot leak this snapshot.
                    state.get(DataResourceLoader.DATA_RESOURCE_STORE_KEY).put(SNAPSHOT, snapshot);
                    BuildupVitals.LOGGER.info("Prepared food profile snapshot: {} profiles, {} indexed items",
                            snapshot.profileCount(), snapshot.itemCount());
                }, applyExecutor);
    }

    private static ProfileSnapshot.Catalog catalog(ResourceManager manager, HolderLookup.RegistryLookup<Item> items) {
        // Use vanilla's public tag loader to resolve this reload's full stack (replace, nested
        // tags and optional entries), without binding or mutating shared registry holders.
        var tags = TagLoader.loadTagsForRegistry(manager, Registries.ITEM,
                (id, required) -> items.get(ResourceKey.create(Registries.ITEM, id)));
        return new ProfileSnapshot.Catalog() {
            @Override
            public boolean containsItem(Identifier item) {
                return items.get(ResourceKey.create(Registries.ITEM, item)).isPresent();
            }

            @Override
            public Optional<? extends Collection<Identifier>> itemsInTag(Identifier tag) {
                return Optional.ofNullable(tags.get(TagKey.create(Registries.ITEM, tag)))
                        .map(holders -> holders.stream().map(holder -> holder.unwrapKey().orElseThrow().identifier()).toList());
            }
        };
    }

    private static ProfileSnapshot load(ResourceManager manager, ProfileSnapshot.Catalog catalog) {
        Map<PackResources, Integer> packOrder = new IdentityHashMap<>();
        var packs = manager.listPacks().toList();
        for (int index = 0; index < packs.size(); index++) {
            packOrder.put(packs.get(index), index);
        }
        var definitions = new ArrayList<ProfileDefinition>();
        // Vanilla resolves same-path pack overrides and resource filters before parsing.
        manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
            Identifier profileId = Identifier.fromNamespaceAndPath(file.getNamespace(),
                    file.getPath().substring(DIRECTORY.length() + 1, file.getPath().length() - ".json".length()));
            var source = new ProfileDefinition.Source(profileId, file, resource.sourcePackId(),
                    packOrder.get(resource.source()));
            try (var reader = resource.openAsReader()) {
                definitions.add(ProfileParser.parse(reader, source));
            } catch (IOException | JsonParseException exception) {
                BuildupVitals.LOGGER.warn("Skipping food profile {} [pack={}]: {}",
                        file, resource.sourcePackId(), exception.getMessage());
            }
        });
        return ProfileSnapshot.compile(definitions, catalog, message -> BuildupVitals.LOGGER.warn("{}", message));
    }
}
