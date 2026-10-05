package com.davidblackcn.buildupvitals.food.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileSnapshotTest {
    private static final Identifier APPLE = Identifier.parse("minecraft:apple");
    private static final Identifier MELON = Identifier.parse("minecraft:melon_slice");
    private static final Identifier FRUIT = Identifier.parse("test:fruit");
    private final List<String> warnings = new ArrayList<>();

    private static ProfileDefinition definition(String id, ProfileDefinition.Selector.Type type,
                                                Identifier target, int pack, int priority) {
        return new ProfileDefinition(new ProfileDefinition.Source(Identifier.parse(id),
                Identifier.parse(id).withPrefix("buildup_vitals/food_profiles/").withSuffix(".json"), "pack-" + pack, pack),
                new ProfileDefinition.Selector(type, target), priority, FoodProfile.fallback());
    }

    private static ProfileSnapshot.Catalog catalog(Map<Identifier, List<Identifier>> tags) {
        return new ProfileSnapshot.Catalog() {
            @Override
            public boolean containsItem(Identifier item) {
                return Set.of(APPLE, MELON).contains(item);
            }

            @Override
            public Optional<? extends Collection<Identifier>> itemsInTag(Identifier tag) {
                return Optional.ofNullable(tags.get(tag));
            }
        };
    }

    private ProfileSnapshot compile(ProfileDefinition... definitions) {
        return ProfileSnapshot.compile(List.of(definitions), catalog(Map.of(FRUIT, List.of(APPLE, MELON))), warnings::add);
    }

    @Test
    void itemWinsOverHigherPriorityTagAndTagMatchesOtherMembers() {
        var item = definition("test:apple", ProfileDefinition.Selector.Type.ITEM, APPLE, 0, -100);
        var tag = definition("test:fruit", ProfileDefinition.Selector.Type.TAG, FRUIT, 10, 100);
        var snapshot = compile(tag, item);
        assertEquals(item, snapshot.resolve(APPLE).definition().orElseThrow());
        assertEquals(tag, snapshot.resolve(MELON).definition().orElseThrow());
        assertFalse(snapshot.resolve(MELON).fallback());
        assertEquals(2, snapshot.profileCount());
        assertEquals(2, snapshot.itemCount());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void packStackOutranksNumericPriority() {
        var builtin = definition("test:a", ProfileDefinition.Selector.Type.ITEM, APPLE, 0, 999);
        var pack = definition("test:z", ProfileDefinition.Selector.Type.ITEM, APPLE, 1, -999);
        assertEquals(pack, compile(builtin, pack).resolve(APPLE).definition().orElseThrow());
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("pack-1"));
        assertTrue(warnings.getFirst().contains("test:buildup_vitals/food_profiles/a.json"));
    }

    @Test
    void explicitPriorityAndIdBreakTiesIndependentlyOfInputOrder() {
        var low = definition("test:aaa", ProfileDefinition.Selector.Type.TAG, FRUIT, 1, -1);
        var a = definition("a:z", ProfileDefinition.Selector.Type.TAG, FRUIT, 1, 0);
        var b = definition("b:a", ProfileDefinition.Selector.Type.TAG, FRUIT, 1, 0);
        assertEquals(a, compile(b, a, low).resolve(APPLE).definition().orElseThrow());
        assertEquals(2, warnings.size()); // One diagnostic per conflicting profile pair, not per item.
        assertEquals(a, compile(low, a, b).resolve(APPLE).definition().orElseThrow());
    }

    @Test
    void fallbackKeepsZeroExtrasAndDoesNotInventDietCategories() {
        var match = compile().resolve(APPLE);
        assertTrue(match.fallback());
        assertEquals(FoodQuality.BASIC, match.profile().quality());
        assertEquals(0, match.profile().recoveryHealth());
        assertEquals(new FoodProfile.Hydration(0, 0), match.profile().hydration());
        assertTrue(match.profile().categories().isEmpty());
        assertTrue(match.profile().overrides().hunger().isEmpty());
        assertTrue(match.profile().overrides().saturation().isEmpty());
    }

    @Test
    void reloadDeletionAndTagChangesProduceFreshSnapshotsWithoutMutatingOldOne() {
        var tag = definition("test:fruit", ProfileDefinition.Selector.Type.TAG, FRUIT, 0, 0);
        var definitions = new ArrayList<>(List.of(tag));
        var members = new ArrayList<>(List.of(APPLE));
        var first = ProfileSnapshot.compile(definitions, catalog(Map.of(FRUIT, members)), warnings::add);
        members.clear();
        members.add(MELON);
        var second = ProfileSnapshot.compile(definitions, catalog(Map.of(FRUIT, members)), warnings::add);
        definitions.clear();
        var third = ProfileSnapshot.compile(definitions, catalog(Map.of(FRUIT, members)), warnings::add);
        assertFalse(first.resolve(APPLE).fallback());
        assertTrue(first.resolve(MELON).fallback());
        assertTrue(second.resolve(APPLE).fallback());
        assertFalse(second.resolve(MELON).fallback());
        assertTrue(third.resolve(APPLE).fallback());
        assertTrue(third.resolve(MELON).fallback());
        assertEquals(0, third.profileCount());
    }

    @Test
    void missingItemAndTagAreDiagnosedAndDoNotHideValidProfiles() {
        var badItem = definition("test:bad_item", ProfileDefinition.Selector.Type.ITEM, Identifier.parse("absent:food"), 1, 0);
        var badTag = definition("test:bad_tag", ProfileDefinition.Selector.Type.TAG, Identifier.parse("absent:fruit"), 1, 0);
        var good = definition("test:good", ProfileDefinition.Selector.Type.ITEM, APPLE, 0, 0);
        var snapshot = compile(badItem, badTag, good);
        assertEquals(1, snapshot.profileCount());
        assertEquals(good, snapshot.resolve(APPLE).definition().orElseThrow());
        assertEquals(2, warnings.size());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("$.selector.item")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("$.selector.tag")));
    }

    @Test
    void emptyKnownTagIsValidAndIndexesNothing() {
        var tag = definition("test:empty", ProfileDefinition.Selector.Type.TAG, FRUIT, 0, 0);
        var snapshot = ProfileSnapshot.compile(List.of(tag), catalog(Map.of(FRUIT, List.of())), warnings::add);
        assertEquals(1, snapshot.profileCount());
        assertEquals(0, snapshot.itemCount());
        assertTrue(warnings.isEmpty());
    }
}
