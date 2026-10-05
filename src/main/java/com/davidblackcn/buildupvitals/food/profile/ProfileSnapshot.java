package com.davidblackcn.buildupvitals.food.profile;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

/** A complete, immutable item index. Each reload builds a new instance from scratch. */
public final class ProfileSnapshot {
    private static final FoodProfile FALLBACK = FoodProfile.fallback();
    private static final Comparator<ProfileDefinition> PRECEDENCE = Comparator
            .comparing((ProfileDefinition d) -> d.selector().type())
            .thenComparing(Comparator.comparingInt((ProfileDefinition d) -> d.source().packPriority()).reversed())
            .thenComparing(Comparator.comparingInt(ProfileDefinition::priority).reversed())
            .thenComparing(d -> d.source().id().toString());

    private final Map<Identifier, ProfileDefinition> byItem;
    private final int profileCount;

    private ProfileSnapshot(Map<Identifier, ProfileDefinition> byItem, int profileCount) {
        this.byItem = Map.copyOf(byItem);
        this.profileCount = profileCount;
    }

    public static ProfileSnapshot compile(Collection<ProfileDefinition> definitions, Catalog catalog,
                                          Consumer<String> warning) {
        Map<Identifier, ProfileDefinition> index = new HashMap<>();
        Set<List<Identifier>> reportedConflicts = new HashSet<>();
        int accepted = 0;
        for (ProfileDefinition definition : definitions.stream().sorted(PRECEDENCE).toList()) {
            var selector = definition.selector();
            Collection<Identifier> items;
            if (selector.type() == ProfileDefinition.Selector.Type.ITEM) {
                if (!catalog.containsItem(selector.id())) {
                    warning.accept(describe(definition) + " $.selector.item: unknown item " + selector.id());
                    continue;
                }
                items = List.of(selector.id());
            } else {
                var members = catalog.itemsInTag(selector.id());
                if (members.isEmpty()) {
                    warning.accept(describe(definition) + " $.selector.tag: unknown tag " + selector.id());
                    continue;
                }
                items = members.get();
            }
            accepted++;
            for (Identifier item : items.stream().sorted(Comparator.comparing(Identifier::toString)).toList()) {
                ProfileDefinition winner = index.putIfAbsent(item, definition);
                if (winner != null && winner.selector().type() == selector.type()
                        && reportedConflicts.add(List.of(winner.source().id(), definition.source().id()))) {
                    warning.accept("Profile conflict for " + item + ": " + describe(winner)
                            + " wins over " + describe(definition)
                            + " (pack priority, then priority, then ascending profile ID)");
                }
            }
        }
        return new ProfileSnapshot(index, accepted);
    }

    private static String describe(ProfileDefinition definition) {
        return definition.source().file() + " [pack=" + definition.source().pack() + "]";
    }

    public Match resolve(Identifier item) {
        ProfileDefinition definition = byItem.get(item);
        return new Match(definition == null ? FALLBACK : definition.profile(), Optional.ofNullable(definition));
    }

    public int profileCount() {
        return profileCount;
    }

    public int itemCount() {
        return byItem.size();
    }

    public record Match(FoodProfile profile, Optional<ProfileDefinition> definition) {
        public boolean fallback() {
            return definition.isEmpty();
        }
    }

    /** Item keys and tag contents from this reload, never from the previous world's static tags. */
    public interface Catalog {
        boolean containsItem(Identifier item);

        Optional<? extends Collection<Identifier>> itemsInTag(Identifier tag);
    }
}
