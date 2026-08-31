package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

/** Client-only names/configuration screen. */
public final class BlackjackConfigScreen extends Screen {
    private EditBoxWidget hostName;
    private EditBoxWidget viewerName;

    public BlackjackConfigScreen() { super(Text.translatable("screen.blackjackcalculator.config")); }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        hostName = new EditBoxWidget(textRenderer, cx - 110, 55, 220, 20, Text.translatable("screen.blackjackcalculator.host_name"));
        hostName.setMaxLength(24); hostName.setValue(BlackjackConfig.getHostName()); addDrawableChild(hostName);
        viewerName = new EditBoxWidget(textRenderer, cx - 110, 105, 220, 20, Text.translatable("screen.blackjackcalculator.viewer_name"));
        viewerName.setMaxLength(24); viewerName.setValue(BlackjackConfig.getViewerName()); addDrawableChild(viewerName);
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.save"), b -> saveAndClose()).dimensions(cx - 55, 145, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.hud"), b -> { save(); client.setScreen(new BlackjackHudEditorScreen()); }).dimensions(cx - 55, 172, 110, 20).build());
    }

    private void save() {
        BlackjackConfig.setHostName(hostName.getValue());
        BlackjackConfig.setViewerName(viewerName.getValue());
        BlackjackConfig.save(MinecraftClient.getInstance());
    }
    private void saveAndClose() { save(); close(); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 25, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blackjackcalculator.host_name"), width / 2 - 110, 43, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.blackjackcalculator.viewer_name"), width / 2 - 110, 93, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() { save(); MinecraftClient.getInstance().setScreen(null); }
}
