package com.blackjackcalculator.mixin;

import com.blackjackcalculator.BlackjackCalculatorClient;
import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameEntityRendererMixin<T extends ItemFrameEntity> {
    @Shadow
    private MapRenderer mapRenderer;

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void blackjack$applySimulation(
            T frame,
            ItemFrameEntityRenderState renderState,
            float tickDelta,
            CallbackInfo ci
    ) {
        boolean selected = BlackjackCalculatorClient.isFrameInSelectedArea(
                net.minecraft.client.MinecraftClient.getInstance(), frame);
        if (!BlackjackCalculatorClient.shouldSimulateSelectedFrame(
                MapSimulationStore.isEnabled(), selected)) {
            return;
        }

        MapState simulatedState = MapSimulationStore.resolve(renderState.mapId);
        if (simulatedState != null) {
            mapRenderer.update(renderState.mapId, simulatedState, renderState.mapRenderState);
        }
    }
}
