package com.blackjackcalculator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameScanGeometryTest {
    private static final BlockPos DISPENSER = new BlockPos(10, 64, 10);

    @Test
    void assignsViewerToLeftAndHostToRightForEveryHorizontalFacing() {
        assertViewerAndHost(Direction.NORTH, new BlockPos(9, 64, 10), new BlockPos(11, 64, 10));
        assertViewerAndHost(Direction.SOUTH, new BlockPos(11, 64, 10), new BlockPos(9, 64, 10));
        assertViewerAndHost(Direction.EAST, new BlockPos(10, 64, 9), new BlockPos(10, 64, 11));
        assertViewerAndHost(Direction.WEST, new BlockPos(10, 64, 11), new BlockPos(10, 64, 9));
    }

    @Test
    void rejectsFramesOutsideTheDispenserWallPlane() {
        assertFalse(FrameScanGeometry.isInDispenserWallPlane(Direction.NORTH, DISPENSER, new BlockPos(11, 64, 13)));
        assertFalse(FrameScanGeometry.isInDispenserWallPlane(Direction.EAST, DISPENSER, new BlockPos(13, 64, 11)));
    }

    @Test
    void rejectsFramesDirectlyInFrontOrBehindAsNeitherSide() {
        assertFalse(FrameScanGeometry.isViewerSide(Direction.NORTH, DISPENSER, DISPENSER));
        assertFalse(FrameScanGeometry.isViewerSide(Direction.SOUTH, DISPENSER, DISPENSER));
        assertFalse(FrameScanGeometry.isViewerSide(Direction.EAST, DISPENSER, DISPENSER));
        assertFalse(FrameScanGeometry.isViewerSide(Direction.WEST, DISPENSER, DISPENSER));
    }

    private static void assertViewerAndHost(Direction facing, BlockPos viewer, BlockPos host) {
        assertTrue(FrameScanGeometry.isViewerSide(facing, DISPENSER, viewer), facing + " viewer side");
        assertFalse(FrameScanGeometry.isViewerSide(facing, DISPENSER, host), facing + " host must not be viewer");
    }
}
