package com.xiaoze.playermonitor.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small client-side configuration for the administrator GUI, editable through Cloth Config. */
public class GuiConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "playermonitor-gui.json";

    private static GuiConfig instance;

    /** Open the GUI automatically when the server authorizes this client. */
    public boolean autoOpen = true;

    /** Maximum number of flattened report lines shown in the detail view. */
    public int detailMaxLines = 200;

    /** Show the raw JSON report in addition to the flattened view. */
    public boolean showRawJson = false;

    public static GuiConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path filePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static GuiConfig load() {
        Path path = filePath();
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    GuiConfig loaded = GSON.fromJson(reader, GuiConfig.class);
                    if (loaded != null) {
                        return loaded;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to defaults.
        }
        return new GuiConfig();
    }

    public static void save() {
        GuiConfig current = get();
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
        } catch (Exception ignored) {
            // Ignore persistence failures.
        }
    }
}
