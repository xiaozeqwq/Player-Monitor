package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** One chunk of a client-to-server diagnostic report (UTF-8 JSON bytes). */
public record ReportChunkPayload(String reportId, int index, int total, byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<ReportChunkPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("report"));
    public static final PacketCodec<RegistryByteBuf, ReportChunkPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ReportChunkPayload::reportId,
            PacketCodecs.VAR_INT, ReportChunkPayload::index,
            PacketCodecs.VAR_INT, ReportChunkPayload::total,
            PacketCodecs.BYTE_ARRAY, ReportChunkPayload::data,
            ReportChunkPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
