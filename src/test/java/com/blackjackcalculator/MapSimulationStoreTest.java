package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationStoreTest {
    @Test
    void simulationRequiresTheRenderedFrameToBeSelected() {
        assertTrue(MapSimulationStore.shouldSimulateForFrame(true, true, true));
        assertFalse(MapSimulationStore.shouldSimulateForFrame(true, false, true));
        assertFalse(MapSimulationStore.shouldSimulateForFrame(false, true, true));
        assertFalse(MapSimulationStore.shouldSimulateForFrame(true, true, false));
    }
}
