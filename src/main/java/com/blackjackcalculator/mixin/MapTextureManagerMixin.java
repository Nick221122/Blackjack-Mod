package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Substitutes only the local texture source; the ItemStack's real Map ID is unchanged. */
@Mixin(targets = "net.minecraft.client.texture.MapTextureManager")
public abstract class MapTextureManagerMixin {
    @ModifyVariable(
            method = "getTextureId",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private MapState blackjack$useHostDisplayState(MapState actualState, MapIdComponent mapId) {
        MapState simulated = MapSimulationStore.resolve(mapId);
        return simulated != null ? simulated : actualState;
    }
}
