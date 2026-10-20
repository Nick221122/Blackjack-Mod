package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Supplies local simulated map pixels to the texture manager while preserving
 * the real map ID and ItemStack. This covers item rendering paths that request
 * map textures directly, including inventory slots.
 */
@Mixin(targets = "net.minecraft.client.texture.MapTextureManager")
public abstract class MapTextureManagerMixin {
    @ModifyVariable(
            method = "getTextureId",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MapState blackjack$useSimulatedMapState(MapState actualState, MapIdComponent mapId) {
        MapState simulatedState = MapSimulationStore.resolve(mapId);
        return simulatedState != null ? simulatedState : actualState;
    }
}
