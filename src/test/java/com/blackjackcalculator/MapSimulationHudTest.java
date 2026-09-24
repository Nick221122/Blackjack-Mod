package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationHudTest {
    @Test
    void suppressesTheRightTopHeartPixelOnlyWhileSimulationIsEnabled() {
        assertTrue(MapSimulationHud.shouldSuppressHeartPixel(true, 7, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(false, 7, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(true, 6, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(true, 7, 1));
    }
}
