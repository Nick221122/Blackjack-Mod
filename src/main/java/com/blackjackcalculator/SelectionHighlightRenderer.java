package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.UUID;

/**
 * Shows a temporary world-space outline while the player is choosing the
 * second corner of a Host or Viewer item-frame area.
 *
 * The outline is only a preview. Once the second corner is selected,
 * FrameSelection.secondKey() becomes non-null and the preview immediately
 * stops being emitted.
 */
public final class SelectionHighlightRenderer implements ClientModInitializer {
    private static final int HOST_COLOR = 0xFF33FF66;
    private static final int VIEWER_COLOR = 0xFF33AAFF;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(SelectionHighlightRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof ItemFrameEntity currentFrame)) {
            return;
        }

        renderRole(client, BlackjackConfig.Role.HOST, currentFrame, HOST_COLOR);
        renderRole(client, BlackjackConfig.Role.VIEWER, currentFrame, VIEWER_COLOR);
    }

    private static void renderRole(MinecraftClient client,
                                   BlackjackConfig.Role role,
                                   ItemFrameEntity currentFrame,
                                   int color) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);

        // A preview exists only after the first corner and before the second.
        if (selection.firstKey() == null || selection.secondKey() != null) {
            return;
        }

        UUID firstUuid = parseUuid(selection.firstKey());
        if (firstUuid == null) {
            return;
        }

        Entity entity = client.world.getEntity(firstUuid);
        if (!(entity instanceof ItemFrameEntity firstFrame) || firstFrame.isRemoved()) {
            return;
        }

        BlockPos a = firstFrame.getBlockPos();
        BlockPos b = currentFrame.getBlockPos();
        Box box = new Box(
                Math.min(a.getX(), b.getX()) - 0.08D,
                Math.min(a.getY(), b.getY()) - 0.08D,
                Math.min(a.getZ(), b.getZ()) - 0.08D,
                Math.max(a.getX(), b.getX()) + 1.08D,
                Math.max(a.getY(), b.getY()) + 1.08D,
                Math.max(a.getZ(), b.getZ()) + 1.08D
        );

        // Minecraft 1.21.11 uses the Gizmo rendering system for debug-style
        // world outlines. Emit the preview for one tick at a time so it is
        // refreshed only while selection is active and disappears immediately
        // after the second corner is saved.
        try (var scope = client.newGizmoScope()) {
            GizmoDrawing.box(box, DrawStyle.stroked(color, 3.0F))
                    .withLifespan(1)
                    .ignoreOcclusion();
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
