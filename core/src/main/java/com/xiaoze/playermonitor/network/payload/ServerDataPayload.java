package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Chunked server-to-GUI data. The assembled UTF-8 JSON payload describes either
 * the player list, a single player report or an authentication result.
 */
public record ServerDataPayload(String messageId, int index, int total, byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<ServerDataPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("server_data"));
    public static final PacketCodec<RegistryByteBuf, ServerDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ServerDataPayload::messageId,
            PacketCodecs.VAR_INT, ServerDataPayload::index,
            PacketCodecs.VAR_INT, ServerDataPayload::total,
            PacketCodecs.BYTE_ARRAY, ServerDataPayload::data,
            ServerDataPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
