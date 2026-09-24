package com.blackjackcalculator.mixin;

import com.blackjackcalculator.BlackjackCalculatorClient;
import com.blackjackcalculator.MapSimulationRenderContext;
import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.render.MapRenderer")
public abstract class MapRendererMixin {
    @ModifyArg(
            method = "update",
            at = @At("HEAD"),
            index = 1
    )
    private MapState blackjack$replaceMapState(MapState actualState) {
        ItemFrameEntity frame = MapSimulationRenderContext.currentFrame();
        if (!MapSimulationStore.isEnabled()
                || !BlackjackCalculatorClient.isSelectedMapFrame(frame)
                || frame == null) {
            return actualState;
        }

        ItemStack stack = frame.getHeldItemStack();
        if (stack.isEmpty()) return actualState;

        MapState simulatedState = MapSimulationStore.resolve(
                stack.get(DataComponentTypes.MAP_ID)
        );
        return simulatedState != null ? simulatedState : actualState;
    }
}
