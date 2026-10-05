package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.command.FoodProfileCommand;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildupVitals implements ModInitializer {
    public static final String MOD_ID = "buildup_vitals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FoodProfileLoader.register();
        FoodProfileCommand.register();
        LOGGER.info("Buildup Vitals food profile foundation initialized.");
    }
}
