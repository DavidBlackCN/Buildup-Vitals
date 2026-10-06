package com.davidblackcn.buildupvitals.config;

/** Built-in Alpha tuning preset. HP and server ticks (20/s); not a reloadable user config.
 * Food-specific values remain in datapack profiles. Persistence bounds must be reviewed before tuning. */
public final class CoreBalance {
    private CoreBalance() { }

    public static final class Recovery {
        public static final int MIN_NATURAL_TICKS = 10;
        public static final double QUENCHED_SPEED = 1.15;
        public static final double MAX_RESERVE = 20;
        public static final double MIN_RESERVE = 0.000001;
        public static final int FOOD_TICKS = 12;
        public static final int STABLE_TICKS = 80;
        public static final int WELL_FED_TICKS = 11;
        public static final int STABLE_HUNGER = 18;
        public static final float STABLE_SATURATION = 0;
        public static final int WELL_FED_HUNGER = 20;
        public static final float WELL_FED_SATURATION = 0;
        public static final float NATURAL_EXHAUSTION_PER_HP = 6;

        private Recovery() { }
    }

    public static final class Overeat {
        public static final int WARNING = 48;
        public static final int OVERFULL = 64;
        public static final int MAX = 80;
        public static final int CLEAR = 32;
        public static final int DECAY_TICKS = 40;
        public static final double DURATION_MULTIPLIER = 1.25;
        private Overeat() { }
    }

    public static final class Benefits {
        public static final int MAX_DURATION = 3600;
        public static final int RESTORATIVE_FOOD_TICKS = 10;
        public static final float INVIGORATED_EXHAUSTION_MULTIPLIER = 0.9F;
        public static final int BASIC_DURATION = 600;
        public static final int PREPARED_DURATION = 1200;
        public static final int MEAL_DURATION = 2400;

        private Benefits() { }
    }

    public static final class Variety {
        public static final int WINDOW = 10;
        public static final double CATEGORY_WEIGHT = 0.65;
        public static final double GROUP_WEIGHT = 0.25;
        public static final double QUALITY_WEIGHT = 0.10;
        public static final double TARGET_DIVERSITY = 4;
        public static final double MIN_REPEAT_FACTOR = 0.90;
        public static final double MAX_FOOD_BONUS = 0.15;
        public static final double MAX_BENEFIT_BONUS = 0.10;
        public static final double MAX_WELL_FED_BONUS = 0.075;

        private Variety() { }
    }
}
