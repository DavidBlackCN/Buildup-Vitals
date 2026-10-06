package com.davidblackcn.buildupvitals.client.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Add a short explanation on hover in either compact or expanded vanilla effect cards. */
@Mixin(EffectsInInventory.class)
public abstract class EffectDescriptionMixin {
    @Inject(method = "extractText(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;IIIIII)V",
            at = @At("TAIL"), require = 1, allow = 1)
    private void buildupVitals$description(GuiGraphicsExtractor graphics, Component name, Component duration, Font font,
            int x, int y, int width, int step, int mouseX, int mouseY, CallbackInfo ci) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + step
                && name.getContents() instanceof TranslatableContents text && text.getKey().startsWith("effect.buildup_vitals.")) {
            graphics.setTooltipForNextFrame(font, List.of(name, duration,
                    Component.translatable(text.getKey() + ".description").withStyle(ChatFormatting.GRAY)), Optional.empty(), mouseX, mouseY);
        }
    }
}
