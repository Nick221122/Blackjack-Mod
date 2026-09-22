package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackjackCalculatorClientTest {
    @Test
    void scanIsAllowedWhenDispenserScreenIsOpenWithoutBlockHitTarget() {
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Dispenser", null));
    }

    @Test
    void scanIsAllowedWhenDropperOrHopperScreenIsOpen() {
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Dropper", null));
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Hopper", null));
    }

    @Test
    void scanIsRejectedForOtherScreens() {
        assertFalse(BlackjackCalculatorClient.shouldScanOpenContainer("Chest", null));
    }

    @Test
    void containerSlotValuesFollowBlackjackMapOrder() {
        assertEquals(11, BlackjackCalculatorClient.scannedValueForSlot(0));
        assertEquals(6, BlackjackCalculatorClient.scannedValueForSlot(4));
        assertEquals(10, BlackjackCalculatorClient.scannedValueForSlot(8));
    }
}
