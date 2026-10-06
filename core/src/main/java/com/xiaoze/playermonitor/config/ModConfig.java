package com.xiaoze.playermonitor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xiaoze.playermonitor.PlayerMonitor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side configuration. Persisted as JSON in the Fabric config directory.
 * The password is stored as a SHA-256 hex digest only, never in plain text.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "playermonitor.json";

    /** SHA-256 hex digest of the verification password. Empty means unset. */
    public String passwordHash = "";

    /** When true, remote actions are rejected unless the admin has verified. */
    public boolean requireAuthForActions = true;

    /** Send a fresh report automatically when a player joins. */
    public boolean reportOnJoin = true;

    /** Collect the running process list (may be restricted by the OS). */
    public boolean collectProcesses = true;

    /** Collect the clipboard contents (may contain sensitive data). */
    public boolean collectClipboard = true;

    /** Maximum number of running processes to include in a report. */
    public int maxProcessCount = 200;

    /** Mask obvious personal data (home directory, host name) inside report strings. */
    public boolean maskPersonalData = false;

    public static Path filePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static ModConfig load() {
        Path path = filePath();
        try {
            if (!Files.exists(path)) {
                ModConfig fresh = new ModConfig();
                fresh.save();
                return fresh;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                return loaded == null ? new ModConfig() : loaded;
            }
        } catch (Exception e) {
            PlayerMonitor.LOGGER.error("[PlayerMonitor] failed to load config, using defaults", e);
            return new ModConfig();
        }
    }

    public void save() {
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            PlayerMonitor.LOGGER.error("[PlayerMonitor] failed to save config", e);
        }
    }
}
