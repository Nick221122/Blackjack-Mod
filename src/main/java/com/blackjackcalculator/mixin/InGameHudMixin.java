package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationHud;
import com.blackjackcalculator.MapSimulationStore;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud")
public abstract class InGameHudMixin {
    @Inject(
            method = "drawHeart",
            at = @At("HEAD"),
            cancellable = true
    )
    private void blackjack$drawSimulatedHeart(
            DrawContext context,
            @Coerce Object type,
            int x,
            int y,
            boolean hardcore,
            boolean blinking,
            boolean half,
            CallbackInfo ci) {
        if (!MapSimulationStore.isEnabled()) return;
        if (!(type instanceof Enum<?> heartType) || !"CONTAINER".equals(heartType.name())) return;
        if (!MapSimulationHud.claimHeartSuppression(true, true)) return;

        Identifier texture = ((HeartTypeAccessor) type).blackjack$getTexture(hardcore, half, blinking);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        int size = MapSimulationHud.HEART_TEXTURE_SIZE;
        int removedX = MapSimulationHud.SUPPRESSED_HEART_PIXEL_X;
        int removedY = MapSimulationHud.SUPPRESSED_HEART_PIXEL_Y;

        if (removedY > 0) {
            context.drawGuiTexture(pipeline, texture, size, size, 0, 0, x, y, size, removedY);
        }
        if (removedX > 0) {
            context.drawGuiTexture(pipeline, texture, size, size, 0, removedY, x, y + removedY, removedX, size - removedY);
        }

        int rightStart = removedX + 1;
        if (rightStart < size) {
            context.drawGuiTexture(
                    pipeline,
                    texture,
                    size,
                    size,
                    rightStart,
                    removedY,
                    x + rightStart,
                    y + removedY,
                    size - rightStart,
                    size - removedY
            );
        }
        ci.cancel();
    }
    @Inject(
            method = "renderStatusBars",
            at = @At("HEAD")
    )
    private void blackjack$resetHeartSuppression(CallbackInfo ci) {
        MapSimulationHud.resetHeartSuppression();
    }

}
