package com.blackjackcalculator;

public final class BlackjackNarration {
    private BlackjackNarration() {}

    public static String formatCount(
            String hostName,
            String hostScore,
            boolean hostHasCards,
            String viewerName,
            String viewerScore,
            boolean viewerHasCards) {
        StringBuilder message = new StringBuilder();

        appendCount(message, hostName, hostScore, hostHasCards);
        appendCount(message, viewerName, viewerScore, viewerHasCards);

        return message.toString();
    }

    private static void appendCount(
            StringBuilder message,
            String name,
            String score,
            boolean hasCards) {
        if (message.length() > 0) message.append(". ");
        message.append(name == null || name.isBlank() ? "Hand" : name);
        message.append(hasCards ? " count " : " has no cards");
        if (hasCards) message.append(score);
    }
}
