package com.hostalwayswins;

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
import net.minecraft.block.Blocks;
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
    private static final double SELECTION_RAY_DISTANCE = 256.0D;
    private static final double SELECTION_HIT_EXPANSION = 0.5D;
    private static final int[] SCAN_VALUES = {11, 3, 4, 5, 6, 7, 8, 9, 10};

    private static KeyBinding editHudKey;
    private static KeyBinding scanContainerKey;
    private static int hostTotal, viewerTotal;
    private static boolean hostHasCards, viewerHasCards;
    private static boolean hostAlwaysWins;
    private static boolean hostAlwaysWinsKeyDown;

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
        if (client.world == null) return;
        BlockPos dispenserPos = findOpenDispenserPos(client);
        if (dispenserPos == null) {
            client.player.sendMessage(Text.literal("Look at the Dispenser or Dropper while scanning."), true);
            return;
        }
        boolean framesSelected = autoSelectFrameAreas(client, dispenserPos);
        Map<String, Integer> scanned = new java.util.LinkedHashMap<>();
        int occupied = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (stack.isEmpty()) continue;
            String mapId = mapIdSignature(stack);
            if (mapId == null) continue;
            MapIdComponent component = stack.get(DataComponentTypes.MAP_ID);
            if (component != null) MapSimulationStore.capture(component.id(), client.world.getMapState(component));
            scanned.putIfAbsent(mapId, SCAN_VALUES[slot]);
            occupied++;
        }
        BlackjackConfig.replaceSharedScan(scanned);
        BlackjackConfig.save(client);
        if (framesSelected) {
            client.player.sendMessage(Text.literal("Scanned " + scanned.size() + " maps. Viewer: left (" + selectedFrameCount(BlackjackConfig.Role.VIEWER) + ") | Host: right (" + selectedFrameCount(BlackjackConfig.Role.HOST) + ")."), true);
        } else {
            client.player.sendMessage(Text.literal("Scanned " + scanned.size() + " maps, but frame selection failed."), true);
        }
    }

    private static BlockPos findOpenDispenserPos(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;
        if (client.crosshairTarget instanceof BlockHitResult hit && isDispenserOrDropper(client, hit.getBlockPos())) {
            return hit.getBlockPos();
        }
        var raycast = client.player.raycast(5.0D, 1.0F, false);
        if (raycast instanceof BlockHitResult hit && isDispenserOrDropper(client, hit.getBlockPos())) {
            return hit.getBlockPos();
        }
        return null;
    }

    private static boolean isDispenserOrDropper(MinecraftClient client, BlockPos pos) {
        if (client.world == null) return false;
        var state = client.world.getBlockState(pos);
        return state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER);
    }

    static String mapIdSignature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
        return mapId == null ? null : Integer.toString(mapId.id());
    }

    private static void onClientTick(MinecraftClient client) {
        while (editHudKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new BlackjackHudEditorScreen());

        boolean rPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rPressed && !hostAlwaysWinsKeyDown) {
            hostAlwaysWins = !hostAlwaysWins;
            MapSimulationStore.setEnabled(hostAlwaysWins);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Host Always Wins: " + (hostAlwaysWins ? "ON" : "OFF")), true);
            }
        }
        hostAlwaysWinsKeyDown = rPressed;
        updateTotals(client);
    }

    /**
     * Automatically assigns nearby item frames on each side of the scanned dispenser.
     * The dispenser's facing determines the wall plane and which side is Viewer or Host.
     */
    private static boolean autoSelectFrameAreas(MinecraftClient client, BlockPos dispenserPos) {
        if (client.world == null || client.player == null) return false;
        var state = client.world.getBlockState(dispenserPos);
        if (!state.contains(DispenserBlock.FACING)) return false;
        Direction facing = state.get(DispenserBlock.FACING);
        if (!facing.getAxis().isHorizontal()) return false;

        Box searchBox = new Box(dispenserPos).expand(9.0D);
        List<ItemFrameEntity> nearby = client.world.getEntitiesByClass(ItemFrameEntity.class, searchBox,
                frame -> !frame.isRemoved() && frame.getHorizontalFacing().getAxis() == facing.getAxis());
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
        BlackjackConfig.setAutoSelectedFrames(BlackjackConfig.Role.VIEWER, viewerFrames.stream().map(BlackjackConfig::selectionKey).collect(java.util.stream.Collectors.toSet()));
        BlackjackConfig.setAutoSelectedFrames(BlackjackConfig.Role.HOST, hostFrames.stream().map(BlackjackConfig::selectionKey).collect(java.util.stream.Collectors.toSet()));
        BlackjackConfig.save(client);
        return true;
    }

    private static int selectedFrameCount(BlackjackConfig.Role role) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        BlockPos first = BlackjackConfig.parseSelectionPos(selection.firstKey());
        BlockPos second = BlackjackConfig.parseSelectionPos(selection.secondKey());
        if (first == null || second == null) return 0;
        int count = 0;
        for (int x = Math.min(first.getX(), second.getX()); x <= Math.max(first.getX(), second.getX()); x++)
            for (int y = Math.min(first.getY(), second.getY()); y <= Math.max(first.getY(), second.getY()); y++)
                for (int z = Math.min(first.getZ(), second.getZ()); z <= Math.max(first.getZ(), second.getZ()); z++)
                    if (BlackjackConfig.isAutoSelectedFrame(role, new BlockPos(x, y, z))) count++;
        return count;
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

    /** Long-range frame targeting used by the selection-area preview renderer. */
    static ItemFrameEntity findTargetFrame(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) return null;
        var start = client.player.getCameraPosVec(1.0F);
        var end = start.add(client.player.getRotationVec(1.0F).multiply(SELECTION_RAY_DISTANCE));
        Box searchBox = new Box(start, end).expand(1.0D);
        ItemFrameEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemFrameEntity frame : client.world.getEntitiesByClass(ItemFrameEntity.class, searchBox, f -> !f.isRemoved())) {
            var result = frame.getBoundingBox().expand(SELECTION_HIT_EXPANSION).raycast(start, end);
            if (result.isEmpty()) continue;
            double distance = result.get().squaredDistanceTo(start);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = frame;
            }
        }
        return best;
    }

    private static void updateTotals(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            hostTotal = viewerTotal = 0;
            hostHasCards = viewerHasCards = false;
            MapSimulationStore.setHostDisplayOverrides(Map.of());
            return;
        }

        Box box = client.player.getBoundingBox().expand(SCAN_RADIUS);
        List<ItemFrameEntity> frames = client.world.getEntitiesByClass(
                ItemFrameEntity.class, box,
                frame -> !frame.isRemoved() && !frame.getHeldItemStack().isEmpty());

        List<Integer> hostMapIds = new java.util.ArrayList<>();
        java.util.Set<Integer> viewerMapIds = new java.util.LinkedHashSet<>();
        Map<Integer, Integer> availableMapValues = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : BlackjackConfig.getSharedScannedMaps().entrySet()) {
            try {
                int id = Integer.parseInt(entry.getKey());
                int value = entry.getValue();
                if (value >= 1 && value <= 11 && MapSimulationStore.hasRepresentation(id)) availableMapValues.put(id, value);
            } catch (NumberFormatException ignored) {
                // Old config entries used MapIdComponent.toString(); a fresh scan normalizes them.
            }
        }

        int viewerFixed = 0, viewerAces = 0;
        boolean foundHost = false, foundViewer = false;
        for (ItemFrameEntity frame : frames) {
            boolean inHost = isFrameInsideSelection(client, frame, BlackjackConfig.Role.HOST);
            boolean inViewer = isFrameInsideSelection(client, frame, BlackjackConfig.Role.VIEWER);
            BlackjackConfig.Role role = inHost && !inViewer ? BlackjackConfig.Role.HOST
                    : inViewer && !inHost ? BlackjackConfig.Role.VIEWER : null;
            if (role == null) continue;

            String mapIdText = mapIdSignature(frame.getHeldItemStack());
            if (mapIdText == null) continue;
            int mapId;
            try {
                mapId = Integer.parseInt(mapIdText);
            } catch (NumberFormatException ignored) {
                continue;
            }

            int value = BlackjackConfig.getScannedMapValue(mapIdText);
            if (role == BlackjackConfig.Role.VIEWER) {
                viewerMapIds.add(mapId);
                if (value < 0) continue;
                foundViewer = true;
                if (value == 11) viewerAces++;
                else viewerFixed += value;
            } else {
                if (value < 0) continue;
                foundHost = true;
                hostMapIds.add(mapId);
            }
        }

        viewerHasCards = foundViewer;
        viewerTotal = BlackjackScore.score(viewerFixed, viewerAces);
        Map<Integer, Integer> displayIds = Map.of();
        if (hostAlwaysWins && foundHost && !availableMapValues.isEmpty()) {
            long seed = 0x9E3779B97F4A7C15L ^ viewerTotal;
            for (int id : hostMapIds) {
                seed = Long.rotateLeft(seed ^ id, 13) * 0xBF58476D1CE4E5B9L;
            }
            for (Map.Entry<Integer, Integer> entry : availableMapValues.entrySet()) {
                seed = Long.rotateLeft(seed ^ entry.getKey(), 7) * 0x94D049BB133111EBL;
                seed ^= entry.getValue();
            }
            displayIds = HostAlwaysWinsStrategy.chooseDisplayIds(
                    hostMapIds, viewerMapIds, availableMapValues, viewerTotal, foundViewer, seed);
            MapSimulationStore.setHostDisplayOverrides(displayIds);
        } else {
            MapSimulationStore.setHostDisplayOverrides(Map.of());
        }

        int hostFixed = 0, hostAces = 0;
        for (int actualId : hostMapIds) {
            int displayId = displayIds.getOrDefault(actualId, actualId);
            int value = availableMapValues.getOrDefault(
                    displayId, BlackjackConfig.getScannedMapValue(Integer.toString(displayId)));
            if (value < 1 || value > 11) continue;
            if (value == 11) hostAces++;
            else hostFixed += value;
        }
        hostHasCards = foundHost;
        hostTotal = BlackjackScore.score(hostFixed, hostAces);
    }

    private static boolean isFrameInsideSelection(MinecraftClient client, ItemFrameEntity frame, BlackjackConfig.Role role) {
        BlackjackConfig.FrameSelection selection = BlackjackConfig.getSelection(role);
        BlockPos first = resolveSelectionPos(client, selection.firstKey());
        BlockPos second = resolveSelectionPos(client, selection.secondKey());
        if (first == null || second == null) return false;
        BlockPos p = frame.getBlockPos();
        return BlackjackConfig.isAutoSelectedFrame(role, p)
                && between(p.getX(), first.getX(), second.getX()) && between(p.getY(), first.getY(), second.getY()) && between(p.getZ(), first.getZ(), second.getZ());
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
        drawTotal(context, client, BlackjackConfig.getHostName(), BlackjackScore.display(hostTotal, hostHasCards), true);
        drawTotal(context, client, BlackjackConfig.getViewerName(), BlackjackScore.display(viewerTotal, viewerHasCards), false);
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

    public static int getHostTotal() { return hostTotal; }
    public static int getViewerTotal() { return viewerTotal; }
    public static boolean hasHostCards() { return hostHasCards; }
    public static boolean hasViewerCards() { return viewerHasCards; }
}
