package com.blackjackcalculator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameScanGeometryTest {
    private static final BlockPos DISPENSER = new BlockPos(0, 64, 0);

    @Test
    void northFacingDispenserUsesWestAsViewerLeftAndEastAsHostRight() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.NORTH, DISPENSER, new BlockPos(-1, 64, 0)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 0)));
    }

    @Test
    void southFacingDispenserUsesEastAsViewerLeftAndWestAsHostRight() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.SOUTH, DISPENSER, new BlockPos(1, 64, 0)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.SOUTH, DISPENSER, new BlockPos(-1, 64, 0)));
    }

    @Test
    void eastAndWestFacingDispenserSplitAcrossZ() {
        assertTrue(FrameScanGeometry.isViewerSide(Direction.EAST, DISPENSER, new BlockPos(0, 64, -1)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.EAST, DISPENSER, new BlockPos(0, 64, 1)));
        assertTrue(FrameScanGeometry.isViewerSide(Direction.WEST, DISPENSER, new BlockPos(0, 64, 1)));
        assertTrue(FrameScanGeometry.isHostSide(Direction.WEST, DISPENSER, new BlockPos(0, 64, -1)));
    }

    @Test
    void framesMustBeWithinOneBlockOfTheDispenserWallPlane() {
        assertTrue(FrameScanGeometry.isInDispenserWallPlane(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 1)));
        assertFalse(FrameScanGeometry.isInDispenserWallPlane(Direction.NORTH, DISPENSER, new BlockPos(1, 64, 2)));
    }

    @Test
    void verticalFacingDoesNotQualify() {
        assertFalse(FrameScanGeometry.isInDispenserWallPlane(Direction.UP, DISPENSER, new BlockPos(1, 64, 0)));
        assertFalse(FrameScanGeometry.isViewerSide(Direction.UP, DISPENSER, new BlockPos(1, 64, 0)));
    }
}
