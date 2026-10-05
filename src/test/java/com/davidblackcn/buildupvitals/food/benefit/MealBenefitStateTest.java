package com.davidblackcn.buildupvitals.food.benefit;

import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class MealBenefitStateTest {
    @Test
    void varietyExtendsDurationWithoutBreakingRefreshOrExistingSaveCap() {
        var state = MealBenefitState.EMPTY.grant(Optional.of(MealBenefitType.RESTORATIVE.id()), FoodQuality.MEAL, 1.1);
        assertEquals(2640, state.remainingTicks());
        assertEquals(2640, state.grant(state.type(), FoodQuality.MEAL, 1).remainingTicks());
        assertEquals(3600, state.grant(state.type(), FoodQuality.FEAST, 1.1).remainingTicks());
        assertEquals(1320, state.grant(Optional.of(MealBenefitType.INVIGORATED.id()), FoodQuality.PREPARED, 1.1).remainingTicks());
        assertThrows(IllegalArgumentException.class, () -> MealBenefitState.EMPTY.grant(state.type(), FoodQuality.MEAL, 0.9));
        assertThrows(IllegalArgumentException.class, () -> MealBenefitState.EMPTY.grant(state.type(), FoodQuality.MEAL, Double.NaN));
    }

    @ParameterizedTest
    @CsvSource({"BASIC,600", "PREPARED,1200", "MEAL,2400", "FEAST,3600"})
    void qualitySetsDurationOnlyWithExplicitBenefit(FoodQuality quality, int ticks) {
        assertEquals(MealBenefitState.EMPTY, MealBenefitState.EMPTY.grant(Optional.empty(), quality));
        assertEquals(ticks, MealBenefitState.EMPTY.grant(Optional.of(MealBenefitType.RESTORATIVE.id()), quality).remainingTicks());
    }

    @Test
    void refreshNeverShortensAndRepeatedFoodCannotStackDuration() {
        var state = MealBenefitState.EMPTY.grant(Optional.of(MealBenefitType.RESTORATIVE.id()), FoodQuality.FEAST).tick();
        assertEquals(3599, state.grant(state.type(), FoodQuality.BASIC).remainingTicks());
        for (int i = 0; i < 100; i++) state = state.grant(state.type(), FoodQuality.FEAST);
        assertEquals(3600, state.remainingTicks());
        assertEquals(2400, new MealBenefitState(state.type(), 1).grant(state.type(), FoodQuality.MEAL).remainingTicks());
    }

    @Test
    void differentTypeReplacesOneSlotAndExpiryIsExact() {
        var state = MealBenefitState.EMPTY.grant(Optional.of(MealBenefitType.RESTORATIVE.id()), FoodQuality.FEAST)
                .grant(Optional.of(MealBenefitType.INVIGORATED.id()), FoodQuality.BASIC);
        assertTrue(state.is(MealBenefitType.INVIGORATED));
        assertFalse(state.is(MealBenefitType.RESTORATIVE));
        for (int i = 0; i < 599; i++) state = state.tick();
        assertEquals(1, state.remainingTicks());
        assertEquals(MealBenefitState.EMPTY, state.tick().tick());
    }

    @Test
    void unknownAndExperimentalTypesDoNotReplaceActiveBenefit() {
        var state = MealBenefitState.EMPTY.grant(Optional.of(MealBenefitType.RESTORATIVE.id()), FoodQuality.MEAL);
        for (Identifier id : new Identifier[]{Identifier.parse("example:missing"), MealBenefitType.STEADY.id()}) {
            assertEquals(state, state.grant(Optional.of(id), FoodQuality.FEAST));
            assertEquals(MealBenefitState.EMPTY, new MealBenefitState(Optional.of(id), 100).tick());
        }
        assertFalse(MealBenefitType.STEADY.implemented());
    }

    @Test
    void rejectsUnboundedOrInconsistentState() {
        assertThrows(IllegalArgumentException.class, () -> new MealBenefitState(Optional.empty(), 1));
        assertThrows(IllegalArgumentException.class, () -> new MealBenefitState(Optional.of(MealBenefitType.RESTORATIVE.id()), 3601));
        assertThrows(IllegalArgumentException.class, () -> new MealBenefitState(Optional.of(MealBenefitType.RESTORATIVE.id()), 0));
    }
}
