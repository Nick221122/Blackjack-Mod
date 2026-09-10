package com.blackjackcalculator;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** HUD editor for independently positioning and naming Host and Viewer. */
public final class BlackjackHudEditorScreen extends Screen {
    private enum DragTarget { NONE, HOST, VIEWER }

    private DragTarget dragging = DragTarget.NONE;
    private int dragOffsetX;
    private int dragOffsetY;
    private EditBox hostNameField;
    private EditBox viewerNameField;

    public BlackjackHudEditorScreen() {
        super(Component.literal("Blackjack HUD Editor"));
    }

    @Override
    protected void init() {
        int panelX = 12;
        hostNameField = new EditBox(font, panelX, 42, 130, 20, Component.literal("Host name"));
        hostNameField.setMaxLength(32);
        hostNameField.setValue(BlackjackConfig.getHostName());
        hostNameField.setHint(Component.literal("blank = numbers only"));
        addRenderableWidget(hostNameField);

        viewerNameField = new EditBox(font, panelX, 92, 130, 20, Component.literal("Viewer name"));
        viewerNameField.setMaxLength(32);
        viewerNameField.setValue(BlackjackConfig.getViewerName());
        viewerNameField.setHint(Component.literal("blank = numbers only"));
        addRenderableWidget(viewerNameField);

        addRenderableWidget(Button.builder(Component.literal("Save names"), button -> saveNames()).bounds(panelX, 120, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset positions"), button -> BlackjackConfig.resetPositions()).bounds(panelX, 145, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose()).bounds(panelX, 170, 130, 20).build());
    }

    private void saveNames() {
        BlackjackConfig.setHostName(hostNameField.getValue());
        BlackjackConfig.setViewerName(viewerNameField.getValue());
        BlackjackConfig.save(Minecraft.getInstance());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, width, height, 0x88000000);
        drawGrid(graphics);
        graphics.text(font, Component.literal("HUD Grid Editor"), 12, 18, 0xFFFFFFFF, true);
        graphics.text(font, Component.literal("Host name"), 12, 30, 0xFFCCCCCC, true);
        graphics.text(font, Component.literal("Viewer name"), 12, 80, 0xFFCCCCCC, true);
        graphics.text(font, Component.literal("Drag either preview. R = reset. Esc = save & close."), 160, 18, 0xFFFFFFFF, true);

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String host = formatPreview(BlackjackConfig.getHostName(), BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards()), true);
            String viewer = formatPreview(BlackjackConfig.getViewerName(), BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards()), false);
            drawPreview(graphics, client, host, true);
            drawPreview(graphics, client, viewer, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    private String formatPreview(String name, String score, boolean host) {
        if (name.isBlank()) return score;
        return host ? name + " " + score : score + " " + name;
    }

    private void drawGrid(GuiGraphicsExtractor graphics) {
        int spacing = 20;
        for (int x = 0; x < width; x += spacing) graphics.fill(x, 0, x + 1, height, 0x33222222);
        for (int y = 0; y < height; y += spacing) graphics.fill(0, y, width, y + 1, 0x33222222);
    }

    private void drawPreview(GuiGraphicsExtractor graphics, Minecraft client, String text, boolean host) {
        int x = host ? BlackjackConfig.getHostX() : resolvedViewerX(client, text);
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        int textWidth = client.font.width(text);
        graphics.fill(x - 4, y - 4, x + textWidth + 4, y + 14, 0x66000000);
        graphics.text(client.font, Component.literal(text), x, y, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubled);
        Minecraft client = Minecraft.getInstance();
        String host = formatPreview(BlackjackConfig.getHostName(), BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards()), true);
        String viewer = formatPreview(BlackjackConfig.getViewerName(), BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards()), false);

        if (inside(event.x(), event.y(), BlackjackConfig.getHostX(), BlackjackConfig.getHostY(), client.font.width(host))) {
            dragging = DragTarget.HOST;
            dragOffsetX = (int) event.x() - BlackjackConfig.getHostX();
            dragOffsetY = (int) event.y() - BlackjackConfig.getHostY();
            return true;
        }
        int viewerX = resolvedViewerX(client, viewer);
        if (inside(event.x(), event.y(), viewerX, BlackjackConfig.getViewerY(), client.font.width(viewer))) {
            dragging = DragTarget.VIEWER;
            dragOffsetX = (int) event.x() - viewerX;
            dragOffsetY = (int) event.y() - BlackjackConfig.getViewerY();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || dragging == DragTarget.NONE) return false;
        int x = clamp((int) event.x() - dragOffsetX, 2, width - 2);
        int y = clamp((int) event.y() - dragOffsetY, 2, height - 14);
        if (dragging == DragTarget.HOST) BlackjackConfig.setHostPosition(x, y);
        else BlackjackConfig.setViewerPosition(x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = DragTarget.NONE;
            saveNames();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_R) {
            BlackjackConfig.resetPositions();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        saveNames();
        Minecraft.getInstance().gui.setScreen(null);
    }

    private int resolvedViewerX(Minecraft client, String text) {
        int configured = BlackjackConfig.getViewerX();
        if (configured >= 0) return configured;
        return client.getWindow().getGuiScaledWidth() - client.font.width(text) - 8;
    }

    private static boolean inside(double mx, double my, int x, int y, int width) {
        return mx >= x - 5 && mx <= x + width + 5 && my >= y - 5 && my <= y + 15;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
