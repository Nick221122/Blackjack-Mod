package com.blackjackcalculator;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackjackCalculatorClientTest {
    @Test
    void scanIsAllowedWhenDispenserScreenIsOpenWithoutBlockHitTarget() {
        Screen dispenserScreen = new Screen(Text.literal("Dispenser")) {
        };

        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer(dispenserScreen, null));
    }
}
