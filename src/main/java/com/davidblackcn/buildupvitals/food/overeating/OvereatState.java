package com.davidblackcn.buildupvitals.food.overeating;

import com.davidblackcn.buildupvitals.config.CoreBalance.Overeat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Hysteresis and warning latch cannot be derived from load in the 32..63 band. */
public record OvereatState(int load, int digestTicks, boolean overfull, boolean warned) {
    public static final OvereatState EMPTY = new OvereatState(0, 0, false, false);
    public static final Codec<OvereatState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, Overeat.MAX).fieldOf("load").forGetter(OvereatState::load),
            Codec.intRange(0, Overeat.DECAY_TICKS - 1).optionalFieldOf("digest_ticks", 0).forGetter(OvereatState::digestTicks),
            Codec.BOOL.optionalFieldOf("overfull", false).forGetter(OvereatState::overfull),
            Codec.BOOL.optionalFieldOf("warned", false).forGetter(OvereatState::warned)
    ).apply(i, OvereatState::new));

    public OvereatState {
        if (load < 0 || load > Overeat.MAX || digestTicks < 0 || digestTicks >= Overeat.DECAY_TICKS)
            throw new IllegalArgumentException("Invalid overeat state");
        overfull = load >= Overeat.OVERFULL || (overfull && load >= Overeat.CLEAR);
        warned = load >= Overeat.WARNING || (warned && load >= Overeat.CLEAR);
        if (load == 0) digestTicks = 0;
    }
    public OvereatState eat(int hungerBefore, int nutrition) {
        int missing = 20 - Math.clamp(hungerBefore, 0, 20);
        long overflow = Math.max(0L, (long) nutrition - missing);
        return new OvereatState((int) Math.min(Overeat.MAX, load + overflow), digestTicks, overfull, warned);
    }
    public OvereatState tick() {
        if (load == 0) return EMPTY;
        return digestTicks + 1 == Overeat.DECAY_TICKS
                ? new OvereatState(load - 1, 0, overfull, warned)
                : new OvereatState(load, digestTicks + 1, overfull, warned);
    }
    public int remainingOverfullTicks() {
        return overfull ? (load - Overeat.CLEAR + 1) * Overeat.DECAY_TICKS - digestTicks : 0;
    }
}
