package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Renders a temporary, client-side outline while the player is choosing the
 * second corner of a Host or Viewer item-frame area.
 */
public final class SelectionHighlightRenderer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(SelectionHighlightRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof ItemFrameEntity currentFrame)) return;

        renderRole(context, BlackjackConfig.Role.HOST, currentFrame, 0.2f, 1.0f, 0.2f);
        renderRole(context, BlackjackConfig.Role.VIEWER, currentFrame, 0.2f, 0.6f, 1.0f);
    }

    private static void renderRole(WorldRenderContext context, BlackjackConfig.Role role,
                                    ItemFrameEntity currentFrame, float red, float green, float blue) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        if (selection.firstKey() == null || selection.secondKey() != null) return;

        UUID firstUuid = parseUuid(selection.firstKey());
        if (firstUuid == null) return;
        Entity entity = currentFrame.getWorld().getEntity(firstUuid);
        if (!(entity instanceof ItemFrameEntity firstFrame) || firstFrame.isRemoved()) return;

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

        Vec3d camera = context.worldState().cameraRenderState.pos;
        context.matrices().push();
        context.matrices().translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer vertices = context.consumers().getBuffer(RenderLayer.getLines());
        WorldRenderer.drawBox(context.matrices(), vertices, box, red, green, blue, 1.0f);

        context.matrices().pop();
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
