package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackjackNarrationTest {
    @Test
    void speaksBothCurrentCounts() {
        assertEquals(
                "Host count 17. Viewer count 12",
                BlackjackNarration.formatCount("Host", "17", true, "Viewer", "12", true));
    }

    @Test
    void handlesHandsWithoutCards() {
        assertEquals(
                "Host has no cards. Viewer count BUST",
                BlackjackNarration.formatCount("Host", "0", false, "Viewer", "BUST", true));
    }
}
