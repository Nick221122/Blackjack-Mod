package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

/** Temporary full-area preview while choosing the second corner of a selection. */
public final class SelectionHighlightRenderer implements ClientModInitializer {
    private static final int HOST_STROKE = 0xFF33FF66;
    private static final int HOST_FILL = 0x3033FF66;
    private static final int VIEWER_STROKE = 0xFF33AAFF;
    private static final int VIEWER_FILL = 0x3033AAFF;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(SelectionHighlightRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof ItemFrameEntity currentFrame)) return;
        renderRole(client, BlackjackConfig.Role.HOST, currentFrame, HOST_STROKE, HOST_FILL);
        renderRole(client, BlackjackConfig.Role.VIEWER, currentFrame, VIEWER_STROKE, VIEWER_FILL);
    }

    private static void renderRole(MinecraftClient client, BlackjackConfig.Role role, ItemFrameEntity currentFrame, int stroke, int fill) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        if (selection.firstKey() == null || selection.secondKey() != null) return;
        BlockPos a = BlackjackConfig.parseSelectionPos(selection.firstKey());
        if (a == null) return;
        BlockPos b = currentFrame.getBlockPos();
        Box box = new Box(Math.min(a.getX(), b.getX()) - 0.05D, Math.min(a.getY(), b.getY()) - 0.05D, Math.min(a.getZ(), b.getZ()) - 0.05D,
                Math.max(a.getX(), b.getX()) + 1.05D, Math.max(a.getY(), b.getY()) + 1.05D, Math.max(a.getZ(), b.getZ()) + 1.05D);
        try (var scope = client.newGizmoScope()) {
            GizmoDrawing.box(box, DrawStyle.filledAndStroked(stroke, 3.0F, fill)).withLifespan(1).ignoreOcclusion();
        }
    }
}
