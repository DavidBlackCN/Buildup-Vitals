package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;

/** Pure numerical rules shared by adapters and executable balance tests. */
public final class CuisineEffectBudget {
    private CuisineEffectBudget() { }
    public static float shieldPrevented(float damage, float saturation) {
        return Math.max(0, Math.min(Math.min(damage * .20F, 4), saturation));
    }
    public static double warmthSpeed(boolean heat, boolean nether) { return heat ? 80.0 / 64 : nether ? 80.0 / 72 : 1; }
    /** Allocate integer hydration across the actual bites without rounding every bite upward. */
    public static int portion(int total, int bite, int count) { return total * (bite + 1) / count - total * bite / count; }
    public static int brewRecovery(int level) { return Math.clamp(level - 1, 0, 5); }
    public static double killRecovery(double health) { return Math.max(0, Math.min(health * .10, 2)); }
    public static long mergedDuration(java.util.List<Integer> durations) {
        long max = durations.stream().mapToLong(value -> Math.max(0, value)).max().orElse(0);
        long total = durations.stream().mapToLong(value -> Math.max(0, value)).sum();
        return max + (total - max) / 2;
    }
    public record Limit(int maxAmplifier, int seconds) { }
    public static Limit tavern(String id, int amplifier) {
        return switch (id) {
            case "minecraft:resistance" -> new Limit(0, 300);
            case "minecraft:strength" -> new Limit(1, amplifier > 0 ? 60 : 300);
            case "minecraft:haste" -> new Limit(1, amplifier > 0 ? 120 : 600);
            case "minecraft:health_boost" -> new Limit(1, 300);
            case "minecraft:fire_resistance" -> new Limit(0, 480);
            case "kaleidoscope_tavern:long_reach", "kaleidoscope_tavern:bloody_mary", "kaleidoscope_tavern:ardent_heat", "kaleidoscope_tavern:tomb_raider" -> new Limit(0, 300);
            case "minecraft:night_vision", "minecraft:water_breathing", "kaleidoscope_tavern:grass_stealth", "kaleidoscope_tavern:xp_drain", "kaleidoscope_tavern:vision" -> new Limit(0, 900);
            default -> new Limit(1, 600);
        };
    }
}
