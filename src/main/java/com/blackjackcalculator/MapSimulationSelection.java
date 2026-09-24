package com.blackjackcalculator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapSimulationSelection {
    private MapSimulationSelection() {}

    /**
     * Builds a one-to-one simulation mapping of scanned map ids.
     *
     * The mapping is a one-to-one derangement that follows the exact scanned
     * dispenser order: each card uses the next scanned representation, wrapping
     * the last card back to the first.
     */
    public static Map<Integer, Integer> buildPermutation(List<Integer> knownMapIds, long seed) {
        if (knownMapIds == null || knownMapIds.isEmpty()) return Map.of();
        if (knownMapIds.size() == 1) return Map.of(knownMapIds.get(0), knownMapIds.get(0));

        // Preserve the exact dispenser scan order: each map uses the next scanned
        // map as its fake representation, and the last scanned map wraps to the first.
        Map<Integer, Integer> permutation = new LinkedHashMap<>();
        for (int i = 0; i < knownMapIds.size(); i++) {
            permutation.put(
                    knownMapIds.get(i),
                    knownMapIds.get((i + 1) % knownMapIds.size())
            );
        }
        return permutation;
    }

    /** Returns the fake card value belonging to the simulated map selected for an actual map. */
    public static Integer selectMappedValue(
            int actualMapId,
            Map<Integer, Integer> mapping,
            Map<Integer, Integer> fakeValues
    ) {
        Integer mappedId = selectMappedDisplayId(actualMapId, mapping);
        return mappedId == null || fakeValues == null ? null : fakeValues.get(mappedId);
    }

    /** Returns the simulated map id for an already-built simulation mapping. */
    public static Integer selectMappedDisplayId(int actualMapId, Map<Integer, Integer> mapping) {
        if (mapping == null) return null;
        return mapping.get(actualMapId);
    }
}
