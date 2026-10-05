package com.davidblackcn.buildupvitals.hydration;

import com.davidblackcn.buildupvitals.food.profile.FoodProfile.Hydration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HydrationAdapterTest {
    @Test
    void boundsAmountsWithoutDiscardingQuenchedOrOverflowing() {
        assertEquals(new Hydration(20, 20), HydrationAdapter.bounded(new Hydration(Integer.MAX_VALUE, Integer.MAX_VALUE)));
        assertEquals(new Hydration(2, 8), HydrationAdapter.bounded(new Hydration(2, 8)));
        assertEquals(new Hydration(0, 0), HydrationAdapter.bounded(new Hydration(0, 0)));
    }
}
