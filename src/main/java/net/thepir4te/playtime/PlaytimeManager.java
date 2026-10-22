package net.thepir4te.playtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PlaytimeManager {
    private static final Path OVERALL_FILE = FabricLoader.getInstance().getConfigDir().resolve("playtime-overall.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static long overallSeconds = 0L;
    public static long worldSeconds = 0L;
    public static long sessionSeconds = 0L;
    public static Consumer<Long> milestoneCallback = null;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PlaytimeTracker-Ticker");
        thread.setDaemon(true);
        return thread;
    });

    public static void init() {
        loadOverall();

        if (!PlaytimeTrackerClient.config.legacyWorldsImported) {
            importLegacySaves();
            PlaytimeTrackerClient.config.legacyWorldsImported = true;
            PlaytimeTrackerClient.config.save();
            saveOverall();
        }

        SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client == null) {
                    return;
                }

                sessionSeconds++;

                boolean inWorld = (client.level != null);
                if (inWorld) {
                    worldSeconds++;
                    overallSeconds++;
                } else if (PlaytimeTrackerClient.config.countMenuTime) {
                    overallSeconds++;
                }

                long totalEffective = getEffectiveOverallSeconds();
                if (totalEffective > 0 && totalEffective % 3600 == 0) {
                    long hours = totalEffective / 3600;
                    if ((hours == 10 || hours == 50 || hours == 100 || hours == 500 || hours == 1000) && milestoneCallback != null) {
                        milestoneCallback.accept(hours);
                    }
                }
            } catch (Exception ignored) {}
        }, 1, 1, TimeUnit.SECONDS);

        SCHEDULER.scheduleAtFixedRate(PlaytimeManager::saveOverall, 30, 30, TimeUnit.SECONDS);
    }

    public static void stop() {
        saveOverall();
        SCHEDULER.shutdownNow();
    }

    public static void loadCurrentWorldTime() {
        Minecraft client = Minecraft.getInstance();
        worldSeconds = 0L;

        if (client.getSingleplayerServer() != null) {
            var server = client.getSingleplayerServer();

            try {
                if (client.player != null) {
                    var playerList = server.getPlayerList();
                    var serverPlayer = playerList.getPlayer(client.player.getUUID());

                    if (serverPlayer != null) {
                        var stats = serverPlayer.getStats();
                        long ticks = stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME));

                        if (ticks > 0) {
                            worldSeconds = ticks / 20L;
                            System.out.println("[PlaytimeTracker] Successfully loaded world playtime from server memory: " + format(worldSeconds));
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[PlaytimeTracker] Failed to read from server memory, falling back to files: " + e.getMessage());
            }

            try {
                Path worldFolder = server.getWorldPath(LevelResource.ROOT).toRealPath();
                Path statsDir = worldFolder.resolve("stats");

                if (!Files.exists(statsDir)) {
                    worldFolder = client.gameDirectory.toPath().resolve("saves").resolve(server.getWorldData().getLevelName());
                    statsDir = worldFolder.resolve("stats");
                }

                if (Files.exists(statsDir)) {
                    UUID uuid = client.getUser() != null ? client.getUser().getProfileId() : null;
                    if (uuid != null) {
                        Path statFile = statsDir.resolve(uuid + ".json");
                        if (Files.exists(statFile)) {
                            long ticks = extractTicksFromStat(statFile);
                            worldSeconds = ticks / 20L;
                            System.out.println("[PlaytimeTracker] Loaded world playtime from file: " + format(worldSeconds));
                            return;
                        }
                    }

                    try (Stream<Path> files = Files.list(statsDir)) {
                        for (Path file : (Iterable<Path>) files::iterator) {
                            if (file.toString().endsWith(".json")) {
                                long ticks = extractTicksFromStat(file);
                                if (ticks > 0) {
                                    worldSeconds = ticks / 20L;
                                    System.out.println("[PlaytimeTracker] Loaded world playtime from fallback file: " + format(worldSeconds));
                                    return;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[PlaytimeTracker] Error loading world stats from file: " + e.getMessage());
            }
        }
    }

    public static void resetWorldTime() {
        worldSeconds = 0L;
    }

    private static void importLegacySaves() {
        Minecraft client = Minecraft.getInstance();
        if (client.getUser() == null) {
            return;
        }

        UUID uuid = client.getUser().getProfileId();
        if (uuid == null) {
            return;
        }

        Path savesDir = client.gameDirectory.toPath().resolve("saves");
        if (!Files.exists(savesDir)) {
            return;
        }

        long totalTicks = 0;
        String uuidStr = uuid.toString();

        try (Stream<Path> stream = Files.list(savesDir)) {
            for (Path worldDir : (Iterable<Path>) stream::iterator) {
                Path statFile = worldDir.resolve("stats").resolve(uuidStr + ".json");
                if (Files.exists(statFile)) {
                    totalTicks += extractTicksFromStat(statFile);
                }
            }
        } catch (Exception e) {
            System.err.println("[PlaytimeTracker] Error importing old saves: " + e.getMessage());
        }

        long importedSeconds = totalTicks / 20L;
        overallSeconds += importedSeconds;
    }

    private static long extractTicksFromStat(Path path) {
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("stats")) {
                JsonObject stats = obj.getAsJsonObject("stats");
                if (stats.has("minecraft:custom")) {
                    JsonObject custom = stats.getAsJsonObject("minecraft:custom");
                    if (custom.has("minecraft:play_time")) {
                        return custom.get("minecraft:play_time").getAsLong();
                    }
                }
            }
            if (obj.has("stat.playOneMinute")) {
                return obj.get("stat.playOneMinute").getAsLong();
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    public static long getEffectiveOverallSeconds() {
        long offset = (PlaytimeTrackerClient.config != null) ? PlaytimeTrackerClient.config.manualOffsetHours * 3600L : 0L;
        return overallSeconds + offset;
    }

    public static void loadOverall() {
        try {
            if (Files.exists(OVERALL_FILE)) {
                try (Reader reader = Files.newBufferedReader(OVERALL_FILE)) {
                    Long loaded = GSON.fromJson(reader, Long.class);
                    overallSeconds = (loaded != null) ? loaded : 0L;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveOverall() {
        try {
            Files.createDirectories(OVERALL_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(OVERALL_FILE)) {
                GSON.toJson(overallSeconds, writer);
            }
        } catch (Exception ignored) {}
    }

    public static String format(long totalSeconds) {
        long h = totalSeconds / 3600L;
        long m = (totalSeconds % 3600L) / 60L;
        long s = totalSeconds % 60L;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }
}
