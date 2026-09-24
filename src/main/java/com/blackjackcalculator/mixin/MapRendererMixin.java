package com.blackjackcalculator.mixin;

import com.blackjackcalculator.BlackjackCalculatorClient;
import com.blackjackcalculator.MapSimulationRenderContext;
import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.render.MapRenderer")
public abstract class MapRendererMixin {
    @ModifyVariable(
            method = "update",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MapState blackjack$replaceMapState(MapState actualState, MapIdComponent mapId) {
        ItemFrameEntity frame = MapSimulationRenderContext.currentFrame();
        MapState simulatedState = MapSimulationStore.resolve(mapId);
        return MapSimulationStore.shouldSimulateForFrame(
                MapSimulationStore.isEnabled(),
                BlackjackCalculatorClient.isSelectedMapFrame(frame),
                simulatedState != null)
                ? simulatedState
                : actualState;
    }

}
