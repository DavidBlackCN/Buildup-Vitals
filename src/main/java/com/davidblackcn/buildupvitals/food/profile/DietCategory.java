package com.davidblackcn.buildupvitals.food.profile;

import java.util.Locale;

public enum DietCategory {
    PROTEIN, GRAIN, VEGETABLE, FRUIT, DAIRY, SWEET;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
