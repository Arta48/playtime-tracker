package net.thepir4te.playtime;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class PlaytimeTrackerClient implements ClientModInitializer {
    public static PlaytimeConfig config;
    public static KeyMapping.Category CATEGORY;
    private static KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        config = PlaytimeConfig.load();
        if (config == null) {
            config = new PlaytimeConfig();
        }

        PlaytimeManager.init();

        PlaytimeManager.milestoneCallback = (hours) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.gui != null) {
                Component message = Component.translatable("playtime-tracker.milestone.message", hours);
                client.gui.setTitle(Component.translatable("playtime-tracker.milestone.title", hours));
                client.gui.setSubtitle(message);
                client.gui.setTimes(10, 70, 20);
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
            }
        };

        PlaytimeHUD.register();

        CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("playtime-tracker", "main"));

        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.playtime.settings",
            GLFW.GLFW_KEY_K,
            CATEGORY
        ));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    client.execute(PlaytimeManager::loadCurrentWorldTime);
                } catch (InterruptedException ignored) {}
            }).start();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlaytimeManager.resetWorldTime();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client != null) {
                while (configKey.consumeClick()) {
                    if (client.player != null) {
                        client.setScreenAndShow(new PlaytimeConfigScreen(config));
                    }
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            PlaytimeManager.stop();
            if (config != null) {
                config.save();
            }
        });
    }
}
