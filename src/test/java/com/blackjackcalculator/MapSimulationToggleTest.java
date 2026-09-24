package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationToggleTest {
    @Test
    void blocksToggleWhenAnItemFrameContainsAnItem() {
        assertTrue(BlackjackCalculatorClient.isMapSimulationToggleBlocked(true));
    }

    @Test
    void allowsToggleWhenAllItemFramesAreEmpty() {
        assertFalse(BlackjackCalculatorClient.isMapSimulationToggleBlocked(false));
    }
}
