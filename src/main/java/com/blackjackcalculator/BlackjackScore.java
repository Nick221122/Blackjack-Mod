package com.blackjackcalculator;

/** Pure blackjack scoring. Every Ace starts at 11 and is reduced by 10 only when needed. */
public final class BlackjackScore {
    private BlackjackScore() {}

    public static int score(int fixedTotal, int aces) {
        int total = fixedTotal + aces * 11;
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public static String display(int total, boolean hasCards) {
        if (!hasCards) return "-";
        if (total > 21) return "Bust";
        if (total == 21) return "Blackjack";
        return Integer.toString(total);
    }
}
