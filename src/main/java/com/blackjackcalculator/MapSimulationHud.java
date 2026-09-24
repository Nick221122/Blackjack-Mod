package com.blackjackcalculator;

public final class MapSimulationHud {
    public static final int HEART_TEXTURE_SIZE = 9;
    public static final int SUPPRESSED_HEART_PIXEL_X = 7;
    public static final int SUPPRESSED_HEART_PIXEL_Y = 0;

    private static boolean heartSuppressionUsed;

    private MapSimulationHud() {}

    public static void resetHeartSuppression() {
        heartSuppressionUsed = false;
    }

    public static boolean claimHeartSuppression(boolean simulationEnabled, boolean containerHeart) {
        if (!simulationEnabled || !containerHeart || heartSuppressionUsed) return false;
        heartSuppressionUsed = true;
        return true;
    }

    public static boolean shouldSuppressHeartPixel(int textureX, int textureY) {
        return textureX == SUPPRESSED_HEART_PIXEL_X
                && textureY == SUPPRESSED_HEART_PIXEL_Y;
    }
}
