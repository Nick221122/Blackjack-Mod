package com.blackjackcalculator;

public final class MapSimulationHud {
    public static final int HEART_TEXTURE_SIZE = 9;
    public static final int SUPPRESSED_HEART_PIXEL_X = 7;
    public static final int SUPPRESSED_HEART_PIXEL_Y = 0;

    private MapSimulationHud() {}

    public static boolean shouldSuppressHeartPixel(boolean simulationEnabled, int textureX, int textureY) {
        return simulationEnabled
                && textureX == SUPPRESSED_HEART_PIXEL_X
                && textureY == SUPPRESSED_HEART_PIXEL_Y;
    }
}
