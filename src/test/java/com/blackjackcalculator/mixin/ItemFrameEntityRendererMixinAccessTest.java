package com.blackjackcalculator.mixin;

import com.blackjackcalculator.BlackjackCalculatorClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFrameEntityRendererMixinAccessTest {
    @Test
    void rendererMixinCanUseTheSelectedFrameSimulationPredicate() {
        assertTrue(BlackjackCalculatorClient.shouldSimulateSelectedFrame(true, true));
    }

    @Test
    void worldMapLookupUsesTheFakeStateWhenSimulationProvidesOne() {
        assertTrue(ClientWorldMixin.shouldUseSimulationState(true));
    }
}
