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
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 256.0D;
    private static final double SELECTION_RAY_DISTANCE = 256.0D;
    private static final double SELECTION_HIT_EXPANSION = 0.5D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding forceHostWinKey;
    private static KeyBinding editHudKey;
    private static int hostTotal, viewerTotal;
    private static boolean hostHasCards, viewerHasCards;
    private static boolean forceHostWin;
    private static final Map<String, ItemStack> ORIGINAL_FRAME_STACKS = new HashMap<>();
    private static final Map<Integer, ItemStack> ORIGINAL_DROPPED_STACKS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlackjackConfig.load(client);
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
        forceHostWinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.force_host_win", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));
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
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.translatable("button.blackjackcalculator.copy"), button -> scanOpenContainer(client, screen)).dimensions(buttonX, buttonY, 56, 20).build());
    }

    private static boolean isDispenserOrDropperScreen(Screen screen) {
        return screen instanceof Generic3x3ContainerScreen || screen.getTitle().getString().equalsIgnoreCase("Dispenser") || screen.getTitle().getString().equalsIgnoreCase("Dropper");
    }

    private static void scanOpenContainer(MinecraftClient client, Screen screen) {
        if (client.player == null || client.world == null) return;
        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handled)) return;
        if (!(handled.getScreenHandler() instanceof net.minecraft.screen.Generic3x3ContainerScreenHandler handler)) {
            client.player.sendMessage(Text.literal("This is not a Dispenser or Dropper."), true); return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult)) {
            client.player.sendMessage(Text.literal("Look at the Dispenser or Dropper while copying."), true); return;
        }
        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) continue;
            String mapId = mapIdSignature(stack);
            if (mapId == null) continue;
            scanned.put(mapId, SCAN_VALUES[slot]);
            MapIdComponent component = stack.get(DataComponentTypes.MAP_ID);
            MapState state = client.world.getMapState(component);
            if (state != null) BlackjackConfig.saveMapImage(mapId, state.colors.clone());
            occupied++;
        }
        BlackjackConfig.replaceSharedScan(scanned);
        BlackjackConfig.save(client);
        client.player.sendMessage(Text.literal("Copied " + occupied + " map(s), including their images."), true);
    }

    private static String mapIdSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
        return mapId == null ? null : mapId.toString();
    }

    private static void onClientTick(MinecraftClient client) {
        while (forceHostWinKey.wasPressed()) toggleForceHostWin(client);
        while (editHudKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());
        if (forceHostWin) applyLocalFakeCards(client); else restoreLocalFakeCards(client);
        updateTotals(client);
    }

    private static void toggleForceHostWin(MinecraftClient client) {
        forceHostWin = !forceHostWin;
        if (!forceHostWin) restoreLocalFakeCards(client);
        if (client.player != null) client.player.sendMessage(Text.literal(forceHostWin ? "Host-win mode ON (local display only)." : "Host-win mode OFF."), true);
    }

    private static void applyLocalFakeCards(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        int currentHost = calculateSelectionTotal(client, BlackjackConfig.Role.HOST);
        int desiredHostValue = currentHost <= 11 ? 10 : 1;
        String hostMapId = BlackjackConfig.getMapIdForValue(desiredHostValue);
        String viewerMapId = BlackjackConfig.getMapIdForValue(10);

        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        for (ItemFrameEntity frame : client.world.getEntitiesByClass(ItemFrameEntity.class, box, f -> !f.isRemoved() && !f.getHeldItemStack().isEmpty())) {
            BlackjackConfig.Role role = roleForFrame(client, frame);
            if (role == null) continue;
            String key = BlackjackConfig.selectionKey(frame);
            ORIGINAL_FRAME_STACKS.putIfAbsent(key, frame.getHeldItemStack().copy());
            String targetMapId = role == BlackjackConfig.Role.HOST ? hostMapId : viewerMapId;
            ItemStack fake = mapStackFromId(targetMapId);
            if (!fake.isEmpty()) frame.setHeldItemStack(fake);
        }

        for (ItemEntity entity : client.world.getEntitiesByClass(ItemEntity.class, box, e -> !e.isRemoved() && !e.getStack().isEmpty())) {
            ItemStack stack = entity.getStack();
            if (mapIdSignature(stack) == null) continue;
            ORIGINAL_DROPPED_STACKS.putIfAbsent(entity.getId(), stack.copy());
            String targetMapId = hostMapId;
            ItemStack fake = mapStackFromId(targetMapId);
            if (!fake.isEmpty()) entity.setStack(fake);
        }
    }

    private static ItemStack mapStackFromId(String id) {
        if (id == null) return ItemStack.EMPTY;
        try {
            int numericId = Integer.parseInt(id.substring(id.lastIndexOf(':') + 1).replaceAll("\\D+", ""));
            ItemStack stack = new ItemStack(Items.FILLED_MAP);
            stack.set(DataComponentTypes.MAP_ID, new MapIdComponent(numericId));
            return stack;
        } catch (Exception ignored) { return ItemStack.EMPTY; }
    }

    private static void restoreLocalFakeCards(MinecraftClient client) {
        if (client.world == null) return;
        for (ItemFrameEntity frame : client.world.getEntitiesByClass(ItemFrameEntity.class, client.player.getBoundingBox().expand(SCAN_RADIUS), f -> !f.isRemoved())) {
            ItemStack original = ORIGINAL_FRAME_STACKS.get(BlackjackConfig.selectionKey(frame));
            if (original != null) frame.setHeldItemStack(original.copy());
        }
        for (ItemEntity entity : client.world.getEntitiesByClass(ItemEntity.class, client.player.getBoundingBox().expand(SCAN_RADIUS), e -> !e.isRemoved())) {
            ItemStack original = ORIGINAL_DROPPED_STACKS.get(entity.getId());
            if (original != null) entity.setStack(original.copy());
        }
        ORIGINAL_FRAME_STACKS.clear();
        ORIGINAL_DROPPED_STACKS.clear();
    }

    private static BlackjackConfig.Role roleForFrame(MinecraftClient client, ItemFrameEntity frame) {
        boolean host = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
        boolean viewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
        return host && !viewer ? BlackjackConfig.Role.HOST : viewer && !host ? BlackjackConfig.Role.VIEWER : null;
    }

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

    private static int calculateSelectionTotal(MinecraftClient client, BlackjackConfig.Role role) {
        int fixed = 0, aces = 0;
        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        for (ItemFrameEntity frame : client.world.getEntitiesByClass(ItemFrameEntity.class, box, f -> !f.isRemoved() && !f.getHeldItemStack().isEmpty())) {
            if (!isFrameInsideSelection(client, frame, role)) continue;
            int value = BlackjackConfig.getScannedMapValue(mapIdSignature(frame.getHeldItemStack()));
            if (value < 0) continue;
            if (value == 11) aces++; else fixed += value;
        }
        return BlackjackScore.score(fixed, aces);
    }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) { hostTotal = viewerTotal = 0; hostHasCards = viewerHasCards = false; return; }
        if (forceHostWin) {
            hostTotal = 21;
            viewerTotal = 0;
            hostHasCards = true;
            viewerHasCards = true;
            return;
        }
        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(ItemFrameEntity.class, box, frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());
        int hostFixed = 0, hostAces = 0, viewerFixed = 0, viewerAces = 0; boolean foundHost = false, foundViewer = false;
        for (ItemFrameEntity frame : frames) {
            BlackjackConfig.Role role = roleForFrame(client, frame);
            if (role == null) continue;
            int value = BlackjackConfig.getScannedMapValue(mapIdSignature(frame.getHeldItemStack())); if (value < 0) continue;
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
