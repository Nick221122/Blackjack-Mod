package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-only blackjack calculator. No server state or packets are used. */
public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double FRAME_SCAN_RADIUS = 64.0D;

    private static KeyBinding toggleRoleKey, assignRoleKey, clearRoleKey, editHudKey, scanScreenKey, configKey;
    private static BlackjackConfig.Role activeScanRole = BlackjackConfig.Role.HOST;
    private static int hostTotal, viewerTotal;
    private static boolean hostHasCards, viewerHasCards;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlackjackConfig.load(client);
        activeScanRole = BlackjackConfig.getActiveRole();
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
        toggleRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.toggle_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));
        assignRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.assign_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        clearRoleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.clear_role", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, category));
        scanScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.scan_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, category));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, category));
        ClientTickEvents.END_CLIENT_TICK.register(BlackjackCalculatorClient::onClientTick);
        HudElementRegistry.addLast(HUD_ID, BlackjackCalculatorClient::renderHud);
        ScreenEvents.AFTER_INIT.register(BlackjackCalculatorClient::onScreenInit);
    }

    private static void onScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof Generic3x3ContainerScreen) || !isOpenedDispenserOrDropper(client)) return;
        int left = (scaledWidth - 176) / 2;
        int top = (scaledHeight - 114) / 2;
        Screens.getWidgets(screen).add(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.scan"), button -> scanOpenContainer(client, screen))
                .dimensions(Math.max(4, left - 62), top + 20, 56, 20).build());
    }

    private static boolean isOpenedDispenserOrDropper(MinecraftClient client) {
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult hit)) return false;
        var state = client.world.getBlockState(hit.getBlockPos());
        return state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER);
    }

    private static void scanOpenContainer(MinecraftClient client, Screen screen) {
        if (client.player == null || client.world == null || !(screen instanceof HandledScreen<?> handled)) return;
        if (!(handled.getScreenHandler() instanceof net.minecraft.screen.Generic3x3ContainerScreenHandler handler)) return;
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) return;
        BlockPos pos = hit.getBlockPos();
        var state = client.world.getBlockState(pos);
        if (!state.isOf(Blocks.DISPENSER) && !state.isOf(Blocks.DROPPER)) return;

        Map<Integer, Integer> scanned = new LinkedHashMap<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            Integer value = stack.isEmpty() ? null : parseCardValue(stack);
            if (value != null) scanned.put(slot + 1, value);
        }
        BlackjackConfig.saveContainerScan(BlackjackConfig.containerKey(client.world.getRegistryKey(), pos), activeScanRole, scanned);
        BlackjackConfig.save(client);
        updateTotals(client);
        client.player.sendMessage(Text.translatable("message.blackjackcalculator.scanned", scanned.size(), activeScanRole == BlackjackConfig.Role.HOST ? BlackjackConfig.getHostName() : BlackjackConfig.getViewerName()), true);
    }

    private static Integer parseCardValue(ItemStack stack) {
        String name = stack.getCustomName() == null ? "" : stack.getCustomName().getString().trim();
        if (!name.matches("(?:[1-9]|10)")) return null;
        int value = Integer.parseInt(name);
        return value >= 1 && value <= 10 ? value : null;
    }

    private static void onClientTick(MinecraftClient client) {
        while (toggleRoleKey.wasPressed()) toggleActiveScanRole(client);
        while (assignRoleKey.wasPressed()) assignTargetRole(client);
        while (clearRoleKey.wasPressed()) clearTargetRole(client);
        while (editHudKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());
        while (scanScreenKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackScanScreen());
        while (configKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackConfigScreen());
        updateTotals(client);
    }

    private static void toggleActiveScanRole(MinecraftClient client) {
        setActiveRole(activeScanRole == BlackjackConfig.Role.HOST ? BlackjackConfig.Role.VIEWER : BlackjackConfig.Role.HOST, client);
    }

    public static void setActiveRole(BlackjackConfig.Role role, MinecraftClient client) {
        activeScanRole = role;
        BlackjackConfig.setActiveRole(role);
        BlackjackConfig.save(client);
        if (client.player != null) client.player.sendMessage(Text.translatable("message.blackjackcalculator.active", role == BlackjackConfig.Role.HOST ? BlackjackConfig.getHostName() : BlackjackConfig.getViewerName()), true);
    }

    private static void assignTargetRole(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.translatable("message.blackjackcalculator.look_frame"), true); return;
        }
        BlackjackConfig.setRole(frame, client.world.getRegistryKey(), activeScanRole);
        BlackjackConfig.save(client);
    }

    private static void clearTargetRole(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.translatable("message.blackjackcalculator.look_frame"), true); return;
        }
        BlackjackConfig.clearRole(frame, client.world.getRegistryKey());
        BlackjackConfig.save(client);
    }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) { hostTotal = viewerTotal = 0; hostHasCards = viewerHasCards = false; return; }
        int[] host = totalsFromContainer(BlackjackConfig.getActiveContainerScan(BlackjackConfig.Role.HOST));
        int[] viewer = totalsFromContainer(BlackjackConfig.getActiveContainerScan(BlackjackConfig.Role.VIEWER));
        boolean hostFromContainer = !BlackjackConfig.getActiveContainer(BlackjackConfig.Role.HOST).isEmpty();
        boolean viewerFromContainer = !BlackjackConfig.getActiveContainer(BlackjackConfig.Role.VIEWER).isEmpty();

        if (!hostFromContainer || !viewerFromContainer) {
            Box box = client.player.getBoundingBox().expand(FRAME_SCAN_RADIUS);
            List<ItemFrameEntity> frames = client.world.getEntitiesByClass(ItemFrameEntity.class, box, frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());
            int hostFixed = 0, hostAces = 0, viewerFixed = 0, viewerAces = 0;
            boolean foundHost = hostFromContainer, foundViewer = viewerFromContainer;
            for (ItemFrameEntity frame : frames) {
                BlackjackConfig.Role role = BlackjackConfig.getRole(frame, client.world.getRegistryKey());
                if (role == null || (role == BlackjackConfig.Role.HOST && hostFromContainer) || (role == BlackjackConfig.Role.VIEWER && viewerFromContainer)) continue;
                Integer value = parseCardValue(frame.getHeldItemStack());
                if (value == null) continue;
                if (role == BlackjackConfig.Role.HOST) { foundHost = true; if (value == 1) hostAces++; else hostFixed += value; }
                else { foundViewer = true; if (value == 1) viewerAces++; else viewerFixed += value; }
            }
            if (!hostFromContainer) { host[0] = hostFixed; host[1] = hostAces; host[2] = foundHost ? 1 : 0; }
            if (!viewerFromContainer) { viewer[0] = viewerFixed; viewer[1] = viewerAces; viewer[2] = foundViewer ? 1 : 0; }
        }
        hostHasCards = host[2] != 0; viewerHasCards = viewer[2] != 0;
        hostTotal = BlackjackScore.score(host[0], host[1]); viewerTotal = BlackjackScore.score(viewer[0], viewer[1]);
    }

    private static int[] totalsFromContainer(Map<Integer, Integer> values) {
        int fixed = 0, aces = 0;
        for (int value : values.values()) { if (value == 1) aces++; else fixed += value; }
        return new int[]{fixed, aces, values.isEmpty() ? 0 : 1};
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        drawTotal(context, client, BlackjackConfig.getHostName() + ": " + BlackjackScore.display(hostTotal, hostHasCards), true);
        drawTotal(context, client, BlackjackConfig.getViewerName() + ": " + BlackjackScore.display(viewerTotal, viewerHasCards), false);
        String active = "Active: " + (activeScanRole == BlackjackConfig.Role.HOST ? BlackjackConfig.getHostName() : BlackjackConfig.getViewerName());
        context.drawText(client.textRenderer, active, (client.getWindow().getScaledWidth() - client.textRenderer.getWidth(active)) / 2, 8, 0xFFAAAAAA, true);
    }

    private static void drawTotal(DrawContext context, MinecraftClient client, String text, boolean host) {
        String display = text.toLowerCase();
        int color = display.contains("bust") ? 0xFFFF5555 : display.contains("blackjack") ? 0xFFFFD700 : 0xFFFFFFFF;
        float scale = host ? BlackjackConfig.getHostScale() : BlackjackConfig.getViewerScale();
        int x = host ? BlackjackConfig.getHostX() : BlackjackConfig.getViewerX();
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        int width = Math.round(client.textRenderer.getWidth(text) * scale);
        if (!host && x < 0) x = client.getWindow().getScaledWidth() - width - 8;
        context.getMatrices().pushMatrix(); context.getMatrices().translate(x, y); context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, true); context.getMatrices().popMatrix();
    }

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
    public static BlackjackConfig.Role getActiveRole() { return activeScanRole; }
}
