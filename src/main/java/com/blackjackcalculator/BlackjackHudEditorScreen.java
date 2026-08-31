package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Drag-and-scale editor for the two HUD entries. */
public final class BlackjackHudEditorScreen extends Screen {
    private enum DragTarget { NONE, HOST, VIEWER }
    private DragTarget dragging = DragTarget.NONE;
    private int dragOffsetX;
    private int dragOffsetY;

    public BlackjackHudEditorScreen() { super(Text.translatable("screen.blackjackcalculator.hud_editor")); }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.host_smaller"), b -> BlackjackConfig.setHostScale(BlackjackConfig.getHostScale() - .1f)).dimensions(8, height - 28, 88, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.host_larger"), b -> BlackjackConfig.setHostScale(BlackjackConfig.getHostScale() + .1f)).dimensions(100, height - 28, 88, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.viewer_smaller"), b -> BlackjackConfig.setViewerScale(BlackjackConfig.getViewerScale() - .1f)).dimensions(width - 188, height - 28, 88, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.viewer_larger"), b -> BlackjackConfig.setViewerScale(BlackjackConfig.getViewerScale() + .1f)).dimensions(width - 96, height - 28, 88, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.blackjackcalculator.hud_editor.help"), width / 2, 18, 0xFFFFFFFF);
        if (client.player != null) {
            drawPreview(context, client, "Host: " + BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards()), true);
            drawPreview(context, client, "Viewer: " + BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards()), false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPreview(DrawContext context, MinecraftClient client, String text, boolean host) {
        float scale = host ? BlackjackConfig.getHostScale() : BlackjackConfig.getViewerScale();
        int x = host ? BlackjackConfig.getHostX() : resolvedViewerX(client, text, scale);
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        int textWidth = Math.round(client.textRenderer.getWidth(text) * scale);
        context.fill(x - 4, y - 4, x + textWidth + 4, y + Math.round(14 * scale), 0x66000000);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_1) return super.mouseClicked(click, doubled);
        MinecraftClient client = MinecraftClient.getInstance();
        String host = "Host: " + BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards());
        String viewer = "Viewer: " + BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards());
        if (inside(click.x(), click.y(), BlackjackConfig.getHostX(), BlackjackConfig.getHostY(), client.textRenderer.getWidth(host) * BlackjackConfig.getHostScale())) {
            dragging = DragTarget.HOST;
            dragOffsetX = (int) click.x() - BlackjackConfig.getHostX();
            dragOffsetY = (int) click.y() - BlackjackConfig.getHostY();
            return true;
        }
        int viewerX = resolvedViewerX(client, viewer, BlackjackConfig.getViewerScale());
        if (inside(click.x(), click.y(), viewerX, BlackjackConfig.getViewerY(), client.textRenderer.getWidth(viewer) * BlackjackConfig.getViewerScale())) {
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
        int y = clamp((int) click.y() - dragOffsetY, 2, height - 20);
        if (dragging == DragTarget.HOST) BlackjackConfig.setHostPosition(x, y); else BlackjackConfig.setViewerPosition(x, y);
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
        if (input.getKeycode() == GLFW.GLFW_KEY_R) { BlackjackConfig.resetPositions(); return true; }
        if (input.getKeycode() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        BlackjackConfig.save(MinecraftClient.getInstance());
        MinecraftClient.getInstance().setScreen(null);
    }

    private int resolvedViewerX(MinecraftClient client, String text, float scale) {
        int configured = BlackjackConfig.getViewerX();
        if (configured >= 0) return configured;
        return client.getWindow().getScaledWidth() - Math.round(client.textRenderer.getWidth(text) * scale) - 8;
    }

    private static boolean inside(double mx, double my, int x, int y, double w) {
        return mx >= x - 6 && mx <= x + w + 6 && my >= y - 6 && my <= y + 18;
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
