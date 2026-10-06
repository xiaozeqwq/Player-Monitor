package com.xiaoze.playermonitor.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xiaoze.playermonitor.network.payload.ServerDataPayload;
import com.xiaoze.playermonitor.util.TextUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

/** In-memory store of the latest report and screenshots for every tracked player. */
public final class ServerDataStore {
    private static final int CHUNK_SIZE = 30000;
    private static final Gson GSON = new Gson();

    public record PlayerReport(UUID uuid, String name, String json, long updatedAt) {
    }

    private static final Map<UUID, PlayerReport> REPORTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, byte[]>> SCREENSHOTS = new ConcurrentHashMap<>();
    private static final Set<UUID> OPEN_GUI = new CopyOnWriteArraySet<>();

    private ServerDataStore() {
    }

    public static void updateReport(UUID uuid, String name, String json) {
        REPORTS.put(uuid, new PlayerReport(uuid, name, json, System.currentTimeMillis()));
    }

    public static PlayerReport getReport(UUID uuid) {
        return REPORTS.get(uuid);
    }

    public static Map<UUID, PlayerReport> getReports() {
        return REPORTS;
    }

    public static void remove(UUID uuid) {
        REPORTS.remove(uuid);
        SCREENSHOTS.remove(uuid);
    }

    public static void clear() {
        REPORTS.clear();
        SCREENSHOTS.clear();
        OPEN_GUI.clear();
    }

    public static void openGui(UUID uuid) {
        OPEN_GUI.add(uuid);
    }

    public static void closeGui(UUID uuid) {
        OPEN_GUI.remove(uuid);
    }

    public static void storeScreenshot(UUID uuid, String shotId, byte[] data) {
        SCREENSHOTS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(shotId, data);
        // Keep at most the three most recent screenshots to bound memory usage.
        Map<String, byte[]> shots = SCREENSHOTS.get(uuid);
        while (shots.size() > 3) {
            String oldest = shots.keySet().iterator().next();
            shots.remove(oldest);
        }
    }

    public static byte[] getScreenshot(UUID uuid, String shotId) {
        Map<String, byte[]> shots = SCREENSHOTS.get(uuid);
        return shots == null ? null : shots.get(shotId);
    }

    /** Sends the player list to one administrator. */
    public static void sendList(MinecraftServer server, ServerPlayerEntity admin) {
        JsonObject root = new JsonObject();
        root.addProperty("kind", "players");
        JsonArray players = new JsonArray();
        for (PlayerReport report : REPORTS.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("uuid", report.uuid().toString());
            entry.addProperty("name", report.name());
            entry.addProperty("updatedAt", report.updatedAt());
            players.add(entry);
        }
        root.add("players", players);
        sendJson(admin, root.toString());
    }

    /** Sends a single report to one administrator. */
    public static void sendReport(ServerPlayerEntity admin, UUID target) {
        PlayerReport report = REPORTS.get(target);
        if (report == null) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("kind", "report");
        root.addProperty("uuid", report.uuid().toString());
        root.addProperty("name", report.name());
        root.addProperty("updatedAt", report.updatedAt());
        try {
            root.add("report", GSON.fromJson(report.json(), JsonObject.class));
        } catch (Exception e) {
            root.addProperty("report", report.json());
        }
        sendJson(admin, root.toString());
    }

    /** Pushes the list update to every administrator that currently has the GUI open. */
    public static void pushToAdmins(MinecraftServer server, UUID updated) {
        for (UUID adminId : OPEN_GUI) {
            ServerPlayerEntity admin = server.getPlayerManager().getPlayer(adminId);
            if (admin != null) {
                sendList(server, admin);
                sendReport(admin, updated);
            }
        }
    }

    /** Pushes a freshly received screenshot to every administrator that has the GUI open. */
    public static void pushScreenshot(MinecraftServer server, UUID owner, String shotId) {
        byte[] data = getScreenshot(owner, shotId);
        if (data == null) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("kind", "screenshot");
        root.addProperty("uuid", owner.toString());
        root.addProperty("shotId", shotId);
        root.addProperty("data", java.util.Base64.getEncoder().encodeToString(data));
        String json = root.toString();
        for (UUID adminId : OPEN_GUI) {
            ServerPlayerEntity admin = server.getPlayerManager().getPlayer(adminId);
            if (admin != null) {
                sendJson(admin, json);
            }
        }
    }

    private static void sendJson(ServerPlayerEntity admin, String json) {
        byte[] data = TextUtil.toBytes(json);
        int total = Math.max(1, (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        String messageId = UUID.randomUUID().toString();
        for (int i = 0; i < total; i++) {
            int start = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, data.length - start);
            byte[] part = Arrays.copyOfRange(data, start, start + length);
            ServerPlayNetworking.send(admin, new ServerDataPayload(messageId, i, total, part));
        }
    }
}
