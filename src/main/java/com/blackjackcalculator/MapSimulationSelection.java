package com.blackjackcalculator;

import java.util.List;

public final class MapSimulationSelection {
    private MapSimulationSelection() {}

    public static Integer selectDisplayId(int actualMapId, List<Integer> knownMapIds) {
        if (knownMapIds == null || knownMapIds.isEmpty()) return null;
        int index = knownMapIds.indexOf(actualMapId);
        // A displayed map may not be one of the scanned IDs. Still map it to a
        // scanned visual representation so the overlay works for nearby frames.
        if (index < 0) return knownMapIds.get(0);
        if (knownMapIds.size() == 1) return knownMapIds.get(0);
        return knownMapIds.get((index + 1) % knownMapIds.size());
    }
}
