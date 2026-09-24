package com.blackjackcalculator.mixin;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud$HeartType")
public interface HeartTypeAccessor {
    @Invoker("getTexture")
    Identifier blackjack$getTexture(boolean hardcore, boolean half, boolean blinking);
}
