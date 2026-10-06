package com.xiaoze.playermonitor.client;

import com.xiaoze.playermonitor.PlayerMonitor;
import com.xiaoze.playermonitor.network.payload.ReportChunkPayload;
import com.xiaoze.playermonitor.util.TextUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;
import java.util.UUID;

/**
 * Builds and uploads the diagnostic report. Reports are chunked so that large
 * sections (process lists, mod lists) stay below the payload size limit.
 */
public final class ClientReportManager {
    private static final int CHUNK_SIZE = 30000;

    private static boolean sent = false;
    private static int ticksSinceJoin = -1;

    private ClientReportManager() {
    }

    public static void onJoin() {
        sent = false;
        ticksSinceJoin = 0;
    }

    public static void onDisconnect() {
        sent = false;
        ticksSinceJoin = -1;
    }

    /** Waits a short grace period after joining, then uploads a single report. */
    public static void tick(MinecraftClient client) {
        if (ticksSinceJoin < 0 || client.player == null || client.world == null) {
            return;
        }
        ticksSinceJoin++;
        if (!sent && ticksSinceJoin >= 60) {
            send(client);
            sent = true;
        }
    }

    /** Forces a new report, used when the server requests one. */
    public static void requestSend(MinecraftClient client) {
        send(client);
    }

    public static void send(MinecraftClient client) {
        try {
            String json = ClientInfoCollector.collect(client).toString();
            byte[] data = TextUtil.toBytes(json);
            int total = Math.max(1, (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
            String reportId = UUID.randomUUID().toString();
            for (int i = 0; i < total; i++) {
                int start = i * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, data.length - start);
                byte[] part = Arrays.copyOfRange(data, start, start + length);
                ClientPlayNetworking.send(new ReportChunkPayload(reportId, i, total, part));
            }
            PlayerMonitor.LOGGER.info("[PlayerMonitor] uploaded report ({} bytes in {} chunk(s)).", data.length, total);
        } catch (Throwable t) {
            PlayerMonitor.LOGGER.error("[PlayerMonitor] failed to build or send report", t);
        }
    }
}
