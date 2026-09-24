package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @ModifyArgs(method = "update", at = @At("HEAD"))
    private void blackjack$replaceMapState(Args args) {
        MapIdComponent mapId = args.get(0);
        MapState actualState = args.get(1);
        MapRenderState renderState = args.get(2);

        MapState simulatedState = MapSimulationStore.resolve(mapId);
        if (simulatedState != null && actualState != null && renderState != null) {
            args.set(1, simulatedState);
        }
    }
}
