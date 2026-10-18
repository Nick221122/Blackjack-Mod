package com.blackjackcalculator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Coordinate rules for the fixed two-column frame layout around a dispenser. */
final class FrameScanGeometry {
    private static final int MIN_SIDE_OFFSET = 1;
    private static final int MAX_SIDE_OFFSET = 2;
    private static final int MIN_VERTICAL_OFFSET = 0;
    private static final int MAX_VERTICAL_OFFSET = 3;

    private FrameScanGeometry() {}

    static boolean isInDispenserWallPlane(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        if (facing == null || !facing.getAxis().isHorizontal()) return false;
        int normalDistance = facing.getAxis() == Direction.Axis.Z
                ? Math.abs(framePos.getZ() - dispenserPos.getZ())
                : Math.abs(framePos.getX() - dispenserPos.getX());
        return normalDistance <= 1;
    }

    static boolean isInExpectedLayout(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        if (!isInDispenserWallPlane(facing, dispenserPos, framePos)) return false;
        int sideOffset = Math.abs(sideOfDispenser(facing, dispenserPos, framePos));
        int verticalOffset = framePos.getY() - dispenserPos.getY();
        return sideOffset >= MIN_SIDE_OFFSET && sideOffset <= MAX_SIDE_OFFSET
                && verticalOffset >= MIN_VERTICAL_OFFSET
                && verticalOffset <= MAX_VERTICAL_OFFSET;
    }

    static boolean isViewerSide(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        int side = sideOfDispenser(facing, dispenserPos, framePos);
        if (!isInExpectedLayout(facing, dispenserPos, framePos) || side == 0) return false;
        // Viewer is on the right when looking in the dispenser's facing direction.
        return facing == Direction.NORTH || facing == Direction.EAST ? side > 0 : side < 0;
    }

    static boolean isHostSide(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        int side = sideOfDispenser(facing, dispenserPos, framePos);
        if (!isInExpectedLayout(facing, dispenserPos, framePos) || side == 0) return false;
        return facing == Direction.NORTH || facing == Direction.EAST ? side < 0 : side > 0;
    }

    private static int sideOfDispenser(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        if (facing == null || !facing.getAxis().isHorizontal()) return 0;
        return facing.getAxis() == Direction.Axis.Z
                ? framePos.getX() - dispenserPos.getX()
                : framePos.getZ() - dispenserPos.getZ();
    }
}
