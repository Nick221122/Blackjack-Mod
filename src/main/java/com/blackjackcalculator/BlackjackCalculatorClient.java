package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 256.0D;
    private static final double SELECTION_RAY_DISTANCE = 256.0D;
    private static final double SELECTION_HIT_EXPANSION = 0.5D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding assignHostKey;
    private static KeyBinding assignViewerKey;
    private static KeyBinding editHudKey;
    private static KeyBinding mapSimulationKey;
    private static KeyBinding autoScanFramesKey;
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
        mapSimulationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.toggle_map_simulation", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, category));
        autoScanFramesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.auto_scan_frames", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        ClientTickEvents.END_CLIENT_TICK.register(BlackjackCalculatorClient::onClientTick);
        HudElementRegistry.addLast(HUD_ID, BlackjackCalculatorClient::renderHud);
    }

    private static void scanFramesFromTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit)) {
            client.player.sendMessage(Text.literal("Look at the dispenser while scanning frames."), true);
            return;
        }
        if (!autoSelectFrameAreas(client, blockHit.getBlockPos())) {
            client.player.sendMessage(Text.literal("No item frames found on both sides of the dispenser."), true);
        }
    }

    /**
     * Automatically assigns up to nine nearby item frames on each side of the scanned dispenser.
     * The dispenser's facing determines the wall plane and which side is Viewer (left) or Host (right).
     */
    private static boolean autoSelectFrameAreas(MinecraftClient client, BlockPos dispenserPos) {
        if (client.world == null) return false;
        var state = client.world.getBlockState(dispenserPos);
        if (!state.contains(DispenserBlock.FACING)) return false;
        Direction facing = state.get(DispenserBlock.FACING);
        if (!facing.getAxis().isHorizontal()) return false;

        Box searchBox = new Box(dispenserPos).expand(6.0D);
        List<ItemFrameEntity> nearby = client.world.getEntitiesByClass(ItemFrameEntity.class, searchBox,
                frame -> !frame.isRemoved() && frame.getHorizontalFacing() == facing);
        java.util.Comparator<ItemFrameEntity> nearestFirst = java.util.Comparator.comparingDouble(
                frame -> frame.squaredDistanceTo(dispenserPos.getX() + 0.5D, dispenserPos.getY() + 0.5D, dispenserPos.getZ() + 0.5D));
        java.util.List<ItemFrameEntity> viewerFrames = new java.util.ArrayList<>();
        java.util.List<ItemFrameEntity> hostFrames = new java.util.ArrayList<>();

        for (ItemFrameEntity frame : nearby) {
            BlockPos p = frame.getBlockPos();
            if (!FrameScanGeometry.isInDispenserWallPlane(facing, dispenserPos, p)) continue;
            if (FrameScanGeometry.isViewerSide(facing, dispenserPos, p)) viewerFrames.add(frame);
            else if (FrameScanGeometry.isHostSide(facing, dispenserPos, p)) hostFrames.add(frame);
        }

        viewerFrames.sort(nearestFirst);
        hostFrames.sort(nearestFirst);
        if (viewerFrames.isEmpty() || hostFrames.isEmpty()) return false;
        if (viewerFrames.size() > 9) viewerFrames = new java.util.ArrayList<>(viewerFrames.subList(0, 9));
        if (hostFrames.size() > 9) hostFrames = new java.util.ArrayList<>(hostFrames.subList(0, 9));

        setSelectionBounds(BlackjackConfig.Role.VIEWER, viewerFrames);
        setSelectionBounds(BlackjackConfig.Role.HOST, hostFrames);
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Auto-selected " + viewerFrames.size() + " Viewer frame(s) and " + hostFrames.size() + " Host frame(s)."), true);
        return true;
    }

    private static void setSelectionBounds(BlackjackConfig.Role role, List<ItemFrameEntity> frames) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ItemFrameEntity frame : frames) {
            BlockPos p = frame.getBlockPos();
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        BlackjackConfig.setFirstSelection(role, minX + "|" + minY + "|" + minZ);
        BlackjackConfig.setSecondSelection(role, maxX + "|" + maxY + "|" + maxZ);
    }

    private static void toggleMapSimulation(MinecraftClient client) {
        MapSimulationStore.toggle();
        if (client.player != null) {
            String state = MapSimulationStore.isEnabled() ? "enabled" : "disabled";
            client.player.sendMessage(Text.literal("Client map simulation " + state + "."), true);
        }
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
        while (mapSimulationKey.wasPressed()) toggleMapSimulation(client);
        while (autoScanFramesKey.wasPressed()) scanFramesFromTarget(client);
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

    /** Long-range selection raycast with a forgiving hitbox so distant frames are easier to select. */
    static ItemFrameEntity findTargetFrame(MinecraftClient client) {
        var start = client.player.getCameraPosVec(1.0F);
        var end = start.add(client.player.getRotationVec(1.0F).multiply(SELECTION_RAY_DISTANCE));
        Box searchBox = new Box(start, end).expand(1.0D);
        ItemFrameEntity best = null; double bestDistance = Double.MAX_VALUE;
        for (ItemFrameEntity frame : client.world.getEntitiesByClass(ItemFrameEntity.class, searchBox, f -> !f.isRemoved())) {
            var result = frame.getBoundingBox().expand(SELECTION_HIT_EXPANSION).raycast(start, end);
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
