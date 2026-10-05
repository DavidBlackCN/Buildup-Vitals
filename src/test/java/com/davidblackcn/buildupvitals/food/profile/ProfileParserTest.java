package com.davidblackcn.buildupvitals.food.profile;

import com.davidblackcn.buildupvitals.data.loader.ProfileParser;
import com.google.gson.JsonParseException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProfileParserTest {
    static final ProfileDefinition.Source SOURCE = new ProfileDefinition.Source(Identifier.parse("test:sample"),
            Identifier.parse("test:buildup_vitals/food_profiles/sample.json"), "test-pack", 1);

    static ProfileDefinition parse(String json) throws Exception {
        return ProfileParser.parse(new StringReader(json), SOURCE);
    }

    static String document(String fields) {
        return "{\"schema_version\":1,\"selector\":{\"item\":\"minecraft:apple\"}" + fields + "}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"basic", "prepared", "meal", "feast"})
    void readsEveryQuality(String quality) throws Exception {
        assertEquals(quality, parse(document(",\"quality\":\"" + quality + "\"")).profile().quality().id());
    }

    @Test
    void parsesFullDataWithoutApplyingOverrides() throws Exception {
        var definition = parse("""
                {"schema_version":1, "selector":{"tag":"test:meals"}, "priority":-10,
                 "quality":"feast", "recovery":{"health":3.5}, "hydration":{"thirst":4,"quenched":2},
                 "diet":{"categories":["protein","grain","vegetable","fruit","dairy","sweet"],
                         "variety_group":"test:stew"},
                 "traits":["soup","warm"],"meal_benefit":"buildup_vitals:restorative",
                 "overrides":{"hunger":8,"saturation":4.5}}
                """);
        assertEquals(ProfileDefinition.Selector.Type.TAG, definition.selector().type());
        assertEquals(Identifier.parse("test:meals"), definition.selector().id());
        assertEquals(-10, definition.priority());
        var p = definition.profile();
        assertEquals(3.5, p.recoveryHealth());
        assertEquals(new FoodProfile.Hydration(4, 2), p.hydration());
        assertEquals(List.of(DietCategory.values()), p.categories());
        assertEquals(Identifier.parse("test:stew"), p.varietyGroupFor(Identifier.parse("minecraft:apple")));
        assertEquals(List.of("soup", "warm"), p.traits());
        assertEquals("buildup_vitals:restorative", p.mealBenefit().orElseThrow().toString());
        assertEquals(8, p.overrides().hunger().orElseThrow());
        assertEquals(4.5, p.overrides().saturation().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> p.traits().add("cold"));
        assertThrows(UnsupportedOperationException.class, () -> p.categories().clear());
    }

    @Test
    void omittedValuesAreNeutralAndGroupDefaultsToQueriedItem() throws Exception {
        var definition = parse(document(""));
        assertEquals(FoodProfile.fallback(), definition.profile());
        assertEquals(ProfileDefinition.Selector.Type.ITEM, definition.selector().type());
        assertEquals(Identifier.parse("minecraft:apple"), definition.profile().varietyGroupFor(definition.selector().id()));
        assertTrue(definition.profile().overrides().hunger().isEmpty());
        assertTrue(definition.profile().overrides().saturation().isEmpty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            ,"quality":"Meal" | $.quality
            ,"quality":null | $.quality
            ,"recovery":{"health":-1} | $.recovery.health
            ,"recovery":{"health":1e999} | $.recovery.health
            ,"recovery":{"health":1.7976931348623158e308} | $.recovery.health
            ,"recovery":{"health":"3"} | $.recovery.health
            ,"recovery":{"helth":3} | $.recovery.helth
            ,"hydration":{"thirst":1.5} | $.hydration.thirst
            ,"hydration":{"thirst":2147483648} | $.hydration.thirst
            ,"hydration":{"quenched":-1} | $.hydration.quenched
            ,"diet":{"categories":["soup"]} | $.diet.categories
            ,"diet":{"categories":["fruit","fruit"]} | $.diet.categories
            ,"diet":{"variety_group":"Apple"} | $.diet.variety_group
            ,"traits":["Soup"] | $.traits
            ,"traits":["warm","warm"] | $.traits
            ,"traits":"soup" | $.traits
            ,"traits":[1] | $.traits[0]
            ,"meal_benefit":"restorative" | $.meal_benefit
            ,"meal_benefit":"test:" | $.meal_benefit
            ,"meal_benefit":":test" | $.meal_benefit
            ,"meal_benefit":false | $.meal_benefit
            ,"overrides":{"hunger":-1} | $.overrides.hunger
            ,"overrides":{"saturation":-0.5} | $.overrides.saturation
            ,"priority":1.1 | $.priority
            ,"typo":1 | $.typo
            ,"quality":"basic","quality":"meal" | $.quality
            """)
    void rejectsInvalidFieldsWithPaths(String fields, String path) {
        var error = assertThrows(JsonParseException.class, () -> parse(document(fields)));
        assertTrue(error.getMessage().contains(path), error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "[]", "null", "{\"schema_version\":2,\"selector\":{\"item\":\"minecraft:apple\"}}",
            "{\"schema_version\":1,\"selector\":{}}",
            "{\"schema_version\":1,\"selector\":{\"item\":\"minecraft:apple\",\"tag\":\"test:fruit\"}}",
            "{\"schema_version\":1,\"selector\":{\"item\":\"minecraft:Apple\"}}",
            "{\"schema_version\":1,\"selector\":{\"tag\":\"fruit\"}}"
    })
    void rejectsInvalidDocumentShape(String json) {
        assertThrows(JsonParseException.class, () -> parse(json));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{} {}", "{broken", "{\"a\":NaN}", "{\"a\":1,}", "/* comment */ {}"})
    void rejectsMalformedJson(String json) {
        assertThrows(Exception.class, () -> parse(json));
    }

    @Test
    void rejectsExcessiveNestingWithoutOverflowingTheStack() {
        String nested = "[".repeat(34) + "0" + "]".repeat(34);
        var error = assertThrows(JsonParseException.class, () -> parse(nested));
        assertTrue(error.getMessage().contains("nesting exceeds"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"apple", "example_fruit", "mushroom_stew"})
    void bundledProfilesFollowSchema(String name) throws Exception {
        String path = "/data/buildup_vitals/buildup_vitals/food_profiles/" + name + ".json";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            assertNotNull(ProfileParser.parse(new InputStreamReader(stream, StandardCharsets.UTF_8), SOURCE));
        }
    }
}
