package com.davidblackcn.buildupvitals;

import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public class HydrationClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (HydrationAdapter.enabled()) ThirstClientScenario.run(context);
        else BuildupVitals.LOGGER.info("Stage 6 client without TWT2 passed; optional scenario NOT RUN");
    }
}
