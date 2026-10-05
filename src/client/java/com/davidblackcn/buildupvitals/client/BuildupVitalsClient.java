package com.davidblackcn.buildupvitals.client;

import net.fabricmc.api.ClientModInitializer;

public final class BuildupVitalsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientFoodProfiles.register();
        FoodTooltips.register();
    }
}
