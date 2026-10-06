package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.command.FoodProfileCommand;
import com.davidblackcn.buildupvitals.command.RecoveryCommand;
import com.davidblackcn.buildupvitals.data.loader.FoodProfileLoader;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.food.benefit.PlayerMealBenefits;
import com.davidblackcn.buildupvitals.diet.PlayerDiet;
import com.davidblackcn.buildupvitals.command.DietCommand;
import com.davidblackcn.buildupvitals.network.FoodProfileSync;
import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildupVitals implements ModInitializer {
    public static final String MOD_ID = "buildup_vitals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FoodProfileLoader.register();
        HydrationAdapter.register();
        FoodProfileSync.register();
        FoodProfileCommand.register();
        com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat.register();
        PlayerRecovery.register();
        PlayerMealBenefits.register();
        PlayerDiet.register();
        DietCommand.register();
        RecoveryCommand.register();
        LOGGER.info("Buildup Vitals recovery, meal benefits and diet memory initialized.");
    }
}
