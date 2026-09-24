package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    /*
     * Hook the public inventory/item rendering entry point. The previous version
     * targeted DrawContext's private world/entity overload, which is an internal
     * implementation detail and can change between 1.21.11 mapping builds.
     */
    @Inject(
            method = "drawItem(Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void blackjack$beginFakeMapRender(
            ItemStack stack,
            int x,
            int y,
            int seed,
            CallbackInfo ci
    ) {
        if (MapSimulationStore.hasGuiSimulationForMap(
                stack.get(DataComponentTypes.MAP_ID))) {
            MapSimulationStore.beginGuiMapRender();
        }
    }

    @Inject(
            method = "drawItem(Lnet/minecraft/item/ItemStack;III)V",
            at = @At("RETURN")
    )
    private void blackjack$endFakeMapRender(
            ItemStack stack,
            int x,
            int y,
            int seed,
            CallbackInfo ci
    ) {
        if (MapSimulationStore.hasGuiSimulationForMap(
                stack.get(DataComponentTypes.MAP_ID))) {
            MapSimulationStore.endGuiMapRender();
        }
    }
}
