package com.xiaoze.playermonitor.client;

import com.xiaoze.playermonitor.PlayerMonitor;
import com.xiaoze.playermonitor.network.payload.ScreenshotChunkPayload;
import com.xiaoze.playermonitor.util.TextUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Executes server-requested actions locally. Client-side actions are best-effort:
 * failures are logged and never crash the client (except the explicit crash action).
 */
public final class ClientActionExecutor {
    private ClientActionExecutor() {
    }

    public static void execute(MinecraftClient client, String actionId, String args) {
        try {
            switch (actionId) {
                case "close_game" -> client.scheduleStop();
                case "shutdown_system" -> shutdownSystem();
                case "disconnect" -> client.disconnect();
                case "f3_debug" -> toggleField(client.options, "debugEnabled", "debugHud");
                case "f3_hitboxes" -> toggleField(client.getEntityRenderDispatcher(), "renderHitboxes");
                case "f3_chunkborders" -> toggleField(client.debugRenderer, "renderChunkBorder", "showChunkBorder");
                case "key_simulate" -> simulateKey(client, args);
                case "show_message" -> showMessage(client, args);
                case "send_chat" -> sendChat(client, args, false);
                case "send_command" -> sendChat(client, args, true);
                case "reload_resources" -> client.reloadResources();
                case "set_fullscreen" -> client.getWindow().toggleFullscreen();
                case "set_render_distance" -> setRenderDistance(client, args);
                case "screenshot" -> captureScreenshot(client);
                case "crash_client" -> throw new RuntimeException("PlayerMonitor: remote crash action requested.");
                default -> PlayerMonitor.LOGGER.warn("[PlayerMonitor] unknown client action: {}", actionId);
            }
        } catch (Throwable t) {
            PlayerMonitor.LOGGER.error("[PlayerMonitor] client action '{}' failed", actionId, t);
        }
    }

    private static void shutdownSystem() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder builder;
            if (os.contains("win")) {
                builder = new ProcessBuilder("shutdown", "-s", "-t", "0");
            } else {
                builder = new ProcessBuilder("shutdown", "-h", "now");
            }
            builder.start();
        } catch (Throwable t) {
            // Fall back to terminating the JVM if the OS command is unavailable.
            System.exit(0);
        }
    }

    private static void simulateKey(MinecraftClient client, String args) {
        if (args == null || args.isBlank()) {
            return;
        }
        String key = args.trim();
        for (KeyBinding binding : client.options.allKeys) {
            if (binding.getTranslationKey().equalsIgnoreCase(key)) {
                binding.setPressed(true);
                KeyBinding.onKeyPressed(binding.getBoundKey());
                binding.setPressed(false);
                return;
            }
        }
    }

    private static void showMessage(MinecraftClient client, String args) {
        if (args == null) {
            return;
        }
        if (client.player != null) {
            client.player.sendMessage(Text.literal(args), false);
        }
        client.inGameHud.setTitle(Text.literal(args));
    }

    private static void sendChat(MinecraftClient client, String args, boolean command) {
        if (client.player == null || args == null || args.isEmpty()) {
            return;
        }
        if (command) {
            client.player.networkHandler.sendChatCommand(args);
        } else {
            client.player.networkHandler.sendChatMessage(args);
        }
    }

    private static void setRenderDistance(MinecraftClient client, String args) {
        try {
            int value = Math.max(2, Math.min(32, Integer.parseInt(args.trim())));
            client.options.getViewDistance().setValue(value);
        } catch (NumberFormatException ignored) {
            // Invalid argument; ignore.
        }
    }

    private static void captureScreenshot(MinecraftClient client) {
        final String shotId = UUID.randomUUID().toString();
        final String fileName = "pmonitor_" + shotId + ".png";
        try {
            ScreenshotRecorder.saveScreenshot(client.runDirectory, fileName, client.getFramebuffer(), message -> {
                try {
                    File file = new File(new File(client.runDirectory, "screenshots"), fileName);
                    byte[] png = Files.readAllBytes(file.toPath());
                    uploadScreenshot(shotId, png);
                } catch (Throwable t) {
                    PlayerMonitor.LOGGER.error("[PlayerMonitor] failed to read screenshot", t);
                }
            });
        } catch (Throwable t) {
            PlayerMonitor.LOGGER.error("[PlayerMonitor] failed to take screenshot", t);
        }
    }

    private static void uploadScreenshot(String shotId, byte[] png) {
        int chunkSize = 30000;
        int total = Math.max(1, (png.length + chunkSize - 1) / chunkSize);
        for (int i = 0; i < total; i++) {
            int start = i * chunkSize;
            int length = Math.min(chunkSize, png.length - start);
            byte[] part = Arrays.copyOfRange(png, start, start + length);
            ClientPlayNetworking.send(new ScreenshotChunkPayload(shotId, i, total, part));
        }
    }

    /** Toggles the first boolean field that exists under any of the candidate names. */
    private static void toggleField(Object target, String... fieldNames) {
        if (target == null) {
            return;
        }
        for (String name : fieldNames) {
            try {
                Field field = findField(target.getClass(), name);
                if (field != null) {
                    field.setAccessible(true);
                    field.setBoolean(target, !field.getBoolean(target));
                    return;
                }
            } catch (Throwable ignored) {
                // Try the next candidate.
            }
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
