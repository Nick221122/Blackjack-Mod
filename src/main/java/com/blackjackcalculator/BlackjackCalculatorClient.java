package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlackjackCalculatorClient implements ClientModInitializer {
    private static final String MOD_ID = "blackjackcalculator";
    private static final Identifier HUD_ID = Identifier.of(MOD_ID, "totals");
    private static final double SCAN_RADIUS = 256.0D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding editHudKey;
    private static KeyBinding scanContainerKey;
    private static int hostTotal, viewerTotal;
    private static boolean hostHasCards, viewerHasCards;
    private static boolean mapSimulationKeyDown;
    private static boolean hostAlwaysWinsKeyDown;
    private static boolean hostAlwaysWins;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlackjackConfig.load(client);
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "controls"));
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.edit_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, category));
        scanContainerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.blackjackcalculator.scan_container", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, category));
        ClientTickEvents.END_CLIENT_TICK.register(BlackjackCalculatorClient::onClientTick);
        ScreenEvents.BEFORE_INIT.register((client1, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.beforeKeyPress(screen).register((screen1, context) -> {
                if (scanContainerKey.matchesKey(context)) {
                    scanOpenContainer(client1, screen1);
                }
            });
        });
        HudElementRegistry.addLast(HUD_ID, BlackjackCalculatorClient::renderHud);
    }

    private static void scanOpenContainer(MinecraftClient client, Screen screen) {
        if (client.player == null) return;
        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handled)) return;
        if (!(handled.getScreenHandler() instanceof net.minecraft.screen.Generic3x3ContainerScreenHandler handler)) {
            client.player.sendMessage(Text.literal("This is not a Dispenser or Dropper."), true);
            return;
        }
        if (client.world == null || !(client.crosshairTarget instanceof BlockHitResult blockHit)) {
            client.player.sendMessage(Text.literal("Look at the Dispenser or Dropper while scanning."), true);
            return;
        }
        boolean framesSelected = autoSelectFrameAreas(client, blockHit.getBlockPos());
        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        List<ItemStack> scannedStacks = new java.util.ArrayList<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) continue;
            String mapId = mapIdSignature(stack);
            if (mapId == null) continue;
            scanned.put(mapId, SCAN_VALUES[slot]);
            scannedStacks.add(stack);
            occupied++;
        }
        BlackjackConfig.replaceSharedScan(scanned);
        MapSimulationStore.clear();
        int simulated = MapSimulationStore.scanContainer(client, scannedStacks);
        BlackjackConfig.save(client);
        String frameStatus = framesSelected
                ? " Automatically selected Viewer frames on the left and Host frames on the right."
                : " Could not find item frames on both sides.";
        client.player.sendMessage(Text.literal("Scanned " + occupied + " map(s). Shared by Host and Viewer. Captured "
                + simulated + " visual map(s) locally." + frameStatus), true);
    }

    /**
     * Automatically assigns up to nine nearby item frames on each side of the scanned dispenser.
     * The dispenser's facing determines the wall plane and which side is Viewer (left) or Host (right).
     */
    private static boolean autoSelectFrameAreas(MinecraftClient client, BlockPos dispenserPos) {
        if (client.world == null || client.player == null) return false;
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
        client.player.sendMessage(Text.literal("Auto-selected " + viewerFrames.size() + " Viewer frame(s) on the left and "
                + hostFrames.size() + " Host frame(s) on the right."), true);
        return true;
    }

    private static void setSelectionBounds(BlackjackConfig.Role role, List<ItemFrameEntity> frames) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ItemFrameEntity frame : frames) {
            BlockPos p = frame.getBlockPos();
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        BlackjackConfig.setFirstSelection(role, minX + "|" + minY + "|" + minZ);
        BlackjackConfig.setSecondSelection(role, maxX + "|" + maxY + "|" + maxZ);
    }

    static String mapIdSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
        return mapId == null ? null : mapId.toString();
    }

    private static void onClientTick(MinecraftClient client) {
        while (editHudKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());
        boolean rPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (isNewMapSimulationKeyPress(rPressed, hostAlwaysWinsKeyDown)) {
            hostAlwaysWins = !hostAlwaysWins;
            // Keep the map-image overlay synchronized with Host Always Wins.
            MapSimulationStore.setEnabled(hostAlwaysWins);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Host Always Wins: " + (hostAlwaysWins ? "ON" : "OFF")), true);
            }
        }
        hostAlwaysWinsKeyDown = rPressed;

        boolean mPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        if (isNewMapSimulationKeyPress(mPressed, mapSimulationKeyDown)
                && !isMapSimulationToggleBlocked(client)) {
            toggleMapSimulation(client);
        }
        mapSimulationKeyDown = mPressed;
        updateTotals(client);
    }

    static boolean isNewMapSimulationKeyPress(boolean pressed, boolean wasPressed) {
        return pressed && !wasPressed;
    }

    static boolean isMapSimulationToggleBlocked(boolean hasFilledHostSelection, boolean hasFilledViewerSelection) {
        return hasFilledHostSelection || hasFilledViewerSelection;
    }

    private static boolean isMapSimulationToggleBlocked(MinecraftClient client) {
        return isMapSimulationToggleBlocked(
                hasFilledSelectedItemFrame(client, BlackjackConfig.Role.HOST),
                hasFilledSelectedItemFrame(client, BlackjackConfig.Role.VIEWER));
    }

    private static boolean hasFilledSelectedItemFrame(MinecraftClient client, BlackjackConfig.Role role) {
        if (client.world == null) return false;
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ItemFrameEntity frame
                    && !frame.isRemoved()
                    && !frame.getHeldItemStack().isEmpty()
                    && isFrameInsideSelection(client, frame, role)) {
                return true;
            }
        }
        return false;
    }

    private static void toggleMapSimulation(MinecraftClient client) {
        MapSimulationStore.toggle();
        updateTotals(client);
        if (client.player != null && MapSimulationStore.isEnabled()) {
            String count = BlackjackNarration.formatCount(
                    BlackjackConfig.getHostName(),
                    hostDisplay(),
                    hostHasCards,
                    BlackjackConfig.getViewerName(),
                    BlackjackScore.display(viewerTotal, viewerHasCards),
                    viewerHasCards);
            client.getNarratorManager().narrateSystemImmediately(count);
        }
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
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            BlackjackConfig.Role role = inHost && !inViewer ? BlackjackConfig.Role.HOST : inViewer && !inHost ? BlackjackConfig.Role.VIEWER : null;
            if (role == null) continue;
            String mapId = mapIdSignature(frame.getHeldItemStack());
            if (mapId == null) continue;
            int value = BlackjackConfig.getScannedMapValue(mapId);
            if (value < 0) continue;
            if (role == BlackjackConfig.Role.HOST) {
                foundHost = true;
                if (value == 11) hostAces++;
                else hostFixed += value;
            } else {
                foundViewer = true;
                if (value == 11) viewerAces++;
                else viewerFixed += value;
            }
        }
        hostHasCards = foundHost;
        viewerHasCards = foundViewer;
        hostTotal = BlackjackScore.score(hostFixed, hostAces);
        viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
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
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean between(int value, int a, int b) {
        return value >= Math.min(a, b) && value <= Math.max(a, b);
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        // Keep real item stacks untouched; only the local display is forced to a winning score.
        // Host Always Wins cannot display a bust and may show 21 when enabled.
        drawTotal(context, client, BlackjackConfig.getHostName(), hostAlwaysWins ? "21" : hostDisplay(), true);
        drawTotal(context, client, BlackjackConfig.getViewerName(), hostAlwaysWins ? "LOSE" : BlackjackScore.display(viewerTotal, viewerHasCards), false);
        String status = "HOST ALWAYS WINS: " + (hostAlwaysWins ? "ON" : "OFF");
        int statusColor = hostAlwaysWins ? 0xFF55FF55 : 0xFFFF5555;
        context.drawText(client.textRenderer, status,
                client.getWindow().getScaledWidth() - client.textRenderer.getWidth(status) - 8,
                8, statusColor, true);
    }

    private static void drawTotal(DrawContext context, MinecraftClient client, String name, String score, boolean host) {
        String text = name.isBlank() ? score : host ? score + " " + name : name + " " + score;
        int color = text.contains("BUST") ? 0xFFFF5555 : 0xFFFFFFFF;
        float scale = host ? BlackjackConfig.getHostScale() : BlackjackConfig.getViewerScale();
        int width = Math.round(client.textRenderer.getWidth(text) * scale);
        int x = resolveHudX(host, host ? BlackjackConfig.getHostX() : BlackjackConfig.getViewerX(), width, client.getWindow().getScaledWidth());
        int y = host ? BlackjackConfig.getHostY() : BlackjackConfig.getViewerY();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(client.textRenderer, text, 0, 0, color, true);
        context.getMatrices().popMatrix();
    }

    static int resolveHudX(boolean host, int configuredX, int textWidth, int screenWidth) {
        if (host && configuredX < 0) return Math.max(0, screenWidth - textWidth - 8);
        return Math.max(0, configuredX);
    }

    private static String hostDisplay() {
        return MapSimulationStore.isEnabled()
                ? BlackjackScore.displayHost(hostTotal, hostHasCards)
                : BlackjackScore.display(hostTotal, hostHasCards);
    }

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
    public static boolean isHostAlwaysWinsEnabled() { return hostAlwaysWins; }
}
