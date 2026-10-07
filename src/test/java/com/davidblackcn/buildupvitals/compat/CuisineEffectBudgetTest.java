package com.davidblackcn.buildupvitals.compat;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectBudget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CuisineEffectBudgetTest {
    @Test void shieldNeverBorrowsHungerOrExceedsPerHitCap() {
        for (float damage : new float[]{0, 4, 10, 20, 40, 1000}) for (float saturation : new float[]{0, .5F, 1, 2, 4, 20}) {
            float prevented = CuisineEffectBudget.shieldPrevented(damage, saturation);
            assertTrue(prevented >= 0 && prevented <= 4 && prevented <= damage * .2F && prevented <= saturation);
        }
    }
    @Test void warmthChoosesStrongestEnvironment() {
        assertEquals(80, 80 / CuisineEffectBudget.warmthSpeed(false,false));
        assertEquals(72, 80 / CuisineEffectBudget.warmthSpeed(false,true));
        assertEquals(64, 80 / CuisineEffectBudget.warmthSpeed(true,false));
        assertEquals(64, 80 / CuisineEffectBudget.warmthSpeed(true,true));
    }
    @Test void allPortionsExactlyConserveIntegerHydration() {
        for (int total = 0; total <= 20; total++) for (int count = 1; count <= 16; count++) {
            int sum = 0;
            for (int bite = 0; bite < count; bite++) sum += CuisineEffectBudget.portion(total,bite,count);
            assertEquals(total,sum);
        }
    }
    @Test void tavernBudgetsRemainFiniteAndDiminishing() {
        assertEquals(250, CuisineEffectBudget.mergedDuration(java.util.List.of(200,100)));
        assertEquals(200, CuisineEffectBudget.mergedDuration(java.util.List.of(200)));
        assertEquals(3221225470L, CuisineEffectBudget.mergedDuration(java.util.List.of(Integer.MAX_VALUE,Integer.MAX_VALUE)));
        for(int level=0;level<=6;level++) assertEquals(Math.max(0,level-1),CuisineEffectBudget.brewRecovery(level));
        assertEquals(.4,CuisineEffectBudget.killRecovery(4)); assertEquals(2,CuisineEffectBudget.killRecovery(500));
        assertEquals(new CuisineEffectBudget.Limit(0,300),CuisineEffectBudget.tavern("minecraft:resistance",99));
        assertEquals(new CuisineEffectBudget.Limit(1,60),CuisineEffectBudget.tavern("minecraft:strength",99));
    }
}
