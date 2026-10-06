package com.davidblackcn.buildupvitals.food.recovery;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Half-health recovery with real controller steps and finite nutrition, not a fixed-saturation rate. */
class RecoveryBalanceComparisonTest {
    private record Result(int ticks, float health, float saturation, double exhaustion) { }

    private static final class Nutrition {
        int hunger;
        float saturation;
        float exhaustion;
        double totalExhaustion;
        Nutrition(int hunger, float saturation) { this.hunger = hunger; this.saturation = saturation; }
        void tick() {
            // Target 26.3 FoodData.tick processes one exhaustion unit before its healing branches.
            if (exhaustion > 4) {
                exhaustion -= 4;
                if (saturation > 0) saturation = Math.max(0, saturation - 1);
                else hunger = Math.max(0, hunger - 1);
            }
        }
        void spend(float amount) { exhaustion = Math.min(40, exhaustion + amount); totalExhaustion += amount; }
    }

    private static Result buildup(int hunger, float saturation, double reserve, int foodTicks, double speed) {
        var food = new Nutrition(hunger, saturation);
        var state = RecoveryState.EMPTY.addFood(reserve);
        float health = 10;
        for (int tick = 1; tick <= 6000; tick++) {
            food.tick();
            var step = RecoveryController.tick(state, new RecoveryController.Conditions(true, true,
                    health, 20, food.hunger, food.saturation, true), foodTicks, speed, true);
            assertTrue(step.healing() <= 1, "Food and natural never add two full pulses");
            float before = health;
            health = Math.min(20, health + step.healing());
            food.spend(step.exhaustion(health - before));
            state = step.settle(health - before);
            if (health == 20) return new Result(tick, health, food.saturation, food.totalExhaustion);
        }
        return new Result(-1, health, food.saturation, food.totalExhaustion);
    }

    private static Result vanilla(int hunger, float saturation) {
        // Independent reference from the resolved MC 26.3 FoodData source: 10/80 ticks,
        // min(saturation,6)/6 HP, exhaustion >4, full offered cost even on the clipped last pulse.
        var food = new Nutrition(hunger, saturation);
        float health = 10;
        int timer = 0;
        for (int tick = 1; tick <= 6000; tick++) {
            food.tick();
            boolean fast = food.hunger >= 20 && food.saturation > 0;
            if (food.hunger >= 18) {
                if (++timer >= (fast ? 10 : 80)) {
                    float spent = fast ? Math.min(food.saturation, 6) : 6;
                    float amount = spent / 6;
                    health = Math.min(20, health + amount);
                    food.spend(spent);
                    timer = 0;
                }
            } else timer = 0;
            if (health == 20) return new Result(tick, health, food.saturation, food.totalExhaustion);
        }
        return new Result(-1, health, food.saturation, food.totalExhaustion);
    }

    private static double profile(String item) throws Exception {
        var path = Path.of("src/main/resources/data/buildup_vitals/buildup_vitals/food_profiles", item + ".json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("recovery").get("health").getAsDouble();
    }

    @Test void halfHealthComparisonWithFiniteNutrition() throws Exception {
        var vanilla = vanilla(20, 20);
        assertEquals(100, vanilla.ticks());
        double apple = profile("apple"), stew = profile("mushroom_stew"), rabbit = profile("rabbit_stew");
        String[] names = {"natural", "apple", "rabbit_meal", "restorative_meal", "quenched",
                "apple_quenched", "max_variety", "restorative_max_variety", "restorative_all_bonuses"};
        double[] reserves = {0, apple, rabbit, stew, 0, apple, 0, stew * 1.15, stew * 1.15};
        int[] foodTicks = {12, 12, 12, 10, 12, 12, 12, 10, 10};
        double[] speeds = {1, 1, 1, 1, 1.15, 1.15, 1.075, 1.075, 1.075 * 1.15};
        int[] expectedTicks = {100, 100, 100, 100, 100, 100, 100, 100, 100};
        Result ordinary = null;
        for (int i = 0; i < names.length; i++) {
            var result = buildup(20, 20, reserves[i], foodTicks[i], speeds[i]);
            assertEquals(expectedTicks[i], result.ticks(), names[i]);
            System.out.printf("BALANCE,%s,reserve=%.4f,ticks=%d,seconds=%.2f,exhaustion=%.4f,saturation=%.4f%n",
                    names[i], reserves[i], result.ticks(), result.ticks() / 20.0, result.exhaustion(), result.saturation());
            assertTrue(result.ticks() >= vanilla.ticks(), "Vitals never exceed the vanilla high-saturation peak");
            assertTrue(result.ticks() <= buildup(20, 20, 0, 12, speeds[i]).ticks(), "Food never delays natural recovery");
            if (i == 0) ordinary = result;
        }
        for (float saturation : new float[]{6, 10, 12.8f, 15}) {
            var reference = vanilla(20, saturation);
            var natural = buildup(20, saturation, 0, 12, 1);
            var result = buildup(20, saturation, apple, 12, 1);
            System.out.printf("BALANCE,finite_sat_%.1f,vanilla_ticks=%d,vanilla_health=%.4f,natural_ticks=%d,apple_ticks=%d,apple_health=%.4f%n",
                    saturation, reference.ticks(), reference.health(), natural.ticks(), result.ticks(), result.health());
            if (saturation == 15) {
                assertEquals(140, reference.ticks()); assertEquals(140, natural.ticks()); assertEquals(110, result.ticks());
            } else if (saturation == 12.8f) {
                assertEquals(310, reference.ticks()); assertEquals(300, natural.ticks()); assertEquals(230, result.ticks());
            } else {
                assertEquals(-1, reference.ticks()); assertEquals(-1, result.ticks());
                assertEquals(reference.health() + 1, result.health(), 1e-4);
            }
        }
        assertEquals(800, vanilla(19, 20).ticks());
        assertEquals(800, buildup(19, 20, 0, 12, 1).ticks());
        assertEquals(696, buildup(19, 20, 0, 12, 1.15).ticks());
        assertEquals(vanilla.ticks(), ordinary.ticks(), "Passive recovery uses the vanilla baseline");
    }
}
