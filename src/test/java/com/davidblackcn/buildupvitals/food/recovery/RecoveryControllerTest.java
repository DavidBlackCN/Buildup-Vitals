package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.food.recovery.RecoveryState.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryControllerTest {
    @Test
    void varietySpeedsOnlyWellFedAndPreservesPerHpCostAndProgress() {
        var step = RecoveryController.tick(new RecoveryState(0, 74, Mode.WELL_FED), conditions(10, 20, 10, true), 50, 75);
        assertEquals(1, step.healing());
        assertEquals(6, step.exhaustion(1));
        step = RecoveryController.tick(new RecoveryState(0, 74, Mode.STABLE), conditions(10, 18, 2, true), 50, 75);
        assertEquals(0, step.healing());
        assertEquals(75, step.state().progress());
        step = RecoveryController.tick(new RecoveryState(0, 78, Mode.WELL_FED), conditions(10, 20, 10, true), 50, 75);
        assertEquals(1, step.healing());
        assertEquals(0, step.state().progress());
        step = RecoveryController.tick(new RecoveryState(0, 74, Mode.WELL_FED), conditions(10, 20, 10, true), 50, 80);
        assertEquals(0, step.healing());
        assertEquals(75, step.state().progress());
    }

    @ParameterizedTest
    @CsvSource({"40", "50"})
    void speedChangesDoNotIncreaseTotalHealing(int interval) {
        var state = RecoveryState.EMPTY.addFood(3.25);
        double healed = 0;
        for (int i = 0; i < interval * 4; i++) {
            var step = RecoveryController.tick(state, conditions(10 + healed, 0, 0, false), interval);
            healed += step.healing();
            state = step.settle(step.healing());
        }
        assertEquals(3.25, healed);
        assertEquals(0, state.reserve());
    }

    @Test
    void speedChangePreservesProgressAndNaturalCost() {
        var step = RecoveryController.tick(new RecoveryState(3, 45, Mode.FOOD), conditions(10, 0, 0, false), 40);
        assertEquals(1, step.healing());
        assertEquals(2, step.settle(1).reserve());
        step = RecoveryController.tick(new RecoveryState(3, 39, Mode.FOOD), conditions(10, 0, 0, false), 50);
        assertEquals(0, step.healing());
        assertEquals(40, step.state().progress());
        step = RecoveryController.tick(new RecoveryState(0, 79, Mode.WELL_FED), conditions(10, 20, 10, true), 40);
        assertEquals(1, step.healing());
        assertEquals(6, step.exhaustion(1));
    }

    private static RecoveryController.Conditions conditions(double health, int hunger, float saturation, boolean natural) {
        return new RecoveryController.Conditions(true, true, health, 20, hunger, saturation, natural);
    }

    @Test
    void foodIsDelayedAndDamageDoesNotRestartTheInterval() {
        var state = RecoveryState.EMPTY.addFood(3);
        for (int tick = 1; tick < 50; tick++) {
            var step = RecoveryController.tick(state, conditions(tick < 25 ? 15 : 10, 0, 0, false));
            assertEquals(0, step.healing());
            state = step.state();
        }
        var step = RecoveryController.tick(state, conditions(10, 0, 0, false));
        assertEquals(1, step.healing());
        assertEquals(2, step.settle(1).reserve());
        assertEquals(0, step.exhaustion(1));
    }

    @ParameterizedTest
    @CsvSource({"18,1,120,STABLE", "20,5,120,STABLE", "20,6,80,WELL_FED", "17,10,0,NONE", "20,0,0,NONE"})
    void naturalThresholdsAndTiming(int hunger, float saturation, int interval, Mode mode) {
        var state = RecoveryState.EMPTY;
        int ticks = interval == 0 ? 240 : interval;
        for (int tick = 1; tick <= ticks; tick++) {
            var step = RecoveryController.tick(state, conditions(10, hunger, saturation, true));
            assertEquals(mode, step.state().mode());
            assertEquals(tick == interval ? 1 : 0, step.healing());
            if (tick == interval) assertEquals(6, step.exhaustion(1));
            state = step.state();
        }
    }

    @Test
    void fullHealthKeepsBoundedReserveWithoutPrecharging() {
        var state = new RecoveryState(3, 49, Mode.FOOD);
        for (int i = 0; i < 1000; i++) {
            state = RecoveryController.tick(state.addFood(5), conditions(20, 20, 20, true)).state();
        }
        assertEquals(20, state.reserve());
        assertEquals(0, state.progress());
        assertEquals(0, RecoveryController.tick(state, conditions(19, 20, 20, true)).healing());
    }

    @Test
    void feedingDoesNotResetExistingFoodProgress() {
        assertEquals(30, new RecoveryState(1, 30, Mode.FOOD).addFood(3).progress());
        var state = new RecoveryState(0, 79, Mode.WELL_FED).addFood(3);
        assertEquals(0, state.progress());
        assertEquals(Mode.FOOD, state.mode());
    }

    @Test
    void reserveHasPriorityAndNaturalRestartsAfterItRunsOut() {
        var step = RecoveryController.tick(new RecoveryState(0.5, 49, Mode.FOOD), conditions(10, 20, 10, true));
        assertEquals(0.5f, step.healing());
        var state = step.settle(0.5f);
        assertEquals(0, state.reserve());
        for (int tick = 1; tick <= 80; tick++) {
            step = RecoveryController.tick(state, conditions(10.5, 20, 10, true));
            assertEquals(tick == 80 ? 1 : 0, step.healing());
            state = step.state();
        }
    }

    @Test
    void onlyActualHealingSpendsReserveAndPartialHealthGapDoesNotWasteIt() {
        var step = RecoveryController.tick(new RecoveryState(2, 49, Mode.FOOD), conditions(19.75, 20, 10, true));
        assertEquals(0.25f, step.healing());
        assertEquals(2, step.settle(0).reserve());
        assertEquals(1.75, step.settle(0.25f).reserve());
        assertEquals(1.75, step.settle(100).reserve());
        assertEquals(2, step.settle(Float.NaN).reserve());
    }

    @Test
    void gameruleAndThresholdChangesResetNaturalProgress() {
        var state = new RecoveryState(0, 79, Mode.WELL_FED);
        assertEquals(RecoveryState.EMPTY, RecoveryController.tick(state, conditions(10, 20, 10, false)).state());
        assertEquals(RecoveryState.EMPTY, RecoveryController.tick(state, conditions(10, 17, 10, true)).state());
        var stable = RecoveryController.tick(state, conditions(10, 18, 1, true));
        assertEquals(0, stable.healing());
        assertEquals(1, stable.state().progress());
    }

    @Test
    void deathClearsButInactivePlayersPreserveReserve() {
        var state = new RecoveryState(3, 30, Mode.FOOD);
        assertEquals(RecoveryState.EMPTY, RecoveryController.tick(state,
                new RecoveryController.Conditions(false, true, 0, 20, 20, 20, true)).state());
        var inactive = RecoveryController.tick(state, new RecoveryController.Conditions(true, false, 10, 20, 20, 20, true));
        assertEquals(3, inactive.state().reserve());
        assertEquals(0, inactive.state().progress());
        assertEquals(0, inactive.healing());
    }

    @Test
    void pathologicalFoodValuesCannotOverflowOrBlockNaturalHealing() {
        assertEquals(RecoveryState.EMPTY, RecoveryState.EMPTY.addFood(Double.NaN));
        assertEquals(RecoveryState.EMPTY, RecoveryState.EMPTY.addFood(Double.POSITIVE_INFINITY));
        assertEquals(RecoveryState.EMPTY, RecoveryState.EMPTY.addFood(-1));
        assertEquals(RecoveryState.EMPTY, RecoveryState.EMPTY.addFood(Double.MIN_VALUE));
        assertEquals(20, RecoveryState.EMPTY.addFood(Double.MAX_VALUE).reserve());
        assertThrows(IllegalArgumentException.class, () -> new RecoveryState(Double.NaN, 0, Mode.NONE));
    }
}
