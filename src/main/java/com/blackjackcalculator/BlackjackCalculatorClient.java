package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 64.0D;

    private static KeyBinding toggleRoleKey;
    private static KeyBinding assignHostKey;
    private static KeyBinding assignViewerKey;
    private static KeyBinding clearRoleKey;
    private static KeyBinding editHudKey;
    private static BlackjackConfig.Role activeScanRole = BlackjackConfig.Role.HOST;

    private static int hostTotal;
    private static int viewerTotal;
    private static boolean hostHasCards;
    private static boolean viewerHasCards;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlackjackConfig.load(client);
        activeScanRole = BlackjackConfig.getActiveRole();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
        toggleRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.toggle_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));
        assignHostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.assign_host", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        assignViewerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.assign_viewer", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, category));
        clearRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.clear_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, category));

        ClientTickEvents.END_CLIENT_TICK.register(BlackjackCalculatorClient::onClientTick);
        HudElementRegistry.addLast(HUD_ID, BlackjackCalculatorClient::renderHud);
        ScreenEvents.AFTER_INIT.register(BlackjackCalculatorClient::onScreenInit);
    }

    private static void onScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof Generic3x3ContainerScreen) && !(screen instanceof GenericContainerScreen)) return;
        if (!isDispenserOrDropperScreen(screen)) return;
        int buttonX = Math.max(4, (scaledWidth - 176) / 2 - 62);
        int buttonY = Math.max(4, (scaledHeight - 114) / 2 + 20);
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.scan"), button -> scanOpenContainer(client, screen)).dimensions(buttonX, buttonY, 56, 20).build());
    }

    private static boolean isDispenserOrDropperScreen(Screen screen) {
        return screen instanceof Generic3x3ContainerScreen
                || screen.getTitle().getString().equalsIgnoreCase("Dispenser")
                || screen.getTitle().getString().equalsIgnoreCase("Dropper");
    }

    private static void scanOpenContainer(MinecraftClient client, Screen screen) {
        if (client.player == null || client.world == null) return;
        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handled)) return;
        if (!(handled.getScreenHandler() instanceof net.minecraft.screen.Generic3x3ContainerScreenHandler handler)) {
            client.player.sendMessage(Text.literal("This is not a Dispenser or Dropper."), true);
            return;
        }
        BlockPos pos = client.crosshairTarget instanceof BlockHitResult hit ? hit.getBlockPos() : null;
        if (pos == null) {
            client.player.sendMessage(Text.literal("Look at the Dispenser or Dropper while scanning."), true);
            return;
        }

        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) continue;
            String name = stack.getCustomName() == null ? "" : stack.getCustomName().getString().trim();
            if (!(name.matches("[1-9]") || name.equals("10"))) continue;
            int value = Integer.parseInt(name);
            scanned.put(itemSignature(stack), value);
            occupied++;
        }

        String key = BlackjackConfig.containerKey(client.world.getRegistryKey(), pos);
        BlackjackConfig.saveContainerScan(key, activeScanRole, scanned);
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Scanned " + occupied + " card(s) for " + (activeScanRole == BlackjackConfig.Role.HOST ? "Host" : "Viewer") + "."), true);
    }

    private static String itemSignature(ItemStack stack) {
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String name = stack.getCustomName() == null ? "" : stack.getCustomName().getString().trim();
        return itemId + "|" + name;
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleRoleKey.wasPressed()) toggleActiveScanRole(client);
        while (assignHostKey.wasPressed()) assignTargetRole(client, BlackjackConfig.Role.HOST);
        while (assignViewerKey.wasPressed()) assignTargetRole(client, BlackjackConfig.Role.VIEWER);
        while (clearRoleKey.wasPressed()) clearTargetRole(client);
        while (editHudKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());
        updateTotals(client);
    }

    private static void toggleActiveScanRole(MinecraftClient client) {
        activeScanRole = activeScanRole == BlackjackConfig.Role.HOST ? BlackjackConfig.Role.VIEWER : BlackjackConfig.Role.HOST;
        BlackjackConfig.setActiveRole(activeScanRole);
        BlackjackConfig.save(client);
        if (client.player != null) client.player.sendMessage(Text.literal("Active side: " + (activeScanRole == BlackjackConfig.Role.HOST ? "Host" : "Viewer") + "."), true);
    }

    private static void assignTargetRole(MinecraftClient client, BlackjackConfig.Role role) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.literal("Look directly at an item frame first."), true);
            return;
        }
        BlackjackConfig.setRole(frame, client.world.getRegistryKey(), role);
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Item frame assigned to " + (role == BlackjackConfig.Role.HOST ? "Host" : "Viewer") + "."), true);
    }

    private static void clearTargetRole(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.literal("Look directly at an item frame first."), true);
            return;
        }
        BlackjackConfig.clearRole(frame, client.world.getRegistryKey());
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Blackjack frame role cleared."), true);
    }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            hostTotal = viewerTotal = 0;
            hostHasCards = viewerHasCards = false;
            return;
        }
        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(ItemFrameEntity.class, box, frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());
        int hostFixed = 0, hostAces = 0, viewerFixed = 0, viewerAces = 0;
        boolean foundHost = false, foundViewer = false;

        for (ItemFrameEntity frame : frames) {
            BlackjackConfig.Role role = BlackjackConfig.getRole(frame, client.world.getRegistryKey());
            if (role == null) continue;
            ItemStack stack = frame.getHeldItemStack();
            String name = stack.getCustomName() == null ? "" : stack.getCustomName().getString().trim();
            int scannedValue = BlackjackConfig.getScannedValue(role, itemSignature(stack));
            if (scannedValue > 0) name = Integer.toString(scannedValue);
            if (name.equals("1")) {
                if (role == BlackjackConfig.Role.HOST) { foundHost = true; hostAces++; } else { foundViewer = true; viewerAces++; }
                continue;
            }
            if (!name.matches("[2-9]|10")) continue;
            int value = Integer.parseInt(name);
            if (role == BlackjackConfig.Role.HOST) { foundHost = true; hostFixed += value; }
            else { foundViewer = true; viewerFixed += value; }
        }
        hostHasCards = foundHost;
        viewerHasCards = foundViewer;
        hostTotal = BlackjackScore.score(hostFixed, hostAces);
        viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        drawTotal(context, client, "Host: " + BlackjackScore.display(hostTotal, hostHasCards), true);
        drawTotal(context, client, "Viewer: " + BlackjackScore.display(viewerTotal, viewerHasCards), false);
        String active = "Active: " + (activeScanRole == BlackjackConfig.Role.HOST ? "Host" : "Viewer");
        context.drawText(client.textRenderer, active, (client.getWindow().getScaledWidth() - client.textRenderer.getWidth(active)) / 2, 8, 0xFFAAAAAA, true);
    }

    private static void drawTotal(DrawContext context, MinecraftClient client, String text, boolean host) {
        int color = text.contains("BUST") ? 0xFFFF5555 : text.contains("BLACKJACK!") ? 0xFFFFD700 : 0xFFFFFFFF;
        float scale = host ? BlackjackConfig.getHostScale() : BlackjackConfig.getViewerScale();
        int width = Math.round(client.textRenderer.getWidth(text) * scale);
        int x = host ? BlackjackConfig.getHostX() : BlackjackConfig.getViewerX();
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        if (!host && x < 0) x = client.getWindow().getScaledWidth() - width - 8;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, true);
        context.getMatrices().popMatrix();
    }

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
}
