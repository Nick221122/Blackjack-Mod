package com.blackjackcalculator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameScanGeometryTest {
    private static final BlockPos DISPENSER = new BlockPos(0, 64, 0);

    @Test
    void recognizesOnlyTwoColumnsBesideEachSideAndOddRowsAboveDispenser() {
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(-2, 65, 0)));
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(-3, 71, 1)));
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 73, -1)));

        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(-1, 65, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(4, 65, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 66, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 64, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 75, 0)));
    }

    @Test
    void northFacingDispenserKeepsViewerRightAndHostLeft() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.NORTH, DISPENSER, new BlockPos(2, 65, 0)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.NORTH, DISPENSER, new BlockPos(-2, 65, 0)));
    }

    @Test
    void southFacingDispenserKeepsViewerRightAndHostLeft() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.SOUTH, DISPENSER, new BlockPos(-2, 65, 0)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.SOUTH, DISPENSER, new BlockPos(2, 65, 0)));
    }

    @Test
    void eastAndWestFacingDispenserSplitAcrossZ() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.EAST, DISPENSER, new BlockPos(0, 65, 2)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.EAST, DISPENSER, new BlockPos(0, 65, -2)));
        assertTrue(FrameScanGeometry.isViewerSide(Direction.WEST, DISPENSER, new BlockPos(0, 65, -2)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.WEST, DISPENSER, new BlockPos(0, 65, 2)));
    }

    @Test
    void framesMustBeOnWallPlane() {
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 65, 1)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 65, 2)));
    }

    @Test
    void verticalFacingDoesNotQualify() {
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.UP, DISPENSER, new BlockPos(2, 65, 0)));
        assertFalse(FrameScanGeometry.isViewerSide(Direction.UP, DISPENSER, new BlockPos(2, 65, 0)));
    }
}
