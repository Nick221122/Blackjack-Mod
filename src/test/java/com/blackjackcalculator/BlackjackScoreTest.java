package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackjackScoreTest {
    @Test
    void scoresBasicHandsAndAces() {
        assertEquals(17, BlackjackScore.score(17, 0));
        assertEquals(20, BlackjackScore.score(9, 1));
        assertEquals(12, BlackjackScore.score(1, 1));
        assertEquals(12, BlackjackScore.score(11, 1));
        assertEquals(14, BlackjackScore.score(2, 2));
        assertEquals(21, BlackjackScore.score(20, 1));
        assertEquals(22, BlackjackScore.score(21, 1));
        assertEquals(19, BlackjackScore.score(19, 0));
        assertEquals(21, BlackjackScore.score(10, 1));
        assertEquals(22, BlackjackScore.score(20, 2));
        assertEquals(13, BlackjackScore.score(12, 1));
    }

    @Test
    void simulationNeverLetsViewerBust() {
        assertEquals(20, BlackjackScore.simulationViewerTotal(20, true));
        assertEquals(20, BlackjackScore.simulationViewerTotal(22, true));
        assertEquals(20, BlackjackScore.simulationViewerTotal(40, true));
    }

    @Test
    void simulationMakesHostWinWhenBothHandsHaveCards() {
        assertEquals(21, BlackjackScore.simulationHostTotal(18, 16, true, true));
        assertEquals(21, BlackjackScore.simulationHostTotal(18, 20, true, true));
        assertEquals(0, BlackjackScore.simulationHostTotal(18, 0, true, false));
    }

    @Test
    void displaysBust() {
        assertEquals("BUST", BlackjackScore.display(22, true));
    }

    @Test
    void hostDisplayShowsBustWhenOverTwentyOne() {
        assertEquals("BUST", BlackjackScore.displayHost(22, true));
        assertEquals("BUST", BlackjackScore.displayHost(30, true));
        assertEquals("-", BlackjackScore.displayHost(0, false));
    }
}
