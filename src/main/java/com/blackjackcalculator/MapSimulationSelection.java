package com.blackjackcalculator;

import java.util.List;

public final class MapSimulationSelection {
    private MapSimulationSelection() {}

    /**
     * Returns the next scanned representation only for a map that was scanned.
     * Unknown map ids are not simulated, so nearby/unscanned maps keep their
     * original visual state.
     */
    public static Integer selectDisplayId(int actualMapId, List<Integer> knownMapIds) {
        if (knownMapIds == null || knownMapIds.isEmpty()) return null;
        int index = knownMapIds.indexOf(actualMapId);
        if (index < 0) return null;
        if (knownMapIds.size() == 1) return knownMapIds.get(0);
        return knownMapIds.get((index + 1) % knownMapIds.size());
    }
}
