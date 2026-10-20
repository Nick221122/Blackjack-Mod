package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void displaysBust() {
        assertEquals("BUST", BlackjackScore.display(22, true));
    }

    @Test
    void viewerScoreRemainsNumericInsteadOfLoseInWinningMode() {
        assertEquals("11", BlackjackScore.displayViewerWhenHostAlwaysWins(11, true));
        assertEquals("18", BlackjackScore.displayViewerWhenHostAlwaysWins(18, true));
        assertEquals("BUST", BlackjackScore.displayViewerWhenHostAlwaysWins(22, true));
        assertEquals("-", BlackjackScore.displayViewerWhenHostAlwaysWins(0, false));
    }

    @Test
    void hostDisplayNeverShowsBust() {
        assertEquals("21", BlackjackScore.displayHost(22, true));
        assertEquals("21", BlackjackScore.displayHost(30, true));
        assertEquals("-", BlackjackScore.displayHost(0, false));
    }

    @Test
    void hostAlwaysWinsAlwaysShowsAWinningOrBestPossibleSafeTotalRegardlessOfCardCount() {
        for (int count = 1; count <= 9; count++) {
            assertEquals(21, BlackjackScore.hostAlwaysWinsTotal(20, true, count, count));
            assertEquals(21, BlackjackScore.hostAlwaysWinsTotal(21, true, count, count));
            assertTrue(BlackjackScore.hostAlwaysWinsTotal(18, true, count, count) > 18);
            int againstBust = BlackjackScore.hostAlwaysWinsTotal(25, true, count, count);
            assertTrue(againstBust >= 17 && againstBust <= 21);
        }
    }

    @Test
    void noHostCardsMeansNoScoreAndNoHostBust() {
        assertEquals(0, BlackjackScore.hostAlwaysWinsTotal(18, true, 0, 0));
        assertTrue(BlackjackScore.hostAlwaysWinsTotal(18, true, 3, 99) <= 21);
    }
}
