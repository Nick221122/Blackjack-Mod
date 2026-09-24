package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapSimulationSelectionTest {
    @Test
    void simulationPermutationUsesEveryStoredRepresentationWithoutKeepingTheSameMap() {
        List<Integer> ids = List.of(10, 20, 30);
        Map<Integer, Integer> permutation = MapSimulationSelection.buildPermutation(ids, 12345L);

        assertEquals(ids.size(), permutation.size());
        assertEquals(ids.stream().sorted().toList(), permutation.keySet().stream().sorted().toList());
        assertEquals(ids.stream().sorted().toList(), permutation.values().stream().sorted().toList());

        for (int id : ids) {
            assertNotEquals(id, permutation.get(id));
        }
    }

    @Test
    void scoringCanUseTheSameSimulatedMapAsTheRenderedMap() {
        Map<Integer, Integer> mapping = Map.of(10, 20, 20, 30, 30, 10);

        assertEquals(20, MapSimulationSelection.selectMappedDisplayId(10, mapping));
        assertEquals(30, MapSimulationSelection.selectMappedDisplayId(20, mapping));
        assertEquals(10, MapSimulationSelection.selectMappedDisplayId(30, mapping));
        assertNull(MapSimulationSelection.selectMappedDisplayId(999, mapping));
    }

    @Test
    void simulatedValueComesFromTheMappedFakeCardValue() {
        Map<Integer, Integer> mapping = Map.of(10, 20, 20, 30, 30, 10);
        Map<Integer, Integer> fakeValues = Map.of(10, 11, 20, 10, 30, 5);

        assertEquals(10, MapSimulationSelection.selectMappedValue(10, mapping, fakeValues));
        assertEquals(5, MapSimulationSelection.selectMappedValue(20, mapping, fakeValues));
        assertEquals(11, MapSimulationSelection.selectMappedValue(30, mapping, fakeValues));
        assertNull(MapSimulationSelection.selectMappedValue(999, mapping, fakeValues));
    }

    @Test
    void simulatedValueDoesNotFallBackToTheRealMapValue() {
        Map<Integer, Integer> mapping = Map.of(10, 20);
        Map<Integer, Integer> fakeValues = Map.of(10, 3, 20, 9);

        assertEquals(9, MapSimulationSelection.selectMappedValue(10, mapping, fakeValues));
    }

    @Test
    void unknownMapIdsAreNotSimulated() {
        assertNull(MapSimulationSelection.selectDisplayId(999, List.of(10, 20)));
    }

    @Test
    void aSingleStoredRepresentationOnlyAppliesToThatMap() {
        assertEquals(10, MapSimulationSelection.selectDisplayId(10, List.of(10)));
        assertNull(MapSimulationSelection.selectDisplayId(999, List.of(10)));
    }

    @Test
    void noStoredRepresentationsDisableSelection() {
        assertNull(MapSimulationSelection.selectDisplayId(10, List.of()));
        assertNull(MapSimulationSelection.selectDisplayId(10, null));
    }
}
