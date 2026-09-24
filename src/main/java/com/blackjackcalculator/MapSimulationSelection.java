package com.blackjackcalculator;

import java.util.List;

/** Pure selection logic for choosing a simulated map representation. */
public final class MapSimulationSelection {
    private MapSimulationSelection() {}

    /**
     * Selects the next scanned representation only when the rendered map itself
     * is one of the scanned map ids. Unknown map ids are left untouched.
     */
    public static Integer selectDisplayId(int actualMapId, List<Integer> knownMapIds) {
        if (knownMapIds == null || knownMapIds.isEmpty()) return null;
        int index = knownMapIds.indexOf(actualMapId);
        if (index < 0) return null;
        if (knownMapIds.size() == 1) return knownMapIds.get(0);
        return knownMapIds.get((index + 1) % knownMapIds.size());
    }
}