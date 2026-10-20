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

    /** Keeps the viewer's visible score numeric while Host Always Wins is enabled. */
    public static String displayViewerWhenHostAlwaysWins(int total, boolean hasCards) {
        return display(total, hasCards);
    }

    /**
     * Returns the client-side Host Always Wins HUD total, independent of how many
     * host maps are currently displayed. Against a valid viewer score below 21,
     * the host shows a higher total; 21 is the best possible total against 21 or bust.
     * randomRoll is supplied by the caller so the selection can be cached per hand.
     */
    public static int hostAlwaysWinsTotal(int viewerTotal, boolean viewerHasCards, int hostCardCount, int randomRoll) {
        if (hostCardCount <= 0) return 0;

        int min;
        int max;
        if (!viewerHasCards || viewerTotal > 21) {
            min = 17;
            max = 21;
        } else if (viewerTotal >= 21) {
            return 21;
        } else {
            min = viewerTotal + 1;
            max = 21;
        }

        if (min >= max) return min;
        return min + Math.floorMod(randomRoll, max - min + 1);
    }
}
