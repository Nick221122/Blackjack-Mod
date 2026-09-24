package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationRenderContext;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void blackjack$setRenderFrame(ItemFrameEntity frame, ItemFrameEntityRenderState state, float tickDelta, CallbackInfo ci) {
        MapSimulationRenderContext.set(frame);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void blackjack$clearRenderFrame(ItemFrameEntity frame, ItemFrameEntityRenderState state, float tickDelta, CallbackInfo ci) {
        MapSimulationRenderContext.clear();
    }
}
