package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Client-side blackjack calculator.
 *
 * Cards are identified only by their custom name (1-10). The role of each item frame
 * is assigned locally with keybinds, so Host and Viewer cards can come from one dispenser.
 */
public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 64.0D;

    private static KeyBinding toggleRoleKey;
    private static KeyBinding clearRoleKey;
    private static KeyBinding editHudKey;

    private static int hostTotal;
    private static int viewerTotal;
    private static boolean hostHasCards;
    private static boolean viewerHasCards;

    @Override
    public void onInitializeClient() {
        BlackjackConfig.load(MinecraftClient.getInstance());

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
        toggleRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blackjackcalculator.toggle_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));
        clearRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blackjackcalculator.clear_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blackjackcalculator.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, category));

        ClientTickEvents.END_CLIENT_TICK.register(BlackjackCalculatorClient::onClientTick);
        HudElementRegistry.addLast(HUD_ID, BlackjackCalculatorClient::renderHud);
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleRoleKey.wasPressed()) toggleTargetRole(client);
        while (clearRoleKey.wasPressed()) assignTargetRole(client, null);
        while (editHudKey.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());
        }
        updateTotals(client);
    }

    private static void toggleTargetRole(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.literal("Look directly at an item frame first."), true);
            return;
        }

        BlackjackConfig.Role current = BlackjackConfig.getRole(frame, client.world.getRegistryKey());
        BlackjackConfig.Role next = current == null || current == BlackjackConfig.Role.VIEWER
                ? BlackjackConfig.Role.HOST
                : BlackjackConfig.Role.VIEWER;
        BlackjackConfig.setRole(frame, client.world.getRegistryKey(), next);
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Frame set to " + (next == BlackjackConfig.Role.HOST ? "Host" : "Viewer") + "."), true);
    }

    private static void assignTargetRole(MinecraftClient client, BlackjackConfig.Role role) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            client.player.sendMessage(Text.literal("Look directly at an item frame first."), true);
            return;
        }
        Entity entity = hit.getEntity();
        if (!(entity instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.literal("Look directly at an item frame first."), true);
            return;
        }

        if (role == null) {
            BlackjackConfig.clearRole(frame, client.world.getRegistryKey());
            client.player.sendMessage(Text.literal("Blackjack frame role cleared."), true);
        } else {
            BlackjackConfig.setRole(frame, client.world.getRegistryKey(), role);
            BlackjackConfig.save(client);
            client.player.sendMessage(Text.literal("Frame set to " + (role == BlackjackConfig.Role.HOST ? "Host" : "Viewer") + "."), true);
        }
    }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            hostTotal = viewerTotal = 0;
            hostHasCards = viewerHasCards = false;
            return;
        }

        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(
                ItemFrameEntity.class, box,
                frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());

        int hostFixed = 0;
        int hostAces = 0;
        int viewerFixed = 0;
        int viewerAces = 0;
        boolean foundHost = false;
        boolean foundViewer = false;

        for (ItemFrameEntity frame : frames) {
            BlackjackConfig.Role role = BlackjackConfig.getRole(frame, client.world.getRegistryKey());
            if (role == null) continue;

            ItemStack stack = frame.getHeldItemStack();
            String name = stack.getCustomName() == null ? "" : stack.getCustomName().getString().trim();
            if (name.equals("1")) {
                if (role == BlackjackConfig.Role.HOST) { foundHost = true; hostAces++; }
                else { foundViewer = true; viewerAces++; }
                continue;
            }
            if (!name.matches("[2-9]|10")) continue;
            int value = Integer.parseInt(name);
            if (role == BlackjackConfig.Role.HOST) {
                foundHost = true;
                if (value == 1) hostAces++; else hostFixed += value;
            } else {
                foundViewer = true;
                if (value == 1) viewerAces++; else viewerFixed += value;
            }
        }

        hostHasCards = foundHost;
        viewerHasCards = foundViewer;
        hostTotal = BlackjackScore.score(hostFixed, hostAces);
        viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;

        drawTotal(context, client, "Host: " + BlackjackScore.display(hostTotal, hostHasCards), true, hostTotal);
        drawTotal(context, client, "Viewer: " + BlackjackScore.display(viewerTotal, viewerHasCards), false, viewerTotal);
    }

    private static void drawTotal(DrawContext context, MinecraftClient client, String text, boolean host, int total) {
        int color;
        if (text.contains("BUST")) color = 0xFFFF5555;
        else if (text.contains("BLACKJACK!")) color = 0xFFFFD700;
        else if (text.endsWith("-")) color = 0xFFAAAAAA;
        else color = 0xFFFFFFFF;

        int width = client.textRenderer.getWidth(text);
        int x = host ? BlackjackConfig.getHostX() : BlackjackConfig.getViewerX();
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        if (!host && x < 0) x = client.getWindow().getScaledWidth() - width - 8;
        if (!host) x = Math.max(0, Math.min(client.getWindow().getScaledWidth() - width, x));
        context.drawText(client.textRenderer, text, x, y, color, true);
    }

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
}
