package com.blackjackcalculator;

import net.minecraft.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackjackCalculatorClientTest {
    @Test
    void recognizesDispenserAndDropperBlocksAsScanTargets() {
        assertTrue(ScanTarget.isDispenserOrDropper(Blocks.DISPENSER.getDefaultState()));
        assertTrue(BlackjackCalculatorClient.isDispenserOrDropperBlock(Blocks.DROPPER.getDefaultState()));
    }

    @Test
    void doesNotTreatOtherBlocksAsScanTargets() {
        assertFalse(BlackjackCalculatorClient.isDispenserOrDropperBlock(Blocks.CHEST.getDefaultState()));
    }
}
