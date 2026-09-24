package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationHudTest {
    @Test
    void suppressesTheRightTopContainerHeartPixelOnlyWhileSimulationIsEnabled() {
        assertTrue(MapSimulationHud.shouldSuppressHeartPixel(true, true, 7, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(true, false, 7, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(false, true, 7, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(true, true, 6, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(true, true, 7, 1));
    }
}
