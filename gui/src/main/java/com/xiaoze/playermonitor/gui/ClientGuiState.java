package com.xiaoze.playermonitor.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiaoze.playermonitor.network.payload.ActionRequestPayload;
import com.xiaoze.playermonitor.network.payload.GuiRequestPayload;
import com.xiaoze.playermonitor.network.payload.ServerDataPayload;
import com.xiaoze.playermonitor.util.ChunkAssembler;
import com.xiaoze.playermonitor.util.TextUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Client-side cache of everything the server sent to the GUI. */
public final class ClientGuiState {
    public record PlayerEntry(String uuid, String name, long updatedAt) {
    }

    private static final Gson GSON = new Gson();
    private static final ChunkAssembler ASSEMBLER = new ChunkAssembler();
    private static final List<PlayerEntry> PLAYERS = new CopyOnWriteArrayList<>();
    private static final Map<String, JsonObject> REPORTS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> SCREENSHOTS = new ConcurrentHashMap<>();

    private static volatile int version = 0;

    private ClientGuiState() {
    }

    public static List<PlayerEntry> players() {
        return PLAYERS;
    }

    public static JsonObject report(String uuid) {
        return REPORTS.get(uuid);
    }

    public static byte[] screenshot(String uuid) {
        return SCREENSHOTS.get(uuid);
    }

    public static int version() {
        return version;
    }

    public static void open(MinecraftClient client) {
        if (!(client.currentScreen instanceof PlayerListScreen)) {
            client.setScreen(new PlayerListScreen(null));
        }
    }

    public static void acceptChunk(ServerDataPayload payload) {
        byte[] assembled = ASSEMBLER.accept(payload.messageId(), payload.index(), payload.total(), payload.data());
        if (assembled == null) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(TextUtil.fromBytes(assembled), JsonObject.class);
            if (root == null || !root.has("kind")) {
                return;
            }
            switch (root.get("kind").getAsString()) {
                case "players" -> handlePlayers(root);
                case "report" -> handleReport(root);
                case "screenshot" -> handleScreenshot(root);
                default -> {
                    // Unknown kind; ignore.
                }
            }
            version++;
        } catch (Throwable ignored) {
            // Malformed payload; ignore.
        }
    }

    private static void handlePlayers(JsonObject root) {
        List<PlayerEntry> entries = new ArrayList<>();
        JsonArray array = root.getAsJsonArray("players");
        if (array != null) {
            for (JsonElement element : array) {
                JsonObject entry = element.getAsJsonObject();
                entries.add(new PlayerEntry(
                        entry.get("uuid").getAsString(),
                        entry.get("name").getAsString(),
                        entry.has("updatedAt") ? entry.get("updatedAt").getAsLong() : 0L));
            }
        }
        PLAYERS.clear();
        PLAYERS.addAll(entries);
    }

    private static void handleReport(JsonObject root) {
        String uuid = root.get("uuid").getAsString();
        REPORTS.put(uuid, root);
    }

    private static void handleScreenshot(JsonObject root) {
        String uuid = root.get("uuid").getAsString();
        try {
            SCREENSHOTS.put(uuid, Base64.getDecoder().decode(root.get("data").getAsString()));
        } catch (IllegalArgumentException ignored) {
            // Invalid base64; ignore.
        }
    }

    public static void requestRefresh() {
        ClientPlayNetworking.send(new GuiRequestPayload("refresh", ""));
    }

    public static void requestReport(String uuid) {
        ClientPlayNetworking.send(new GuiRequestPayload("report", uuid));
    }

    public static void sendAction(String uuid, String actionId, String args) {
        ClientPlayNetworking.send(new ActionRequestPayload(uuid, actionId, args == null ? "" : args));
    }

    /** Flattens a report JSON tree into readable "path: value" lines. */
    public static List<String> flatten(JsonElement element, int maxLines) {
        List<String> lines = new ArrayList<>();
        flattenInto(element, "", lines, maxLines);
        return lines;
    }

    private static void flattenInto(JsonElement element, String prefix, List<String> lines, int maxLines) {
        if (lines.size() >= maxLines) {
            return;
        }
        if (element == null || element.isJsonNull()) {
            lines.add(prefix + ": null");
        } else if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                flattenInto(entry.getValue(), prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey(), lines, maxLines);
            }
        } else if (element.isJsonArray()) {
            int index = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                flattenInto(child, prefix + "[" + index + "]", lines, maxLines);
                index++;
                if (lines.size() >= maxLines) {
                    break;
                }
            }
        } else {
            lines.add(prefix + ": " + element.getAsString());
        }
    }
}
