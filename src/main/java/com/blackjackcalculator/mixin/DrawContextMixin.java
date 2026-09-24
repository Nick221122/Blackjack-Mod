package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Inject(
            method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void blackjack$beginFakeMapRender(
            LivingEntity entity,
            World world,
            ItemStack stack,
            int x,
            int y,
            int seed,
            CallbackInfo ci
    ) {
        if (MapSimulationStore.hasGuiSimulationForMap(
                stack.get(net.minecraft.component.DataComponentTypes.MAP_ID))) {
            MapSimulationStore.beginGuiMapRender();
        }
    }

    @Inject(
            method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("RETURN")
    )
    private void blackjack$endFakeMapRender(
            LivingEntity entity,
            World world,
            ItemStack stack,
            int x,
            int y,
            int seed,
            CallbackInfo ci
    ) {
        if (MapSimulationStore.hasGuiSimulationForMap(
                stack.get(net.minecraft.component.DataComponentTypes.MAP_ID))) {
            MapSimulationStore.endGuiMapRender();
        }
    }
}
