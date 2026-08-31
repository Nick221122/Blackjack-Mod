package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Map;

/** Small client-only paper/card scan screen for reviewing the two independent assignments. */
public final class BlackjackScanScreen extends Screen {
    public BlackjackScanScreen() { super(Text.translatable("screen.blackjackcalculator.scan")); }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.switch"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            BlackjackConfig.Role next = BlackjackCalculatorClient.getActiveRole() == BlackjackConfig.Role.HOST
                    ? BlackjackConfig.Role.VIEWER : BlackjackConfig.Role.HOST;
            BlackjackConfig.setActiveRole(next);
            BlackjackConfig.save(client);
        }).dimensions(cx - 55, height - 55, 110, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        int cx = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, cx, 20, 0xFFFFFFFF);
        drawRole(context, "Host", BlackjackConfig.Role.HOST, cx - 110, 55, client);
        drawRole(context, "Viewer", BlackjackConfig.Role.VIEWER, cx + 20, 55, client);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.blackjackcalculator.active", BlackjackCalculatorClient.getActiveRole() == BlackjackConfig.Role.HOST ? "Host" : "Viewer"),
                cx, height - 80, 0xFFAAAAAA);
    }

    private void drawRole(DrawContext context, String label, BlackjackConfig.Role role, int x, int y, MinecraftClient client) {
        context.drawTextWithShadow(textRenderer, label, x, y, 0xFFFFFFFF);
        String container = BlackjackConfig.getActiveContainer(role);
        context.drawTextWithShadow(textRenderer,
                container.isEmpty() ? Text.translatable("screen.blackjackcalculator.none") : Text.translatable("screen.blackjackcalculator.container"),
                x, y + 18, 0xFFAAAAAA);
        Map<Integer, Integer> scan = BlackjackConfig.getActiveContainerScan(role);
        int line = y + 36;
        if (scan.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.blackjackcalculator.empty"), x, line, 0xFF777777);
            return;
        }
        for (var entry : scan.entrySet()) {
            context.drawTextWithShadow(textRenderer, "Slot " + entry.getKey() + ": " + entry.getValue(), x, line, 0xFFFFFFFF);
            line += 13;
            if (line > height - 70) break;
        }
        int total = role == BlackjackConfig.Role.HOST ? BlackjackCalculatorClient.getHostTotal() : BlackjackCalculatorClient.getViewerTotal();
        boolean has = role == BlackjackConfig.Role.HOST ? BlackjackCalculatorClient.hasHostCards() : BlackjackCalculatorClient.hasViewerCards();
        context.drawTextWithShadow(textRenderer, "Score: " + BlackjackScore.display(total, has), x, line + 5, 0xFFFFD700);
    }
}
