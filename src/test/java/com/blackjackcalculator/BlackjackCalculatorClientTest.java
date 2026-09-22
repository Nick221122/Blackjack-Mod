package com.blackjackcalculator;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackjackCalculatorClientTest {
    @Test
    void clientInitializerImplementsFabricClientModInitializer() {
        assertTrue(ClientModInitializer.class.isAssignableFrom(BlackjackCalculatorClient.class));
    }

    @Test
    void resetHudRestoresDefaultHostAndViewerNames() {
        BlackjackConfig.setHostName("Custom Host");
        BlackjackConfig.setViewerName("Custom Viewer");

        BlackjackConfig.resetHudDefaults();

        assertEquals("Host", BlackjackConfig.getHostName());
        assertEquals("Viewer", BlackjackConfig.getViewerName());
    }

    @Test
    void resetHudResolvesHostToRightAndViewerToLeft() {
        assertEquals(142, BlackjackCalculatorClient.resolveHudX(true, -1, 50, 200));
        assertEquals(8, BlackjackCalculatorClient.resolveHudX(false, 8, 50, 200));
    }
}
