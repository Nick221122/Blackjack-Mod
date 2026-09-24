package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapSimulationSelectionTest {
    @Test
    void selectsTheNextStoredRepresentationForKnownMapIds() {
        assertEquals(20, MapSimulationSelection.selectDisplayId(10, List.of(10, 20, 30)));
        assertEquals(30, MapSimulationSelection.selectDisplayId(20, List.of(10, 20, 30)));
        assertEquals(10, MapSimulationSelection.selectDisplayId(30, List.of(10, 20, 30)));
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
