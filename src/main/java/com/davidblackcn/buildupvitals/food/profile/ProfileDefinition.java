package com.davidblackcn.buildupvitals.food.profile;

import net.minecraft.resources.Identifier;

public record ProfileDefinition(Source source, Selector selector, int priority, FoodProfile profile) {
    public record Source(Identifier id, Identifier file, String pack, int packPriority) { }

    public record Selector(Type type, Identifier id) {
        public enum Type { ITEM, TAG }
    }
}
