package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(
            method = "getTooltip",
            at = @At("RETURN")
    )
    private void blackjack$appendSimulationValue(
            Item.TooltipContext context,
            PlayerEntity player,
            TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        if (!MapSimulationStore.isEnabled()) return;

        ItemStack stack = (ItemStack) (Object) this;
        MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
        if (mapId == null) return;

        Integer fakeValue = MapSimulationStore.resolveValue(mapId);
        if (fakeValue != null) {
            List<Text> tooltip = new ArrayList<>(cir.getReturnValue());
            tooltip.add(Text.literal("Blackjack value: " + fakeValue));
            cir.setReturnValue(tooltip);
        }
    }
}
