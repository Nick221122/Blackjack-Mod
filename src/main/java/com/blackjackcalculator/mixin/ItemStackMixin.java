package com.blackjackcalculator.mixin;

import com.blackjackcalculator.MapSimulationStore;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void blackjack$appendSimulatedValue(
            net.minecraft.item.Item.TooltipContext context,
            net.minecraft.entity.player.PlayerEntity player,
            net.minecraft.item.tooltip.TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        MapIdComponent mapId = ((ItemStack) (Object) this).get(DataComponentTypes.MAP_ID);
        Integer fakeValue = MapSimulationStore.resolveGuiValue(mapId);
        if (fakeValue != null) {
            cir.getReturnValue().add(Text.literal("Blackjack value: " + fakeValue));
        }
    }
}
