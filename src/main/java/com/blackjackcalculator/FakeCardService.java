package com.blackjackcalculator;

import net.minecraft.item.ItemStack;

/** Pure card manipulation helpers used by the client-side fake-card feature. */
public final class FakeCardService {
    private FakeCardService() {}

    public static int clampCardValue(int value) {
        return Math.max(1, Math.min(10, value));
    }

    public static int lowerCardValue(int value) {
        return value <= 1 ? 1 : value - 1;
    }

    public static int higherCardValue(int value) {
        return value >= 10 ? 10 : value + 1;
    }

    /** Returns a copy that preserves all components while replacing the map id/value carrier. */
    public static ItemStack copyStack(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }
}
