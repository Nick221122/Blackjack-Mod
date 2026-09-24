package com.blackjackcalculator;

import net.minecraft.entity.decoration.ItemFrameEntity;

public final class MapSimulationRenderContext {
    private static final ThreadLocal<ItemFrameEntity> CURRENT_FRAME = new ThreadLocal<>();

    private MapSimulationRenderContext() {}

    public static void set(ItemFrameEntity frame) {
        CURRENT_FRAME.set(frame);
    }

    public static ItemFrameEntity currentFrame() {
        return CURRENT_FRAME.get();
    }

    public static void clear() {
        CURRENT_FRAME.remove();
    }
}
