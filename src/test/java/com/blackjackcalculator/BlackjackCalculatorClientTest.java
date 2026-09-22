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
    void scanIsAllowedWhenDispenserScreenIsOpenWithoutBlockHitTarget() {
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Dispenser", null));
    }

    @Test
    void scanIsAllowedWhenDropperScreenIsOpen() {
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Dropper", null));
    }

    @Test
    void differentMapIdsRemainDistinctScanKeys() {
        ItemStack first = new ItemStack(Items.FILLED_MAP);
        first.set(DataComponentTypes.MAP_ID, new MapIdComponent(101));
        ItemStack second = new ItemStack(Items.FILLED_MAP);
        second.set(DataComponentTypes.MAP_ID, new MapIdComponent(202));

        assertEquals(first.get(DataComponentTypes.MAP_ID).toString(), BlackjackCalculatorClient.mapIdSignature(first));
        assertEquals(second.get(DataComponentTypes.MAP_ID).toString(), BlackjackCalculatorClient.mapIdSignature(second));
        assertFalse(BlackjackCalculatorClient.mapIdSignature(first).equals(BlackjackCalculatorClient.mapIdSignature(second)));
    }

    @Test
    void scanDoesNotRequireAWorldBlockHitWhenContainerScreenIsOpen() {
        assertTrue(BlackjackCalculatorClient.shouldScanOpenContainer("Dispenser", null));
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
