package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSimulationHudTest {
    @Test
    void suppressesOnlyOneContainerHeartPixelWhileSimulationIsEnabled() {
        MapSimulationHud.resetHeartSuppression();
        assertTrue(MapSimulationHud.claimHeartSuppression(true, true));
        assertFalse(MapSimulationHud.claimHeartSuppression(true, true));
        assertFalse(MapSimulationHud.claimHeartSuppression(true, false));
        MapSimulationHud.resetHeartSuppression();
        assertFalse(MapSimulationHud.claimHeartSuppression(false, true));
        assertTrue(MapSimulationHud.claimHeartSuppression(true, true));
    }

    @Test
    void targetsOnlyTheTopRightPixel() {
        assertTrue(MapSimulationHud.shouldSuppressHeartPixel(7, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(6, 0));
        assertFalse(MapSimulationHud.shouldSuppressHeartPixel(7, 1));
    }
}
