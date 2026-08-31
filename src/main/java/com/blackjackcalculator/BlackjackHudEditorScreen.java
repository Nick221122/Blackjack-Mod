package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** HUD editor for independently positioning and naming Host and Viewer. */
public final class BlackjackHudEditorScreen extends Screen {
    private enum DragTarget { NONE, HOST, VIEWER }

    private DragTarget dragging = DragTarget.NONE;
    private int dragOffsetX;
    private int dragOffsetY;
    private TextFieldWidget hostNameField;
    private TextFieldWidget viewerNameField;

    public BlackjackHudEditorScreen() {
        super(Text.literal("Blackjack HUD Editor"));
    }

    @Override
    protected void init() {
        int panelX = 12;
        hostNameField = new TextFieldWidget(textRenderer, panelX, 42, 130, 20, Text.literal("Host name"));
        hostNameField.setMaxLength(32);
        hostNameField.setText(BlackjackConfig.getHostName());
        hostNameField.setPlaceholder(Text.literal("blank = numbers only"));
        addDrawableChild(hostNameField);

        viewerNameField = new TextFieldWidget(textRenderer, panelX, 92, 130, 20, Text.literal("Viewer name"));
        viewerNameField.setMaxLength(32);
        viewerNameField.setText(BlackjackConfig.getViewerName());
        viewerNameField.setPlaceholder(Text.literal("blank = numbers only"));
        addDrawableChild(viewerNameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save names"), button -> saveNames()).dimensions(panelX, 120, 130, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset positions"), button -> BlackjackConfig.resetPositions()).dimensions(panelX, 145, 130, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(panelX, 170, 130, 20).build());
    }

    private void saveNames() {
        BlackjackConfig.setHostName(hostNameField.getText());
        BlackjackConfig.setViewerName(viewerNameField.getText());
        BlackjackConfig.save(MinecraftClient.getInstance());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0x88000000);
        drawGrid(context);
        context.drawTextWithShadow(textRenderer, "HUD Grid Editor", 12, 18, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Host name", 12, 30, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, "Viewer name", 12, 80, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer, "Drag either preview. R = reset. Esc = save & close.", 160, 18, 0xFFFFFFFF);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String host = formatPreview(BlackjackConfig.getHostName(), BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards()), true);
            String viewer = formatPreview(BlackjackConfig.getViewerName(), BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards()), false);
            drawPreview(context, client, host, true);
            drawPreview(context, client, viewer, false);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private String formatPreview(String name, String score, boolean host) {
        if (name.isBlank()) return score;
        return host ? name + " " + score : score + " " + name;
    }

    private void drawGrid(DrawContext context) {
        int spacing = 20;
        for (int x = 0; x < width; x += spacing) context.drawVerticalLine(x, 0, height, 0x33222222);
        for (int y = 0; y < height; y += spacing) context.drawHorizontalLine(0, width, y, 0x33222222);
    }

    private void drawPreview(DrawContext context, MinecraftClient client, String text, boolean host) {
        int x = host ? BlackjackConfig.getHostX() : resolvedViewerX(client, text);
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        int textWidth = client.textRenderer.getWidth(text);
        context.fill(x - 4, y - 4, x + textWidth + 4, y + 14, 0x66000000);
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_1) return super.mouseClicked(click, doubled);
        MinecraftClient client = MinecraftClient.getInstance();
        String host = formatPreview(BlackjackConfig.getHostName(), BlackjackScore.display(BlackjackCalculatorClient.getHostTotal(), BlackjackCalculatorClient.hasHostCards()), true);
        String viewer = formatPreview(BlackjackConfig.getViewerName(), BlackjackScore.display(BlackjackCalculatorClient.getViewerTotal(), BlackjackCalculatorClient.hasViewerCards()), false);

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
            saveNames();
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
        saveNames();
        MinecraftClient.getInstance().setScreen(null);
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
