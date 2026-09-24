package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    static boolean shouldUseSimulationState(boolean fakeStatePresent) {
        return fakeStatePresent;
    }

    @Inject(method = "getMapState", at = @At("HEAD"), cancellable = true)
    private void blackjack$useSimulatedMapState(
            MapIdComponent mapId,
            CallbackInfoReturnable<MapState> cir
    ) {
        MapState fakeState = MapSimulationStore.resolve(mapId);
        if (shouldUseSimulationState(fakeState != null)) {
            cir.setReturnValue(fakeState);
        }
    }
}
