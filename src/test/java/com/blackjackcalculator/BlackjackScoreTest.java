package com.blackjackcalculator;

/** Lightweight source-level test cases; run with any Java test runner if desired. */
public final class BlackjackScoreTest {
    public static void main(String[] args) {
        check(17, BlackjackScore.score(17, 0));
        check(20, BlackjackScore.score(9, 1)); // 9 + Ace = 20
        check(12, BlackjackScore.score(1, 1));
        check(12, BlackjackScore.score(11, 1));
        check(14, BlackjackScore.score(2, 2));
        check(21, BlackjackScore.score(20, 1)); // Aces remain 11 when possible
        check(22, BlackjackScore.score(21, 1));
        check(19, BlackjackScore.score(19, 0)); // 10 + 9
        check(21, BlackjackScore.score(10, 1)); // 10 + Ace = 21
        if (!"Bust".equals(BlackjackScore.display(22, true))) throw new AssertionError("Bust display failed");

        check(22, BlackjackScore.score(20, 2));
        check(13, BlackjackScore.score(12, 1));

        BlackjackConfig.resetPositions();
        if (BlackjackConfig.getViewerX() != 8 || BlackjackConfig.getHostX() != -1) {
            throw new AssertionError("HUD defaults must place Viewer left and Host right");
        }
        if (!"HOST • Dealer: 18".equals(BlackjackCalculatorClient.formatHudLabel(BlackjackConfig.Role.HOST, "Dealer", 18, true))) {
            throw new AssertionError("Host HUD label failed");
        }
        if (!"VIEWER • Player: 20".equals(BlackjackCalculatorClient.formatHudLabel(BlackjackConfig.Role.VIEWER, "Player", 20, true))) {
            throw new AssertionError("Viewer HUD label failed");
        }
        System.out.println("BlackjackScoreTest: PASS");
    }

    private static void check(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
