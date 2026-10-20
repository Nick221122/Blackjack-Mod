package com.blackjackcalculator;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapIdSignatureTest {
    @Test
    void usesStableNumericMapIdForCountingLookup() {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponentTypes.MAP_ID, new MapIdComponent(42));

        assertEquals("42", BlackjackCalculatorClient.mapIdSignature(map));
    }

    @Test
    void returnsNullForStacksWithoutMapIds() {
        assertNull(BlackjackCalculatorClient.mapIdSignature(new ItemStack(Items.STICK)));
    }
}
