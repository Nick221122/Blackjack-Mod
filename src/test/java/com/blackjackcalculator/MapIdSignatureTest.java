package com.blackjackcalculator;

import net.minecraft.component.type.MapIdComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapIdSignatureTest {
    @Test
    void usesStableNumericMapIdForCountingLookup() {
        assertEquals("42", BlackjackCalculatorClient.mapIdSignature(new MapIdComponent(42)));
    }

    @Test
    void returnsNullForMissingMapId() {
        assertNull(BlackjackCalculatorClient.mapIdSignature((MapIdComponent) null));
    }
}
