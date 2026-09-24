package com.blackjackcalculator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MapSimulationSelection {
    private MapSimulationSelection() {}

    /**
     * Builds a one-to-one simulation mapping of scanned map ids.
     *
     * The mapping is a derangement when there is more than one stored map:
     * every simulated card comes from a different stored representation.
     * This removes the old fixed "next map" bias while keeping the same
     * simulation for every frame until the simulation is toggled or rescanned.
     */
    public static Map<Integer, Integer> buildPermutation(List<Integer> knownMapIds, long seed) {
        if (knownMapIds == null || knownMapIds.isEmpty()) return Map.of();
        if (knownMapIds.size() == 1) return Map.of(knownMapIds.get(0), knownMapIds.get(0));

        List<Integer> shuffled = new ArrayList<>(knownMapIds);
        Random random = new Random(seed);
        for (int attempt = 0; attempt < 100; attempt++) {
            java.util.Collections.shuffle(shuffled, random);
            boolean valid = true;
            for (int i = 0; i < knownMapIds.size(); i++) {
                if (knownMapIds.get(i).equals(shuffled.get(i))) {
                    valid = false;
                    break;
                }
            }
            if (valid) break;
        }

        boolean valid = true;
        for (int i = 0; i < knownMapIds.size(); i++) {
            if (knownMapIds.get(i).equals(shuffled.get(i))) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            for (int i = 0; i < knownMapIds.size(); i++) {
                shuffled.set(i, knownMapIds.get((i + 1) % knownMapIds.size()));
            }
        }

        Map<Integer, Integer> permutation = new LinkedHashMap<>();
        for (int i = 0; i < knownMapIds.size(); i++) {
            permutation.put(knownMapIds.get(i), shuffled.get(i));
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
