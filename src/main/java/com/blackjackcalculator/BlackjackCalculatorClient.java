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

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 64.0D;
    private static final double SELECTION_RAY_DISTANCE = 64.0D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding assignHostKey;
    private static KeyBinding assignViewerKey;
    private static KeyBinding editHudKey;
    private static int hostTotal, viewerTotal;
    private static boolean hostHasCards, viewerHasCards;

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
        return screen instanceof Generic3x3ContainerScreen || screen.getTitle().getString().equalsIgnoreCase("Dispenser") || screen.getTitle().getString().equalsIgnoreCase("Dropper");
    }

    private static void scanOpenContainer(MinecraftClient client, Screen screen) {
        if (client.player == null) return;
        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handled)) return;
        if (!(handled.getScreenHandler() instanceof net.minecraft.screen.Generic3x3ContainerScreenHandler handler)) { client.player.sendMessage(Text.literal("This is not a Dispenser or Dropper."), true); return; }
        if (!(client.crosshairTarget instanceof BlockHitResult)) { client.player.sendMessage(Text.literal("Look at the Dispenser or Dropper while scanning."), true); return; }
        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) continue;
            String mapId = mapIdSignature(stack);
            if (mapId == null) continue;
            scanned.put(mapId, SCAN_VALUES[slot]); occupied++;
        }
        BlackjackConfig.replaceSharedScan(scanned); BlackjackConfig.save(client);
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
        updateTotals(client);
    }

    private static void selectCorner(MinecraftClient client, BlackjackConfig.Role role) {
        if (client.player == null || client.world == null) return;
        ItemFrameEntity frame = findTargetFrame(client);
        if (frame == null) { client.player.sendMessage(Text.literal("Look directly at an item frame first."), true); return; }
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        String current = BlackjackConfig.selectionKey(frame);
        if (selection.firstKey() == null || selection.secondKey() != null) {
            BlackjackConfig.setFirstSelection(role, current); BlackjackConfig.setSecondSelection(role, null); BlackjackConfig.save(client);
            client.player.sendMessage(Text.literal(roleName(role) + ": first corner selected."), true);
        } else {
            BlackjackConfig.setSecondSelection(role, current); BlackjackConfig.save(client);
            client.player.sendMessage(Text.literal(roleName(role) + ": area saved until you select new corners."), true);
        }
    }

    /** Selection uses a 64-block custom raycast instead of the normal interaction range. */
    private static ItemFrameEntity findTargetFrame(MinecraftClient client) {
        var start = client.player.getCameraPosVec(1.0F);
        var end = start.add(client.player.getRotationVec(1.0F).multiply(SELECTION_RAY_DISTANCE));
        Box searchBox = new Box(start, end).expand(1.0D);
        ItemFrameEntity best = null; double bestDistance = Double.MAX_VALUE;
        for (ItemFrameEntity frame : client.world.getEntitiesByClass(ItemFrameEntity.class, searchBox, f -> !f.isRemoved())) {
            var result = frame.getBoundingBox().expand(0.15D).raycast(start, end);
            if (result.isEmpty()) continue;
            double distance = result.get().squaredDistanceTo(start);
            if (distance < bestDistance) { bestDistance = distance; best = frame; }
        }
        return best;
    }

    private static String roleName(BlackjackConfig.Role role) { return role == BlackjackConfig.Role.HOST ? "Host" : "Viewer"; }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) { hostTotal = viewerTotal = 0; hostHasCards = viewerHasCards = false; return; }
        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(ItemFrameEntity.class, box, frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());
        int hostFixed = 0, hostAces = 0, viewerFixed = 0, viewerAces = 0; boolean foundHost = false, foundViewer = false;
        for (ItemFrameEntity frame : frames) {
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            BlackjackConfig.Role role = inHost && !inViewer ? BlackjackConfig.Role.HOST : inViewer && !inHost ? BlackjackConfig.Role.VIEWER : null;
            if (role == null) continue;
            String mapId = mapIdSignature(frame.getHeldItemStack()); if (mapId == null) continue;
            int value = BlackjackConfig.getScannedMapValue(mapId); if (value < 0) continue;
            if (role == BlackjackConfig.Role.HOST) { foundHost = true; if (value == 11) hostAces++; else hostFixed += value; }
            else { foundViewer = true; if (value == 11) viewerAces++; else viewerFixed += value; }
        }
        hostHasCards = foundHost; viewerHasCards = foundViewer;
        hostTotal = BlackjackScore.score(hostFixed, hostAces); viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
    }

    private static boolean isFrameInsideSelection(MinecraftClient client, ItemFrameEntity frame, BlackjackConfig.Role role) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        BlockPos first = resolveSelectionPos(client, selection.firstKey());
        BlockPos second = resolveSelectionPos(client, selection.secondKey());
        if (first == null || second == null) return false;
        BlockPos p = frame.getBlockPos();
        return between(p.getX(), first.getX(), second.getX()) && between(p.getY(), first.getY(), second.getY()) && between(p.getZ(), first.getZ(), second.getZ());
    }

    private static BlockPos resolveSelectionPos(MinecraftClient client, String key) {
        BlockPos pos = BlackjackConfig.parseSelectionPos(key);
        if (pos != null) return pos;
        if (key == null || client.world == null) return null;
        try {
            String uuidText = key.substring(key.lastIndexOf(':') + 1);
            Entity entity = client.world.getEntity(UUID.fromString(uuidText));
            return entity instanceof ItemFrameEntity frame ? frame.getBlockPos() : null;
        } catch (Exception ignored) { return null; }
    }

    private static boolean between(int value, int a, int b) { return value >= Math.min(a, b) && value <= Math.max(a, b); }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        drawTotal(context, client, BlackjackConfig.getHostName(), BlackjackScore.display(hostTotal, hostHasCards), true);
        drawTotal(context, client, BlackjackConfig.getViewerName(), BlackjackScore.display(viewerTotal, viewerHasCards), false);
    }

    private static void drawTotal(DrawContext context, MinecraftClient client, String name, String score, boolean host) {
        String text = name.isBlank() ? score : host ? name + " " + score : score + " " + name;
        int color = text.contains("BUST") ? 0xFFFF5555 : 0xFFFFFFFF;
        float scale = host ? BlackjackConfig.getHostScale() : BlackjackConfig.getViewerScale();
        int width = Math.round(client.textRenderer.getWidth(text) * scale);
        int x = host ? BlackjackConfig.getHostX() : BlackjackConfig.getViewerX(), y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        if (!host && x < 0) x = client.getWindow().getScaledWidth() - width - 8;
        context.getMatrices().pushMatrix(); context.getMatrices().translate(x, y); context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, true); context.getMatrices().popMatrix();
    }

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
}
