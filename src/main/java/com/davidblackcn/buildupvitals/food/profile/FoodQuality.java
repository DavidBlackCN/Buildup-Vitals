package com.davidblackcn.buildupvitals.food.profile;

import java.util.Locale;

public enum FoodQuality {
    BASIC, PREPARED, MEAL, FEAST;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
