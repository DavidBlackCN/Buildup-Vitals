package com.davidblackcn.buildupvitals.diet;

import java.util.ArrayList;
import java.util.List;

/** Oldest first; immutable entries and a derived, non-serialized score cached per meal/load. */
public final class DietMemory {
    public static final DietMemory EMPTY = new DietMemory(List.of());
    private final List<DietEntry> entries;
    private final VarietyResult variety;

    public DietMemory(List<DietEntry> entries) {
        if (entries.size() > VarietyBalance.WINDOW) throw new IllegalArgumentException("Diet window exceeds limit");
        this.entries = List.copyOf(entries);
        this.variety = VarietyCalculator.calculate(this.entries);
    }

    public List<DietEntry> entries() { return entries; }
    public VarietyResult variety() { return variety; }

    public DietMemory append(DietEntry entry) {
        var updated = new ArrayList<>(entries);
        if (updated.size() == VarietyBalance.WINDOW) updated.removeFirst();
        updated.add(entry);
        return new DietMemory(updated);
    }

    @Override
    public boolean equals(Object other) { return other instanceof DietMemory memory && entries.equals(memory.entries); }

    @Override
    public int hashCode() { return entries.hashCode(); }
}
