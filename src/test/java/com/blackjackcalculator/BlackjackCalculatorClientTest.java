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
    void resetHudResolvesHostToRightAndViewerToLeft() {
        assertEquals(142, BlackjackCalculatorClient.resolveHudX(true, -1, 50, 200));
        assertEquals(8, BlackjackCalculatorClient.resolveHudX(false, 8, 50, 200));
    }

    @Test
    void missingSimulationRepresentationFallsBackToActualMapId() {
        assertEquals(42, BlackjackCalculatorClient.resolveDisplayMapId(42, true, null));
    }

    @Test
    void disabledSimulationUsesActualMapId() {
        assertEquals(42, BlackjackCalculatorClient.resolveDisplayMapId(42, false, 7));
    }

    @Test
    void availableSimulationRepresentationIsUsed() {
        assertEquals(7, BlackjackCalculatorClient.resolveDisplayMapId(42, true, 7));
    }
}
