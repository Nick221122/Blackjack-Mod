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
    void toggleIsBlockedOnlyWhenASelectedAreaContainsAFilledFrame() {
        assertTrue(BlackjackCalculatorClient.shouldBlockMapSimulationToggle(true, false, true));
        assertTrue(BlackjackCalculatorClient.shouldBlockMapSimulationToggle(false, true, true));
        assertTrue(BlackjackCalculatorClient.shouldBlockMapSimulationToggle(true, true, true));
        assertTrue(!BlackjackCalculatorClient.shouldBlockMapSimulationToggle(false, false, true));
        assertTrue(!BlackjackCalculatorClient.shouldBlockMapSimulationToggle(true, false, false));
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
    void resetHudResolvesHostToRightAndViewerToLeft() {
        assertEquals(142, BlackjackCalculatorClient.resolveHudX(true, -1, 50, 200));
        assertEquals(8, BlackjackCalculatorClient.resolveHudX(false, 8, 50, 200));
    }
}
