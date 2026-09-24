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

    /**
     * Selects the value used for counting a card.
     * Simulation mode deliberately does not fall back to the real value when
     * no fake value exists, so simulated scoring cannot accidentally use real cards.
     */
    public static int selectCountValue(int realValue, Integer fakeValue, boolean simulationEnabled) {
        if (!simulationEnabled) return realValue;
        return fakeValue == null ? -1 : fakeValue;
    }

    /** Returns a simulation-safe total that never exceeds 20, preserving the player's count. */
    public static int simulationViewerTotal(int total, boolean hasCards) {
        if (!hasCards) return 0;
        return Math.min(20, Math.max(0, total));
    }

    /** In win mode the host must finish above the viewer whenever the viewer has cards. */
    public static int simulationHostTotal(int hostTotal, int viewerTotal, boolean hostHasCards, boolean viewerHasCards) {
        if (!hostHasCards) return 0;
        int safeViewer = simulationViewerTotal(viewerTotal, viewerHasCards);
        return viewerHasCards ? Math.max(21, safeViewer + 1) : Math.max(0, hostTotal);
    }

    public static String display(int total, boolean hasCards) {
        if (!hasCards) return "-";
        if (total > 21) return "BUST";
        return Integer.toString(total);
    }

    public static String displayHost(int total, boolean hasCards) {
        return display(total, hasCards);
    }
}
