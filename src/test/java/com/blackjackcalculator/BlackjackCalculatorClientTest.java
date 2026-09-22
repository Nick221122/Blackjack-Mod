package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackjackCalculatorClientTest {
    @Test
    void scanIsAllowedWhenDispenserScreenIsOpenWithoutBlockHitTarget() {
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Dispenser", null));
    }
}
