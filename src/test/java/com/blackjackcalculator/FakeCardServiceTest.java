package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeCardServiceTest {
    @Test
    void lowerValueDoesNotGoBelowAce() {
        assertEquals(1, FakeCardService.lowerCardValue(1));
        assertEquals(4, FakeCardService.lowerCardValue(5));
    }

    @Test
    void higherValueDoesNotGoAboveTen() {
        assertEquals(10, FakeCardService.higherCardValue(10));
        assertEquals(7, FakeCardService.higherCardValue(6));
    }

    @Test
    void clampKeepsCardValuesInSupportedRange() {
        assertEquals(1, FakeCardService.clampCardValue(-4));
        assertEquals(10, FakeCardService.clampCardValue(25));
        assertEquals(6, FakeCardService.clampCardValue(6));
    }

    @Test
    void randomizedWinningTotalsAreNotForcedToTwentyOne() {
        int[] totals = FakeCardService.randomizedWinningTotals(new Random(7));
        assertTrue(totals[0] >= 17 && totals[0] <= 21);
        assertTrue(totals[1] >= 12 && totals[1] < totals[0]);
    }
}
