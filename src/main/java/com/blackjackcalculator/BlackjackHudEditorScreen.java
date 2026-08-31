package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Simple mouse-drag editor for the two HUD totals. */
public final class BlackjackHudEditorScreen extends Screen {
    private enum DragTarget { NONE, HOST, VIEWER }

    private DragTarget dragging = DragTarget.NONE;
    private int dragOffsetX;
    private int dragOffsetY;

    public BlackjackHudEditorScreen() {
        super(Text.literal("Blackjack HUD Position Editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0x88000000);
        context.drawCenteredTextWithShadow(textRenderer,
                "Drag Host and Viewer to move them. Press R to reset. Esc to save.",
                width / 2, 18, 0xFFFFFFFF);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String host = "Host: " + BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards());
            String viewer = "Viewer: " + BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards());
            drawPreview(context, client, host, true);
            drawPreview(context, client, viewer, false);
        }
    }

    private void drawPreview(DrawContext context, MinecraftClient client, String text, boolean host) {
        int x = host ? BlackjackConfig.getHostX() : resolvedViewerX(client, text);
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        int width = client.textRenderer.getWidth(text);
        context.fill(x - 3, y - 3, x + width + 3, y + 12, 0x66000000);
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_1) return super.mouseClicked(click, doubled);
        MinecraftClient client = MinecraftClient.getInstance();
        String host = "Host: " + BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards());
        String viewer = "Viewer: " + BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards());

        if (inside(click.x(), click.y(), BlackjackConfig.getHostX(), BlackjackConfig.getHostY(), client.textRenderer.getWidth(host))) {
            dragging = DragTarget.HOST;
            dragOffsetX = (int) click.x() - BlackjackConfig.getHostX();
            dragOffsetY = (int) click.y() - BlackjackConfig.getHostY();
            return true;
        }
        int viewerX = resolvedViewerX(client, viewer);
        if (inside(click.x(), click.y(), viewerX, BlackjackConfig.getViewerY(), client.textRenderer.getWidth(viewer))) {
            dragging = DragTarget.VIEWER;
            dragOffsetX = (int) click.x() - viewerX;
            dragOffsetY = (int) click.y() - BlackjackConfig.getViewerY();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_1 || dragging == DragTarget.NONE) return false;
        int x = clamp((int) click.x() - dragOffsetX, 2, width - 2);
        int y = clamp((int) click.y() - dragOffsetY, 2, height - 14);
        if (dragging == DragTarget.HOST) BlackjackConfig.setHostPosition(x, y);
        else BlackjackConfig.setViewerPosition(x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_1) {
            dragging = DragTarget.NONE;
            BlackjackConfig.save(MinecraftClient.getInstance());
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == GLFW.GLFW_KEY_R) {
            BlackjackConfig.resetPositions();
            return true;
        }
        if (input.getKeycode() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        BlackjackConfig.save(MinecraftClient.getInstance());
        client.setScreen(null);
    }

    private int resolvedViewerX(MinecraftClient client, String text) {
        int configured = BlackjackConfig.getViewerX();
        if (configured >= 0) return configured;
        return client.getWindow().getScaledWidth() - client.textRenderer.getWidth(text) - 8;
    }

    private static boolean inside(double mx, double my, int x, int y, int width) {
        return mx >= x - 5 && mx <= x + width + 5 && my >= y - 5 && my <= y + 15;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
