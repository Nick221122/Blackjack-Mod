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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 64.0D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding assignHostKey;
    private static KeyBinding assignViewerKey;
    private static KeyBinding editHudKey;

    private static int hostTotal;
    private static int viewerTotal;
    private static boolean hostHasCards;
    private static boolean viewerHasCards;

    private static final Map<UUID, Boolean> selectionHighlights = new HashMap<>();

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlackjackConfig.load(client);
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
        assignHostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.assign_host", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        assignViewerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.assign_viewer", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, category));
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
        if (client.player == null) return;
        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handled)) return;
        if (!(handled.getScreenHandler() instanceof net.minecraft.screen.Generic3x3ContainerScreenHandler handler)) {
            client.player.sendMessage(Text.literal("This is not a Dispenser or Dropper."), true);
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult)) {
            client.player.sendMessage(Text.literal("Look at the Dispenser or Dropper while scanning."), true);
            return;
        }

        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) continue;
            String mapId = mapIdSignature(stack);
            if (mapId == null) continue;
            scanned.put(mapId, SCAN_VALUES[slot]);
            occupied++;
        }
        BlackjackConfig.replaceSharedScan(scanned);
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Scanned " + occupied + " map(s). Shared by Host and Viewer."), true);
    }

    private static String mapIdSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var mapId = stack.get(DataComponentTypes.MAP_ID);
        return mapId == null ? null : mapId.toString();
    }

    private static void onClientTick(MinecraftClient client) {
        while (assignHostKey.wasPressed()) selectCorner(client, BlackjackConfig.Role.HOST);
        while (assignViewerKey.wasPressed()) selectCorner(client, BlackjackConfig.Role.VIEWER);
        while (editHudKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());
        updateSelectionHighlights(client);
        updateTotals(client);
    }

    private static void selectCorner(MinecraftClient client, BlackjackConfig.Role role) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ItemFrameEntity frame)) {
            client.player.sendMessage(Text.literal("Look directly at an item frame first."), true);
            return;
        }
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        String current = frame.getUuidAsString();
        if (selection.firstKey() == null || selection.secondKey() != null) {
            clearSelectionHighlights(client);
            BlackjackConfig.setFirstSelection(role, current);
            BlackjackConfig.setSecondSelection(role, null);
            BlackjackConfig.save(client);
            client.player.sendMessage(Text.literal(roleName(role) + ": first corner selected."), true);
        } else {
            BlackjackConfig.setSecondSelection(role, current);
            clearSelectionHighlights(client);
            BlackjackConfig.save(client);
            client.player.sendMessage(Text.literal(roleName(role) + ": area saved until you select new corners."), true);
        }
    }

    private static String roleName(BlackjackConfig.Role role) { return role == BlackjackConfig.Role.HOST ? "Host" : "Viewer"; }

    /**
     * Shows a temporary preview of the area between the already-selected first
     * corner and the item frame currently under the crosshair. This is visual
     * only: it is never persisted and is removed immediately when the second
     * corner is selected.
     */
    private static void updateSelectionHighlights(MinecraftClient client) {
        clearSelectionHighlights(client);
        if (client.world == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof ItemFrameEntity currentFrame)) return;

        highlightPreviewArea(client, BlackjackConfig.Role.HOST, currentFrame);
        highlightPreviewArea(client, BlackjackConfig.Role.VIEWER, currentFrame);
    }

    private static void highlightPreviewArea(MinecraftClient client, BlackjackConfig.Role role, ItemFrameEntity currentFrame) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        if (selection.firstKey() == null || selection.secondKey() != null) return;

        UUID firstUuid = parseUuid(selection.firstKey());
        ItemFrameEntity firstFrame = findFrameByUuid(client, firstUuid);
        if (firstFrame == null || firstFrame.isRemoved() || currentFrame.isRemoved()) return;

        BlockPos a = firstFrame.getBlockPos();
        BlockPos b = currentFrame.getBlockPos();
        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());

        Box area = new Box(minX - 0.25D, minY - 0.25D, minZ - 0.25D,
                maxX + 1.25D, maxY + 1.25D, maxZ + 1.25D);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(ItemFrameEntity.class, area,
                frame -> !frame.isRemoved());

        // Always include both corner frames, even if the entity query clips an
        // edge. Glowing is client-side and does not alter the saved selection.
        glowFrame(firstFrame);
        glowFrame(currentFrame);
        for (ItemFrameEntity frame : frames) glowFrame(frame);
    }

    private static void glowFrame(ItemFrameEntity frame) {
        UUID id = frame.getUuid();
        selectionHighlights.putIfAbsent(id, frame.isGlowing());
        frame.setGlowing(true);
    }

    private static void clearSelectionHighlights(MinecraftClient client) {
        if (client.world == null) {
            selectionHighlights.clear();
            return;
        }
        for (Map.Entry<UUID, Boolean> entry : selectionHighlights.entrySet()) {
            ItemFrameEntity frame = findFrameByUuid(client, entry.getKey());
            if (frame != null && !frame.isRemoved()) frame.setGlowing(entry.getValue());
        }
        selectionHighlights.clear();
    }

    private static ItemFrameEntity findFrameByUuid(MinecraftClient client, UUID uuid) {
        if (client.world == null || uuid == null) return null;
        Entity entity = client.world.getEntity(uuid);
        return entity instanceof ItemFrameEntity frame ? frame : null;
    }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) { hostTotal = viewerTotal = 0; hostHasCards = viewerHasCards = false; return; }
        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(ItemFrameEntity.class, box, frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());
        int hostFixed = 0, hostAces = 0, viewerFixed = 0, viewerAces = 0;
        boolean foundHost = false, foundViewer = false;
        for (ItemFrameEntity frame : frames) {
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            BlackjackConfig.Role role = null;
            if (inHost && !inViewer) role = BlackjackConfig.Role.HOST;
            else if (inViewer && !inHost) role = BlackjackConfig.Role.VIEWER;
            if (role == null) continue;
            String mapId = mapIdSignature(frame.getHeldItemStack());
            if (mapId == null) continue;
            int value = BlackjackConfig.getScannedMapValue(mapId);
            if (value < 0) continue;
            if (role == BlackjackConfig.Role.HOST) { foundHost = true; if (value == 11) hostAces++; else hostFixed += value; }
            else { foundViewer = true; if (value == 11) viewerAces++; else viewerFixed += value; }
        }
        hostHasCards = foundHost; viewerHasCards = foundViewer;
        hostTotal = BlackjackScore.score(hostFixed, hostAces); viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
    }

    private static boolean isFrameInsideSelection(MinecraftClient client, ItemFrameEntity frame, BlackjackConfig.Role role) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        if (selection.firstKey() == null || selection.secondKey() == null) return false;
        ItemFrameEntity first = findFrameByUuid(client, parseUuid(selection.firstKey()));
        ItemFrameEntity second = findFrameByUuid(client, parseUuid(selection.secondKey()));
        if (first == null || second == null) return false;
        BlockPos a = first.getBlockPos(), b = second.getBlockPos(), p = frame.getBlockPos();
        return between(p.getX(), a.getX(), b.getX()) && between(p.getY(), a.getY(), b.getY()) && between(p.getZ(), a.getZ(), b.getZ());
    }

    private static UUID parseUuid(String value) { try { return UUID.fromString(value); } catch (Exception e) { return null; } }
    private static boolean between(int value, int a, int b) { return value >= Math.min(a, b) && value <= Math.max(a, b); }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        drawTotal(context, client, BlackjackConfig.getHostName(), BlackjackScore.display(hostTotal, hostHasCards), true);
        drawTotal(context, client, BlackjackConfig.getViewerName(), BlackjackScore.display(viewerTotal, viewerHasCards), false);
    }

    private static void drawTotal(DrawContext context, MinecraftClient client, String name, String score, boolean host) {
        String text;
        if (name.isBlank()) text = score;
        else if (host) text = name + " " + score;
        else text = score + " " + name;
        int color = text.contains("BUST") ? 0xFFFF5555 : 0xFFFFFFFF;
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
