package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class HydrationGameTests {
    @GameTest
    public void optionalAdapter(GameTestHelper helper) {
        if (HydrationAdapter.enabled()) {
            ThirstTestSupport.exercise(RecoveryGameTests.player(helper));
            BuildupVitals.LOGGER.info("Stage 6 TWT2 consumption, config, blacklist, overflow and purity tests passed");
        } else {
            helper.assertTrue(HydrationAdapter.find(net.minecraft.resources.Identifier.parse("minecraft:apple")) == null,
                    "Absent dependency has no hydration overlay");
            BuildupVitals.LOGGER.info("Stage 6 absent-TWT2 adapter guard passed; installed-mod cases NOT RUN");
        }
        helper.succeed();
    }
}
