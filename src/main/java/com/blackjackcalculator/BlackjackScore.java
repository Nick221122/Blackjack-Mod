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
     * Returns a client-side host HUD score based on the number of host cards/maps.
     * randomRoll is supplied by the caller so the selection can be cached per hand.
     * The host never displays a bust and does not jump to a winning score from one card.
     */
    public static int hostAlwaysWinsTotal(int viewerTotal, boolean viewerHasCards, int hostCardCount, int randomRoll) {
        if (hostCardCount <= 0) return 0;

        int min;
        int max;
        if (hostCardCount == 1) {
            min = 2;
            max = 11;
        } else if (hostCardCount == 2) {
            min = 12;
            max = 17;
        } else if (!viewerHasCards || viewerTotal > 21) {
            min = 17;
            max = 21;
        } else {
            min = Math.min(21, Math.max(18, viewerTotal + 1));
            max = 21;
        }

        if (min >= max) return min;
        return min + Math.floorMod(randomRoll, max - min + 1);
    }
}
