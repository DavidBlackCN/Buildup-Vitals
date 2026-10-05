package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.BuildupVitals;
import net.minecraft.resources.Identifier;

/** Buildup-owned behavior, not a vanilla MobEffect. Steady is reserved, not active. */
public enum MealBenefitType {
    RESTORATIVE("restorative", true), INVIGORATED("invigorated", true), STEADY("steady", false);

    private final Identifier id;
    private final boolean implemented;

    MealBenefitType(String path, boolean implemented) {
        this.id = Identifier.fromNamespaceAndPath(BuildupVitals.MOD_ID, path);
        this.implemented = implemented;
    }

    public Identifier id() { return id; }
    public boolean implemented() { return implemented; }
}
