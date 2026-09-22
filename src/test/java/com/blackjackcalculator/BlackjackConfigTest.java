package com.blackjackcalculator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackjackConfigTest {
    @BeforeEach
    void setUp() {
        BlackjackConfig.setHostName("Host");
        BlackjackConfig.setViewerName("Viewer");
        BlackjackConfig.resetPositions();
    }

    @AfterEach
    void tearDown() {
        BlackjackConfig.setHostName("Host");
        BlackjackConfig.setViewerName("Viewer");
        BlackjackConfig.resetPositions();
    }

    @Test
    void exposesHostAndViewerDisplayNames() {
        assertEquals("Host", BlackjackConfig.getHostName());
        assertEquals("Viewer", BlackjackConfig.getViewerName());
    }

    @Test
    void savesHostAndViewerNamesToTheirOwnDisplaySlots() {
        BlackjackConfig.setHostName("Host Slot");
        BlackjackConfig.setViewerName("Viewer Slot");

        assertEquals("Host Slot", BlackjackConfig.getHostName());
        assertEquals("Viewer Slot", BlackjackConfig.getViewerName());
    }

    @Test
    void resetPositionsUsesViewerLeftAndHostRightDefaults() {
        BlackjackConfig.resetPositions();

        assertEquals(-1, BlackjackConfig.getHostX());
        assertEquals(8, BlackjackConfig.getViewerX());
        assertEquals(8, BlackjackConfig.getHostY());
        assertEquals(8, BlackjackConfig.getViewerY());
    }
}
