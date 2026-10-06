package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** One chunk of a PNG screenshot produced by a remote capture request. */
public record ScreenshotChunkPayload(String shotId, int index, int total, byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<ScreenshotChunkPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("screenshot"));
    public static final PacketCodec<RegistryByteBuf, ScreenshotChunkPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ScreenshotChunkPayload::shotId,
            PacketCodecs.VAR_INT, ScreenshotChunkPayload::index,
            PacketCodecs.VAR_INT, ScreenshotChunkPayload::total,
            PacketCodecs.BYTE_ARRAY, ScreenshotChunkPayload::data,
            ScreenshotChunkPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
