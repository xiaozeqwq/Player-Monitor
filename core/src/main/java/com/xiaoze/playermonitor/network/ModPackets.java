package com.xiaoze.playermonitor.network;

import com.xiaoze.playermonitor.network.payload.ActionRequestPayload;
import com.xiaoze.playermonitor.network.payload.GuiRequestPayload;
import com.xiaoze.playermonitor.network.payload.MonitorActionPayload;
import com.xiaoze.playermonitor.network.payload.OpenGuiPayload;
import com.xiaoze.playermonitor.network.payload.ReportChunkPayload;
import com.xiaoze.playermonitor.network.payload.RequestReportPayload;
import com.xiaoze.playermonitor.network.payload.ScreenshotChunkPayload;
import com.xiaoze.playermonitor.network.payload.ServerDataPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers every custom payload on both the client-to-server and server-to-client channels. */
public final class ModPackets {
    private ModPackets() {
    }

    public static void register() {
        // Client to server.
        PayloadTypeRegistry.playC2S().register(ReportChunkPayload.ID, ReportChunkPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScreenshotChunkPayload.ID, ScreenshotChunkPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ActionRequestPayload.ID, ActionRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GuiRequestPayload.ID, GuiRequestPayload.CODEC);

        // Server to client.
        PayloadTypeRegistry.playS2C().register(MonitorActionPayload.ID, MonitorActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RequestReportPayload.ID, RequestReportPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID, OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDataPayload.ID, ServerDataPayload.CODEC);
    }
}
