package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;

/** Temporary full-area preview while choosing the second corner of a selection. */
public final class SelectionHighlightRenderer implements ClientModInitializer {
    private static final int HOST_STROKE = 0xFF33FF66;
    private static final int HOST_FILL = 0x3033FF66;
    private static final int VIEWER_STROKE = 0xFF33AAFF;
    private static final int VIEWER_FILL = 0x3033AAFF;

    @Override
    public void onInitializeClient() {
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> render());
    }

    private static void render() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        ItemFrame currentFrame = BlackjackCalculatorClient.findTargetFrame(client);
        if (currentFrame == null) return;
        renderRole(client, BlackjackConfig.Role.HOST, currentFrame, HOST_STROKE, HOST_FILL);
        renderRole(client, BlackjackConfig.Role.VIEWER, currentFrame, VIEWER_STROKE, VIEWER_FILL);
    }

    private static void renderRole(Minecraft client, BlackjackConfig.Role role, ItemFrame currentFrame, int stroke, int fill) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        if (selection.firstKey() == null || selection.secondKey() != null) return;
        BlockPos a = BlackjackConfig.parseSelectionPos(selection.firstKey());
        if (a == null) return;
        BlockPos b = currentFrame.blockPosition();
        AABB box = new AABB(
                Math.min(a.getX(), b.getX()) - 0.05D,
                Math.min(a.getY(), b.getY()) - 0.05D,
                Math.min(a.getZ(), b.getZ()) - 0.05D,
                Math.max(a.getX(), b.getX()) + 1.05D,
                Math.max(a.getY(), b.getY()) + 1.05D,
                Math.max(a.getZ(), b.getZ()) + 1.05D
        );
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(stroke, 3.0F, fill)).setAlwaysOnTop();
    }
}
