package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server asks a client to send a fresh diagnostic report. */
public record RequestReportPayload() implements CustomPayload {
    public static final RequestReportPayload INSTANCE = new RequestReportPayload();
    public static final CustomPayload.Id<RequestReportPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("request_report"));
    public static final PacketCodec<RegistryByteBuf, RequestReportPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
