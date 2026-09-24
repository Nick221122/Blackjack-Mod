package com.blackjackcalculator;

import java.util.Random;

/** Pure card-value helpers for the local fake-card feature. */
public final class FakeCardService {
    private FakeCardService() {}

    public static int clampCardValue(int value) {
        return Math.max(1, Math.min(10, value));
    }

    public static int lowerCardValue(int value) {
        return clampCardValue(value - 1);
    }

    public static int higherCardValue(int value) {
        return clampCardValue(value + 1);
    }

    /**
     * Generates a believable local simulation where the host total is always
     * higher than the viewer total, without forcing either side to 21.
     */
    public static int[] randomizedWinningTotals(Random random) {
        int host = 17 + random.nextInt(5);
        int viewer = 12 + random.nextInt(Math.max(1, host - 12));
        return new int[] {host, viewer};
    }
}
