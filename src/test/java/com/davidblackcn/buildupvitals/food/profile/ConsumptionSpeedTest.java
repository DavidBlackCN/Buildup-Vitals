package com.davidblackcn.buildupvitals.food.profile;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.google.gson.JsonParseException;
import static org.junit.jupiter.api.Assertions.*;
class ConsumptionSpeedTest {
    @Test void durationsAndAbsentField() throws Exception {
        assertEquals(40,ConsumptionSpeed.duration(Optional.empty(),40,false));
        assertEquals(32,ConsumptionSpeed.duration(Optional.of(ConsumptionSpeed.NORMAL),40,false));
        assertEquals(21,ConsumptionSpeed.duration(Optional.of(ConsumptionSpeed.QUICK),32,false));
        assertEquals(16,ConsumptionSpeed.duration(Optional.of(ConsumptionSpeed.FAST),32,false));
        assertEquals(20,ConsumptionSpeed.duration(Optional.of(ConsumptionSpeed.FAST),32,true));
        assertEquals(27,ConsumptionSpeed.duration(Optional.of(ConsumptionSpeed.QUICK),32,true));
        assertTrue(ProfileParserTest.parse(ProfileParserTest.document("")).profile().consumptionSpeed().isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"normal","quick","fast"})
    void parsesOptionalV1Field(String tier) throws Exception {
        var p=ProfileParserTest.parse(ProfileParserTest.document(",\"consumption\":{\"speed\":\""+tier+"\"}"));
        assertEquals(tier,p.profile().consumptionSpeed().orElseThrow().id());
    }
    @ParameterizedTest @ValueSource(strings={"turbo","FAST","1.5"})
    void rejectsUnknownTier(String tier) {
        assertThrows(JsonParseException.class,()->ProfileParserTest.parse(ProfileParserTest.document(",\"consumption\":{\"speed\":\""+tier+"\"}")));
    }
}
