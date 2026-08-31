package com.blackjackcalculator;

/** Pure blackjack scoring logic. An Ace starts as 11 and becomes 1 when needed. */
public final class BlackjackScore {
    private BlackjackScore() {}

    public static int score(int fixedTotal, int aces) {
        int total = fixedTotal + (aces * 11);
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public static String display(int total, boolean hasCards) {
        if (!hasCards) return "-";
        if (total > 21) return "BUST";
        if (total == 21) return "BLACKJACK!";
        return Integer.toString(total);
    }
}
