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
        return Integer.toString(total);
    }

    public static String displayHost(int total, boolean hasCards) {
        if (!hasCards) return "-";
        return Integer.toString(Math.min(total, 21));
    }

    /**
     * Chooses a safe host HUD score for Host Always Wins mode.
     * It goes one point above a non-busted viewer hand when possible, capped at 21.
     * This changes the local display only; it does not alter server-side game state.
     */
    public static int hostAlwaysWinsTotal(int viewerTotal, boolean viewerHasCards) {
        if (!viewerHasCards || viewerTotal < 2 || viewerTotal >= 21) return 21;
        return Math.min(21, viewerTotal + 1);
    }
}
