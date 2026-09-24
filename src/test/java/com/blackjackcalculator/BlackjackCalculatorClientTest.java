package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackjackCalculatorClientTest {
    @Test
    void clientInitializerImplementsFabricClientModInitializer() {
        assertTrue(ClientModInitializer.class.isAssignableFrom(BlackjackCalculatorClient.class));
    }

    @Test
    void resetHudRestoresDefaultHostAndViewerNames() {
        BlackjackConfig.setHostName("Custom Host");
        BlackjackConfig.setViewerName("Custom Viewer");

        BlackjackConfig.resetHudDefaults();

        assertEquals("Host", BlackjackConfig.getHostName());
        assertEquals("Viewer", BlackjackConfig.getViewerName());
    }

    @Test
    void simulationMakesHostStandAt17OrHigher() {
        assertTrue(BlackjackCalculatorClient.hostMustStand(17, true, true));
        assertTrue(BlackjackCalculatorClient.hostMustStand(21, true, true));
        assertEquals(false, BlackjackCalculatorClient.hostMustStand(16, true, true));
        assertEquals(false, BlackjackCalculatorClient.hostMustStand(17, true, false));
        assertEquals(false, BlackjackCalculatorClient.hostMustStand(17, false, true));
    }

    @Test
    void simulationToggleDetectsOnlyThePressEdge() {
        assertTrue(BlackjackCalculatorClient.isNewMapSimulationKeyPress(true, false));
        assertEquals(false, BlackjackCalculatorClient.isNewMapSimulationKeyPress(true, true));
        assertEquals(false, BlackjackCalculatorClient.isNewMapSimulationKeyPress(false, false));
        assertEquals(false, BlackjackCalculatorClient.isNewMapSimulationKeyPress(false, true));
    }

    @Test
    void resetHudResolvesHostToRightAndViewerToLeft() {
        assertEquals(142, BlackjackCalculatorClient.resolveHudX(true, -1, 50, 200));
        assertEquals(8, BlackjackCalculatorClient.resolveHudX(false, 8, 50, 200));
    }
}
