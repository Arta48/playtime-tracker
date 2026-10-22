package net.thepir4te.playtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlaytimeConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("playtime-config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int hudX = 20;
    public int hudY = 20;
    public float scale = 1.0F;
    public float relX = 0.05F;
    public float relY = 0.05F;
    public boolean showSession = true;
    public boolean showWorld = true;
    public boolean showOverall = true;
    public int colorSession = 0x55FFFF;
    public int colorWorld   = 0x55FF55;
    public int colorOverall = 0xFFAA00;

    public boolean legacyWorldsImported = false;
    public boolean countMenuTime = true;
    public long manualOffsetHours = 0L;

    public static PlaytimeConfig load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader reader = Files.newBufferedReader(FILE)) {
                    PlaytimeConfig loaded = GSON.fromJson(reader, PlaytimeConfig.class);
                    return loaded != null ? loaded : new PlaytimeConfig();
                }
            }
        } catch (Exception e) {
            System.err.println("[PlaytimeTracker] Could not load config, using defaults.");
        }
        return new PlaytimeConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[PlaytimeTracker] Could not save config!");
            e.printStackTrace();
        }
    }
}
