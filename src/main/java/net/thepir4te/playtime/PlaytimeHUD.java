package net.thepir4te.playtime;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

@Environment(EnvType.CLIENT)
public class PlaytimeHUD {
    public static void register() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("playtime-tracker", "hud"),
            (graphics, deltaTracker) -> {
                Minecraft client = Minecraft.getInstance();

                if (client.getDebugOverlay().showDebugScreen()) {
                    return;
                }

                PlaytimeConfig cfg = PlaytimeTrackerClient.config;
                if (cfg != null) {
                    float scale = cfg.scale;
                    Matrix3x2fStack pose = graphics.pose();
                    pose.pushMatrix();
                    pose.translate((float)cfg.hudX, (float)cfg.hudY);
                    pose.scale(scale, scale);

                    int currentY = 0;
                    if (cfg.showSession) {
                        graphics.text(client.font, "Session: " + PlaytimeManager.format(PlaytimeManager.sessionSeconds), 0, currentY, cfg.colorSession | 0xFF000000, true);
                        currentY += 10;
                    }

                    if (cfg.showWorld) {
                        graphics.text(client.font, "World: " + PlaytimeManager.format(PlaytimeManager.worldSeconds), 0, currentY, cfg.colorWorld | 0xFF000000, true);
                        currentY += 10;
                    }

                    if (cfg.showOverall) {
                        graphics.text(client.font, "Overall: " + PlaytimeManager.format(PlaytimeManager.getEffectiveOverallSeconds()), 0, currentY, cfg.colorOverall | 0xFF000000, true);
                    }

                    pose.popMatrix();
                }
            }
        );
    }
}
