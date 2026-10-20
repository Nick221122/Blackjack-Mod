package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handles dispenser/dropper map scanning and nearby frame selection. */
final class MapContainerScanner {
    private MapContainerScanner() {}

    static void scanOpenContainer(MinecraftClient client, Screen screen) {
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
            String mapId = BlackjackCalculatorClient.mapIdSignature(stack);
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


}
