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
    }

    @AfterEach
    void tearDown() {
        BlackjackConfig.setHostName("Host");
        BlackjackConfig.setViewerName("Viewer");
    }

    @Test
    void exposesSwappedDisplayNamesForHostAndViewer() {
        assertEquals("Viewer", BlackjackConfig.getHostName());
        assertEquals("Host", BlackjackConfig.getViewerName());
    }

    @Test
    void savesNamesBackToTheirSwappedDisplaySlots() {
        BlackjackConfig.setHostName("Viewer Slot");
        BlackjackConfig.setViewerName("Host Slot");

        assertEquals("Viewer Slot", BlackjackConfig.getHostName());
        assertEquals("Host Slot", BlackjackConfig.getViewerName());
    }
}
