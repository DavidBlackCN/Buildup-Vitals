package com.davidblackcn.buildupvitals.food.recovery;

import com.davidblackcn.buildupvitals.food.recovery.RecoveryState.Mode;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryControllerTest {
    private static RecoveryController.Conditions c(int hunger, float saturation, boolean natural) {
        return new RecoveryController.Conditions(true, true, 10, 20, hunger, saturation, natural);
    }
    @ParameterizedTest
    @CsvSource({"1,0.16666667", "2,0.33333333", "3,0.5", "6,1", "12,1"})
    void continuousSaturation(float saturation, float expected) {
        var step = RecoveryController.tick(new RecoveryState(0, 11, Mode.WELL_FED), c(20, saturation, true));
        assertEquals(expected, step.healing(), 1e-6);
        assertEquals(expected * 6, step.exhaustion(step.healing()), 1e-5);
        assertEquals(0, step.exhaustion(0));
    }
    @ParameterizedTest
    @CsvSource({"18,0", "19,10", "20,0"})
    void stableNeedsHungerButNotSaturation(int hunger, float saturation) {
        var state = RecoveryState.EMPTY;
        for (int i = 1; i <= 80; i++) {
            var step = RecoveryController.tick(state, c(hunger, saturation, true));
            assertEquals(i == 80 ? 1 : 0, step.healing());
            state = step.state();
        }
    }
    @ParameterizedTest
    @CsvSource({"10", "12"})
    void foodClockAndFractionalTail(int interval) {
        var state = RecoveryState.EMPTY.addFood(3.5);
        float total = 0;
        for (int i = 0; i < interval * 4; i++) {
            var step = RecoveryController.tick(state, c(0, 0, false), interval);
            total += step.healing();
            assertEquals(0, step.exhaustion(step.healing()));
            state = step.settle(step.healing());
        }
        assertEquals(3.5, total);
        assertEquals(0, state.reserve());
    }
    @Test
    void foodNeverSlowsNaturalEvenAtFractionalReserveAndBoostCap() {
        var step = RecoveryController.tick(new RecoveryState(.5, 9, Mode.FOOD), c(20, 6, true), 12, 9, true);
        assertEquals(1, step.healing());
        assertEquals(0, step.settle(1).reserve());
        assertEquals(3, step.exhaustion(1));
        assertEquals(0, step.exhaustion(.25f));
        assertEquals(.25, step.settle(.25f).reserve());
        assertEquals(10, RecoveryController.naturalInterval(c(20, 6, true), 1.15 * 1.075));
    }
    @Test
    void fractionalIntervalsActuallyRewardVarietyWithoutExceedingItsRate() {
        var state = RecoveryState.EMPTY;
        float total = 0;
        for (int i = 0; i < 1200; i++) {
            var step = RecoveryController.tick(state, c(20, 6, true), 12, 1.075, true);
            total += step.healing(); state = step.state();
        }
        assertEquals(107, total);
    }
    @Test
    void foodAddedBeforeNaturalPulseDoesNotRestartClock() {
        var state = new RecoveryState(0, 11, Mode.WELL_FED).addFood(3);
        var step = RecoveryController.tick(state, c(20, 6, true));
        assertEquals(1, step.healing());
        assertEquals(2, step.settle(1).reserve());
    }
    @Test
    void fullHealthKeepsReserveAndDamageDoesNotResetProgress() {
        var step = RecoveryController.tick(new RecoveryState(3, 11, Mode.FOOD),
                new RecoveryController.Conditions(true, true, 20, 20, 20, 6, true));
        assertEquals(3, step.state().reserve()); assertEquals(0, step.state().progress());
        var state = step.state();
        for (int i = 1; i <= 12; i++) {
            step = RecoveryController.tick(state, new RecoveryController.Conditions(true, true, 20-i, 20, 0, 0, false));
            assertEquals(i == 12 ? 1 : 0, step.healing()); state = step.state();
        }
    }
    @Test
    void actualHealingControlsCostsAndGapClipping() {
        var step = RecoveryController.tick(new RecoveryState(2, 11, Mode.FOOD),
                new RecoveryController.Conditions(true, true, 19.75, 20, 20, 6, true));
        assertEquals(.25, step.healing()); assertEquals(2, step.settle(0).reserve());
        assertEquals(1.75, step.settle(100).reserve()); assertEquals(2, step.settle(Float.NaN).reserve());
    }
    @Test
    void overfullPausesReserveButNotNatural() {
        var step = RecoveryController.tick(new RecoveryState(3, 11, Mode.FOOD), c(20, 6, true), 12, 1.0, false);
        assertEquals(1, step.healing()); assertEquals(3, step.settle(1).reserve()); assertEquals(6, step.exhaustion(1));
        step = RecoveryController.tick(step.state(), c(0, 0, false), 12, 1.0, false);
        assertEquals(0, step.healing()); assertEquals(3, step.state().reserve());
    }
    @Test
    void gameruleAndInactivePlayers() {
        assertEquals(0, RecoveryController.tick(RecoveryState.EMPTY, c(20, 6, false)).healing());
        var state = new RecoveryState(3, 11, Mode.FOOD);
        assertEquals(1, RecoveryController.tick(state, c(0, 0, false)).healing());
        assertEquals(RecoveryState.EMPTY, RecoveryController.tick(state,
                new RecoveryController.Conditions(false, true, 0, 20, 20, 6, true)).state());
        assertEquals(3, RecoveryController.tick(state,
                new RecoveryController.Conditions(true, false, 10, 20, 20, 6, true)).state().reserve());
    }
    @Test
    void legacyProgressAndFractionalProgressDecodeSafely() {
        var old = RecoveryState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"reserve\":3,\"progress\":119,\"mode\":\"stable\"}")).getOrThrow();
        assertEquals(3, old.reserve()); assertEquals(79, old.progress());
        var state = new RecoveryState(1.5, 11.75, Mode.FOOD);
        assertEquals(state, RecoveryState.CODEC.parse(JsonOps.INSTANCE,
                RecoveryState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow()).getOrThrow());
    }
    @Test
    void invalidAndTinyValuesCannotTrapRecovery() {
        assertEquals(RecoveryState.EMPTY, RecoveryState.EMPTY.addFood(Double.MIN_VALUE));
        assertEquals(RecoveryState.EMPTY, RecoveryState.EMPTY.addFood(Double.NaN));
        assertEquals(20, RecoveryState.EMPTY.addFood(Double.MAX_VALUE).reserve());
        assertThrows(IllegalArgumentException.class, () -> new RecoveryState(1, Double.NaN, Mode.FOOD));
    }
}
