package com.davidblackcn.buildupvitals.data.loader;

import com.davidblackcn.buildupvitals.food.profile.DietCategory;
import com.davidblackcn.buildupvitals.food.profile.ConsumptionSpeed;
import com.davidblackcn.buildupvitals.food.profile.FoodProfile;
import com.davidblackcn.buildupvitals.food.profile.FoodQuality;
import com.davidblackcn.buildupvitals.food.profile.ProfileDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Strict schema-v1 parser. Every diagnostic includes the JSON field path. */
public final class ProfileParser {
    private ProfileParser() { }

    public static ProfileDefinition parse(Reader input, ProfileDefinition.Source source) throws IOException {
        JsonElement document;
        try (JsonReader reader = new JsonReader(input)) {
            reader.setStrictness(Strictness.STRICT);
            document = readJson(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw error("$", "expected exactly one JSON document");
            }
        }
        JsonObject root = object(document, "$", "schema_version", "selector", "priority", "quality",
                "recovery", "hydration", "diet", "traits", "meal_benefit", "overrides", "consumption");
        if (integer(required(root, "schema_version", "$"), "$.schema_version") != 1) {
            throw error("$.schema_version", "only version 1 is supported");
        }
        JsonObject selector = object(required(root, "selector", "$"), "$.selector", "item", "tag");
        if (selector.size() != 1) {
            throw error("$.selector", "specify exactly one of item or tag");
        }
        boolean item = selector.has("item");
        String selectorField = item ? "item" : "tag";
        var target = new ProfileDefinition.Selector(item ? ProfileDefinition.Selector.Type.ITEM
                : ProfileDefinition.Selector.Type.TAG, id(selector.get(selectorField), "$.selector." + selectorField));
        int priority = root.has("priority") ? integer(root.get("priority"), "$.priority") : 0;
        FoodQuality quality = FoodQuality.BASIC;
        if (root.has("quality")) {
            String value = string(root.get("quality"), "$.quality");
            quality = Arrays.stream(FoodQuality.values()).filter(q -> q.id().equals(value)).findFirst()
                    .orElseThrow(() -> error("$.quality", "expected basic, prepared, meal or feast"));
        }
        JsonObject consumption = section(root, "consumption", "speed");
        Optional<ConsumptionSpeed> speed = Optional.empty();
        if (consumption.has("speed")) {
            String value = string(consumption.get("speed"), "$.consumption.speed");
            speed = Optional.of(Arrays.stream(ConsumptionSpeed.values()).filter(t -> t.id().equals(value)).findFirst()
                    .orElseThrow(() -> error("$.consumption.speed", "expected normal, quick or fast")));
        }
        JsonObject recovery = section(root, "recovery", "health");
        JsonObject hydration = section(root, "hydration", "thirst", "quenched");
        JsonObject diet = section(root, "diet", "categories", "variety_group");
        JsonObject overrides = section(root, "overrides", "hunger", "saturation");
        List<DietCategory> categories = new ArrayList<>();
        for (String value : strings(diet, "categories", "$.diet.categories")) {
            categories.add(Arrays.stream(DietCategory.values()).filter(c -> c.id().equals(value)).findFirst()
                    .orElseThrow(() -> error("$.diet.categories", "unknown category " + value)));
        }
        List<String> traits = strings(root, "traits", "$.traits");
        for (String trait : traits) {
            if (!trait.matches("[a-z0-9_]+")) {
                throw error("$.traits", "trait must match [a-z0-9_]+: " + trait);
            }
        }
        FoodProfile profile = new FoodProfile(quality,
                recovery.has("health") ? nonnegativeNumber(recovery.get("health"), "$.recovery.health") : 0,
                new FoodProfile.Hydration(nonnegativeInt(hydration, "thirst", "$.hydration"),
                        nonnegativeInt(hydration, "quenched", "$.hydration")),
                categories, optionalId(diet, "variety_group", "$.diet.variety_group"), traits,
                optionalId(root, "meal_benefit", "$.meal_benefit"),
                new FoodProfile.Overrides(overrides.has("hunger")
                        ? OptionalInt.of(nonnegativeInt(overrides, "hunger", "$.overrides")) : OptionalInt.empty(),
                        overrides.has("saturation")
                                ? OptionalDouble.of(nonnegativeNumber(overrides.get("saturation"), "$.overrides.saturation"))
                                : OptionalDouble.empty()), speed);
        return new ProfileDefinition(source, target, priority, profile);
    }

    // Building the tree explicitly rejects duplicate fields rather than silently accepting the last one.
    private static JsonElement readJson(JsonReader reader, int depth) throws IOException {
        if (depth > 32) {
            throw error(reader.getPath(), "JSON nesting exceeds 32 levels");
        }
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) {
                        throw error(reader.getPath(), "duplicate field");
                    }
                    object.add(name, readJson(reader, depth + 1));
                }
                reader.endObject();
                yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) {
                    array.add(readJson(reader, depth + 1));
                }
                reader.endArray();
                yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> {
                String path = reader.getPath();
                String value = reader.nextString();
                try {
                    yield new JsonPrimitive(new BigDecimal(value));
                } catch (NumberFormatException exception) {
                    throw error(path, "invalid number");
                }
            }
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> throw error(reader.getPath(), "expected JSON value");
        };
    }

    private static JsonObject object(JsonElement value, String path, String... fields) {
        if (value == null || !value.isJsonObject()) {
            throw error(path, "expected object");
        }
        JsonObject object = value.getAsJsonObject();
        Set<String> allowed = Set.of(fields);
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) {
                throw error(path + "." + key, "unknown field");
            }
        }
        return object;
    }

    private static JsonObject section(JsonObject root, String name, String... fields) {
        return root.has(name) ? object(root.get(name), "$." + name, fields) : new JsonObject();
    }

    private static JsonElement required(JsonObject object, String name, String path) {
        if (!object.has(name)) {
            throw error(path + "." + name, "required field is missing");
        }
        return object.get(name);
    }

    private static String string(JsonElement value, String path) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw error(path, "expected string");
        }
        return value.getAsString();
    }

    private static Identifier id(JsonElement value, String path) {
        String text = string(value, path);
        int colon = text.indexOf(':');
        Identifier id = Identifier.tryParse(text);
        if (colon <= 0 || colon == text.length() - 1 || id == null) {
            throw error(path, "expected a namespaced ID (namespace:path)");
        }
        return id;
    }

    private static Optional<Identifier> optionalId(JsonObject object, String field, String path) {
        return object.has(field) ? Optional.of(id(object.get(field), path)) : Optional.empty();
    }

    private static BigDecimal number(JsonElement value, String path) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw error(path, "expected number");
        }
        return value.getAsBigDecimal();
    }

    private static int integer(JsonElement value, String path) {
        try {
            return number(value, path).intValueExact();
        } catch (ArithmeticException exception) {
            throw error(path, "expected 32-bit integer");
        }
    }

    private static int nonnegativeInt(JsonObject object, String name, String path) {
        int value = object.has(name) ? integer(object.get(name), path + "." + name) : 0;
        if (value < 0) {
            throw error(path + "." + name, "must be nonnegative");
        }
        return value;
    }

    private static double nonnegativeNumber(JsonElement element, String path) {
        BigDecimal value = number(element, path);
        double result = value.doubleValue();
        if (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 || !Double.isFinite(result)) {
            throw error(path, "must be finite and nonnegative");
        }
        return result;
    }

    private static List<String> strings(JsonObject object, String field, String path) {
        if (!object.has(field)) {
            return List.of();
        }
        JsonElement value = object.get(field);
        if (!value.isJsonArray()) {
            throw error(path, "expected array");
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement element : value.getAsJsonArray()) {
            String text = string(element, path + "[" + result.size() + "]");
            if (!seen.add(text)) {
                throw error(path, "duplicate value " + text);
            }
            result.add(text);
        }
        return result;
    }

    private static JsonParseException error(String path, String reason) {
        return new JsonParseException(path + ": " + reason);
    }
}
