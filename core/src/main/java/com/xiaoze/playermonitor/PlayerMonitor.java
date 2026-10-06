package com.xiaoze.playermonitor;

import com.xiaoze.playermonitor.config.ModConfig;
import com.xiaoze.playermonitor.network.ModPackets;
import com.xiaoze.playermonitor.server.MonitorCommand;
import com.xiaoze.playermonitor.server.ServerLifecycle;
import com.xiaoze.playermonitor.server.ServerNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (main) entrypoint. Registers payload types, server receivers, commands and lifecycle hooks.
 * This runs on both the dedicated server and the integrated server.
 */
public class PlayerMonitor implements ModInitializer {
    public static final String MOD_ID = "playermonitor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config = new ModConfig();

    @Override
    public void onInitialize() {
        config = ModConfig.load();

        ModPackets.register();
        ServerNetworking.register();
        MonitorCommand.register();
        ServerLifecycle.register();

        LOGGER.info("[PlayerMonitor] common initialization complete.");
    }

    public static ModConfig config() {
        return config;
    }

    public static void setConfig(ModConfig newConfig) {
        config = newConfig;
    }

    /** Creates a namespaced identifier inside the playermonitor namespace. */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
