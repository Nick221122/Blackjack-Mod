package com.blackjackcalculator;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 256.0D;
    private static final double SELECTION_RAY_DISTANCE = 256.0D;
    private static final double SELECTION_HIT_EXPANSION = 0.5D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyMapping assignHostKey;
    private static KeyMapping assignViewerKey;
    private static KeyMapping editHudKey;
    private static int hostTotal, viewerTotal;
    private static boolean hostHasCards, viewerHasCards;

    @Override
    public void onInitializeClient() {
        Minecraft client = Minecraft.getInstance();
        BlackjackConfig.load(client);
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
        assignHostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blackjackcalculator.assign_host", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        assignViewerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blackjackcalculator.assign_viewer", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, category));
        editHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blackjackcalculator.edit_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, category));
        ClientTickEvents.END_CLIENT_TICK.register(BlackjackCalculatorClient::onClientTick);
        HudElementRegistry.addLast(HUD_ID, BlackjackCalculatorClient::renderHud);
        ScreenEvents.AFTER_INIT.register(BlackjackCalculatorClient::onScreenInit);
    }

    private static void onScreenInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!isDispenserOrDropperScreen(screen)) return;
        int buttonX = Math.max(4, (scaledWidth - 176) / 2 - 62);
        int buttonY = Math.max(4, (scaledHeight - 114) / 2 + 20);
        Screens.getWidgets(screen).add(Button.builder(Component.translatable("button.blackjackcalculator.scan"), button -> scanOpenContainer(client, screen)).bounds(buttonX, buttonY, 56, 20).build());
    }

    private static boolean isDispenserOrDropperScreen(Screen screen) {
        String title = screen.getTitle().getString();
        return title.equalsIgnoreCase("Dispenser") || title.equalsIgnoreCase("Dropper");
    }

    private static void scanOpenContainer(Minecraft client, Screen screen) {
        if (client.player == null) return;
        if (!(screen instanceof AbstractContainerScreen<?> handled)) return;
        AbstractContainerMenu handler = handled.getMenu();
        if (handler.slots.size() < 9) {
            client.player.sendSystemMessage(Component.literal("This is not a Dispenser or Dropper."));
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult)) {
            client.player.sendSystemMessage(Component.literal("Look at the Dispenser or Dropper while scanning."));
            return;
        }
        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            String mapId = mapIdSignature(stack);
            if (mapId == null) continue;
            scanned.put(mapId, SCAN_VALUES[slot]);
            occupied++;
        }
        BlackjackConfig.replaceSharedScan(scanned);
        BlackjackConfig.save(client);
        client.player.sendSystemMessage(Component.literal("Scanned " + occupied + " map(s). Shared by Host and Viewer."));
    }

    private static String mapIdSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var mapId = stack.get(DataComponents.MAP_ID);
        return mapId == null ? null : mapId.toString();
    }

    private static void onClientTick(Minecraft client) {
        while (assignHostKey.consumeClick()) selectCorner(client, BlackjackConfig.Role.HOST);
        while (assignViewerKey.consumeClick()) selectCorner(client, BlackjackConfig.Role.VIEWER);
        while (editHudKey.consumeClick()) if (client.gui.screen() == null) client.gui.setScreen(new BlackjackHudEditorScreen());
        updateTotals(client);
    }

    private static void selectCorner(Minecraft client, BlackjackConfig.Role role) {
        if (client.player == null || client.level == null) return;
        ItemFrame frame = findTargetFrame(client);
        if (frame == null) {
            client.player.sendSystemMessage(Component.literal("Look directly at an item frame first."));
            return;
        }
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        String current = BlackjackConfig.selectionKey(frame);
        if (selection.firstKey() == null || selection.secondKey() != null) {
            BlackjackConfig.setFirstSelection(role, current);
            BlackjackConfig.setSecondSelection(role, null);
            BlackjackConfig.save(client);
            client.player.sendSystemMessage(Component.literal(roleName(role) + ": first corner selected."));
        } else {
            BlackjackConfig.setSecondSelection(role, current);
            BlackjackConfig.save(client);
            client.player.sendSystemMessage(Component.literal(roleName(role) + ": area saved until you select new corners."));
        }
    }

    static ItemFrame findTargetFrame(Minecraft client) {
        Vec3 start = client.player.getEyePosition(1.0F);
        Vec3 end = start.add(client.player.getViewVector(1.0F).scale(SELECTION_RAY_DISTANCE));
        AABB searchBox = new AABB(start, end).inflate(1.0D);
        ItemFrame best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemFrame frame : client.level.getEntitiesOfClass(ItemFrame.class, searchBox, f -> !f.isRemoved())) {
            var result = frame.getBoundingBox().inflate(SELECTION_HIT_EXPANSION).clip(start, end);
            if (result.isEmpty()) continue;
            double distance = result.get().distanceToSqr(start);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = frame;
            }
        }
        return best;
    }

    private static String roleName(BlackjackConfig.Role role) {
        return role == BlackjackConfig.Role.HOST ? "Host" : "Viewer";
    }

    private static void updateTotals(Minecraft client) {
        if (client.player == null || client.level == null) {
            hostTotal = viewerTotal = 0;
            hostHasCards = viewerHasCards = false;
            return;
        }
        AABB box = client.player.getBoundingBox().inflate(SCAN_RADIUS);
        List<ItemFrame> frames = client.level.getEntitiesOfClass(ItemFrame.class, box, frame -> !frame.isRemoved() && !frame.getItem().isEmpty());
        int hostFixed = 0, hostAces = 0, viewerFixed = 0, viewerAces = 0;
        boolean foundHost = false, foundViewer = false;
        for (ItemFrame frame : frames) {
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            BlackjackConfig.Role role = inHost && !inViewer ? BlackjackConfig.Role.HOST : inViewer && !inHost ? BlackjackConfig.Role.VIEWER : null;
            if (role == null) continue;
            String mapId = mapIdSignature(frame.getItem());
            if (mapId == null) continue;
            int value = BlackjackConfig.getScannedMapValue(mapId);
            if (value < 0) continue;
            if (role == BlackjackConfig.Role.HOST) {
                foundHost = true;
                if (value == 11) hostAces++; else hostFixed += value;
            } else {
                foundViewer = true;
                if (value == 11) viewerAces++; else viewerFixed += value;
            }
        }
        hostHasCards = foundHost;
        viewerHasCards = foundViewer;
        hostTotal = BlackjackScore.score(hostFixed, hostAces);
        viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
    }

    private static boolean isFrameInsideSelection(Minecraft client, ItemFrame frame, BlackjackConfig.Role role) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        BlockPos first = BlackjackConfig.parseSelectionPos(selection.firstKey());
        BlockPos second = BlackjackConfig.parseSelectionPos(selection.secondKey());
        if (first == null || second == null) return false;
        BlockPos p = frame.blockPosition();
        return between(p.getX(), first.getX(), second.getX()) && between(p.getY(), first.getY(), second.getY()) && between(p.getZ(), first.getZ(), second.getZ());
    }

    private static boolean between(int value, int a, int b) {
        return value >= Math.min(a, b) && value <= Math.max(a, b);
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.hud.isHidden()) return;
        drawTotal(graphics, client, BlackjackConfig.getHostName(), BlackjackScore.display(hostTotal, hostHasCards), true);
        drawTotal(graphics, client, BlackjackConfig.getViewerName(), BlackjackScore.display(viewerTotal, viewerHasCards), false);
    }

    private static void drawTotal(GuiGraphicsExtractor graphics, Minecraft client, String name, String score, boolean host) {
        String text = name.isBlank() ? score : host ? name + " " + score : score + " " + name;
        int color = text.contains("BUST") ? 0xFFFF5555 : 0xFFFFFFFF;
        float scale = host ? BlackjackConfig.getHostScale() : BlackjackConfig.getViewerScale();
        int width = Math.round(client.font.width(text) * scale);
        int x = host ? BlackjackConfig.getHostX() : BlackjackConfig.getViewerX();
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        if (!host && x < 0) x = client.getWindow().getGuiScaledWidth() - width - 8;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(client.font, Component.literal(text), 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
}
