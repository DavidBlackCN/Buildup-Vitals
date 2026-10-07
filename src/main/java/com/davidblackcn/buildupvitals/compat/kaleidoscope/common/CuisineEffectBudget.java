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
}
