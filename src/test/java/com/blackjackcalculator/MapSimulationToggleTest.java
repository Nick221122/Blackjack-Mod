package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationToggleTest {
    @Test
    void blocksToggleWhileLookingAtAnItemFrame() {
        assertTrue(BlackjackCalculatorClient.canToggleMapSimulation(false));
    }

    @Test
    void allowsToggleAfterLookingAwayFromAnItemFrame() {
        assertFalse(BlackjackCalculatorClient.canToggleMapSimulation(true));
    }
}
