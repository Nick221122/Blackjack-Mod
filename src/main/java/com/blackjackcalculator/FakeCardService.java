package com.blackjackcalculator;

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
}
