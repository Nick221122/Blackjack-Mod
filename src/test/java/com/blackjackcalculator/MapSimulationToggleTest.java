package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationToggleTest {
    @Test
    void blocksToggleWhenASelectedHostOrViewerFrameContainsAnItem() {
        assertTrue(BlackjackCalculatorClient.isMapSimulationToggleBlocked(true, false));
        assertTrue(BlackjackCalculatorClient.isMapSimulationToggleBlocked(false, true));
        assertFalse(BlackjackCalculatorClient.isMapSimulationToggleBlocked(false, false));
    }

    @Test
    void allowsToggleWhenAllSelectedItemFramesAreEmpty() {
        assertFalse(BlackjackCalculatorClient.isMapSimulationToggleBlocked(false, false));
    }

    @Test
    void onlyTreatsAKeyPressAsAToggleOnce() {
        assertTrue(BlackjackCalculatorClient.isNewMapSimulationKeyPress(true, false));
        assertFalse(BlackjackCalculatorClient.isNewMapSimulationKeyPress(true, true));
        assertFalse(BlackjackCalculatorClient.isNewMapSimulationKeyPress(false, true));
    }
}
