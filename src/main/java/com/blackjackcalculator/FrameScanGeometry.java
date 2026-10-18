package com.blackjackcalculator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Coordinate rules for assigning dispenser-adjacent item frames to the player's left/right. */
final class FrameScanGeometry {
    private FrameScanGeometry() {}

    static boolean isInDispenserWallPlane(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        if (facing == null || !facing.getAxis().isHorizontal()) return false;
        int normalDistance = facing.getAxis() == Direction.Axis.Z
                ? Math.abs(framePos.getZ() - dispenserPos.getZ())
                : Math.abs(framePos.getX() - dispenserPos.getX());
        return normalDistance <= 1;
    }

    static boolean isViewerSide(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        int side = sideOfDispenser(facing, dispenserPos, framePos);
        if (side == 0) return false;
        return facing == Direction.NORTH || facing == Direction.EAST ? side > 0 : side < 0;
    }

    static boolean isHostSide(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        int side = sideOfDispenser(facing, dispenserPos, framePos);
        if (side == 0) return false;
        return facing == Direction.NORTH || facing == Direction.EAST ? side < 0 : side > 0;
    }

    private static int sideOfDispenser(Direction facing, BlockPos dispenserPos, BlockPos framePos) {
        if (!isInDispenserWallPlane(facing, dispenserPos, framePos)) return 0;
        boolean splitAlongX = facing.getAxis() == Direction.Axis.Z;
        return splitAlongX
                ? Integer.compare(framePos.getX(), dispenserPos.getX())
                : Integer.compare(framePos.getZ(), dispenserPos.getZ());
    }
}
