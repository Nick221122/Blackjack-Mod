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
    static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding editHudKey;
    private static KeyBinding scanContainerKey;
    private static int hostTotal, viewerTotal;
    private static int hostCardCount;
    private static int cachedWinningHostScore;
    private static int cachedHostCardCount = -1;
    private static int cachedViewerTotal = Integer.MIN_VALUE;
    private static boolean cachedViewerHasCards;
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

    static String mapIdSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
        return mapIdSignature(mapId);
    }

    static String mapIdSignature(MapIdComponent mapId) {
        // Use the stable numeric map ID for both the dispenser scan and frame lookup.
        return mapId == null ? null : Integer.toString(mapId.id());
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
            hostCardCount = 0;
            hostHasCards = viewerHasCards = false;
            MapSimulationStore.setHostDisplayOverrides(Map.of());
            return;
        }

        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(
                ItemFrameEntity.class, box, frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());
        List<Integer> hostMapIds = new java.util.ArrayList<>();
        java.util.Set<Integer> viewerMapIds = new java.util.HashSet<>();
        int viewerFixed = 0, viewerAces = 0;
        boolean foundViewer = false;

        // First calculate the viewer from the ordinary overlay. Host overrides are
        // chosen only after the viewer's total is known for this tick.
        for (ItemFrameEntity frame : frames) {
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            BlackjackConfig.Role role = inHost && !inViewer ? BlackjackConfig.Role.HOST
                    : inViewer && !inHost ? BlackjackConfig.Role.VIEWER : null;
            if (role == null) continue;

            String signature = mapIdSignature(frame.getHeldItemStack());
            if (signature == null) continue;
            int actualId;
            try {
                actualId = Integer.parseInt(signature);
            } catch (NumberFormatException ignored) {
                continue;
            }

            if (role == BlackjackConfig.Role.HOST) {
                hostMapIds.add(actualId);
                continue;
            }

            viewerMapIds.add(actualId);
            Integer displayedId = resolveDisplayMapId(actualId, MapSimulationStore.isEnabled(),
                    MapSimulationStore.defaultDisplayMapId(actualId));
            if (displayedId == null) continue;
            int value = BlackjackConfig.getScannedMapValue(Integer.toString(displayedId));
            if (value < 0) continue;
            foundViewer = true;
            if (value == 11) viewerAces++;
            else viewerFixed += value;
        }

        viewerHasCards = foundViewer;
        viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);

        if (MapSimulationStore.isEnabled() && hostAlwaysWins) {
            // A map ID shared by both sides cannot show two different images, so
            // leave shared IDs on the normal overlay rather than corrupting Viewer.
            hostMapIds.removeIf(viewerMapIds::contains);
            Map<Integer, Integer> availableMapValues = BlackjackConfig.getScannedMapValuesById();
            availableMapValues.keySet().removeIf(id -> !MapSimulationStore.hasRepresentation(id));
            MapSimulationStore.setHostDisplayOverrides(HostAlwaysWinsStrategy.chooseDisplayIds(
                    hostMapIds, availableMapValues, viewerTotal, viewerHasCards));
        } else {
            MapSimulationStore.setHostDisplayOverrides(Map.of());
        }

        int hostFixed = 0, hostAces = 0, foundHostCount = 0;
        boolean foundHost = false;
        for (ItemFrameEntity frame : frames) {
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            if (!inHost || inViewer) continue;

            String signature = mapIdSignature(frame.getHeldItemStack());
            if (signature == null) continue;
            int actualId;
            try {
                actualId = Integer.parseInt(signature);
            } catch (NumberFormatException ignored) {
                continue;
            }
            Integer displayedId = resolveDisplayMapId(actualId, MapSimulationStore.isEnabled(),
                    MapSimulationStore.displayMapId(actualId));
            if (displayedId == null) continue;
            int value = BlackjackConfig.getScannedMapValue(Integer.toString(displayedId));
            if (value < 0) continue;
            foundHost = true;
            foundHostCount++;
            if (value == 11) hostAces++;
            else hostFixed += value;
        }

        hostHasCards = foundHost;
        hostCardCount = foundHostCount;
        if (hostCardCount == 0) cachedHostCardCount = -1;
        hostTotal = BlackjackScore.score(hostFixed, hostAces);
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
        // Show the score calculated from fake map values; never append a fake WIN result.
        String hostScore = hostDisplay();
        String viewerScore = BlackjackScore.display(viewerTotal, viewerHasCards);
        drawTotal(context, client, BlackjackConfig.getHostName(), hostScore, true);
        drawTotal(context, client, BlackjackConfig.getViewerName(), viewerScore, false);
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

    static int resolveDisplayMapId(int actualMapId, boolean simulationEnabled, Integer simulatedMapId) {
        // Do not let a missing local map representation trigger ternary unboxing NPEs.
        // An unavailable simulation should fall back to the actual ID (which may be unscanned).
        return simulationEnabled && simulatedMapId != null ? simulatedMapId : actualMapId;
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
