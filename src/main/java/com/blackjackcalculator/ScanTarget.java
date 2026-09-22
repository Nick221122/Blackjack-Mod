package com.blackjackcalculator;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

final class ScanTarget {
    private ScanTarget() {}

    static boolean isDispenserOrDropper(BlockState state) {
        return state != null && (state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER));
    }
}
