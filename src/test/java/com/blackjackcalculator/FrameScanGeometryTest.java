package com.blackjackcalculator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameScanGeometryTest {
    private static final BlockPos DISPENSER = new BlockPos(0, 64, 0);

    @Test
    void recognizesTwoColumnsAndFourRowsShownInLayout() {
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(-1, 64, 0)));
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(-2, 67, 1)));
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 65, -1)));
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 66, 0)));

        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(0, 64, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(-3, 64, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 68, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 63, 0)));
    }

    @Test
    void supportsSmallerLayoutsWithoutExpandingThePermittedArea() {
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 0)));
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 65, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(3, 65, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 68, 0)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(2, 65, 2)));
    }

    @Test
    void northFacingDispenserKeepsViewerRightAndHostLeft() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 0)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.NORTH, DISPENSER, new BlockPos(-1, 64, 0)));
    }

    @Test
    void southFacingDispenserKeepsViewerRightAndHostLeft() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.SOUTH, DISPENSER, new BlockPos(-1, 64, 0)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.SOUTH, DISPENSER, new BlockPos(1, 64, 0)));
    }

    @Test
    void eastAndWestFacingDispenserSplitAcrossZ() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.EAST, DISPENSER, new BlockPos(0, 64, 1)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.EAST, DISPENSER, new BlockPos(0, 64, -1)));
        assertTrue(FrameScanGeometry.isViewerSide(Direction.WEST, DISPENSER, new BlockPos(0, 64, -1)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.WEST, DISPENSER, new BlockPos(0, 64, 1)));
    }

    @Test
    void framesMustBeOnWallPlane() {
        assertTrue(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 1)));
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 2)));
    }

    @Test
    void verticalFacingDoesNotQualify() {
        assertFalse(FrameScanGeometry.isInExpectedLayout(Direction.UP, DISPENSER, new BlockPos(1, 64, 0)));
        assertFalse(FrameScanGeometry.isViewerSide(Direction.UP, DISPENSER, new BlockPos(1, 64, 0)));
    }
}
