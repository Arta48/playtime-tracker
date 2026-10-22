package net.thepir4te.playtime.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.thepir4te.playtime.PlaytimeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void drawPlaytime(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.font != null) {
            long seconds = PlaytimeManager.getEffectiveOverallSeconds();
            Component text = seconds <= 0L
                ? Component.translatable("text.playtime.none")
                : Component.translatable("text.playtime.total", PlaytimeManager.format(seconds));

            int guiWidth = graphics.guiWidth();
            int guiHeight = graphics.guiHeight();
            int x = guiWidth - client.font.width(text) - 5;
            int y = guiHeight - 19;

            graphics.text(client.font, text, x, y, 0xFFFFFFFF, true);
        }
    }
}
