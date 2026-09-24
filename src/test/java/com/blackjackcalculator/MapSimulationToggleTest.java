package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationToggleTest {
    @Test
    void blocksToggleWhileLookingAtAnItemFrame() {
        assertTrue(BlackjackCalculatorClient.isMapSimulationToggleBlocked(true));
    }

    @Test
    void allowsToggleAfterLookingAwayFromAnItemFrame() {
        assertFalse(BlackjackCalculatorClient.isMapSimulationToggleBlocked(false));
    }
}
