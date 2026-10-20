package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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
        MapState simulatedState = MapSimulationStore.resolve(mapId);
        return simulatedState != null ? simulatedState : actualState;
    }

    /**
     * Resolve the texture ID at draw time as well as replacing update state.
     * MapRenderer caches textures, so swapping only the MapState during update
     * does not reliably refresh a map already visible in an inventory screen
     * when the simulation toggle changes. Redirecting the lookup makes every
     * normal map render path (including inventory slots) use the selected
     * local representation without modifying the ItemStack or server state.
     */
    @ModifyVariable(
            method = "getMapTexture",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MapIdComponent blackjack$useSimulatedTextureId(MapIdComponent actualMapId) {
        if (actualMapId == null) return null;
        Integer displayId = MapSimulationStore.displayMapId(actualMapId.id());
        return displayId == null ? actualMapId : new MapIdComponent(displayId);
    }
}
