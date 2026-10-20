package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Substitutes only the local map texture's state argument.
 * The real MapIdComponent and the ItemStack are never changed.
 */
@Mixin(targets = "net.minecraft.client.texture.MapTextureManager")
public abstract class MapTextureManagerMixin {
    @ModifyArgs(
            method = "getTextureId",
            at = @At("HEAD")
    )
    private void blackjack$useHostDisplayState(Args args) {
        // Minecraft 1.21.11 signature: getTextureId(MapIdComponent, MapState).
        MapIdComponent actualMapId = (MapIdComponent) args.get(0);
        MapState actualState = (MapState) args.get(1);
        MapState simulated = MapSimulationStore.resolve(actualMapId);
        if (simulated != null && simulated != actualState) {
            args.set(1, simulated);
        }
    }
}
