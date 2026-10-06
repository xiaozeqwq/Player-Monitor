package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Sent by an authorized administrator GUI to request server-side GUI data
 * (refresh the player list or fetch a specific player report).
 */
public record GuiRequestPayload(String request, String targetUuid) implements CustomPayload {
    public static final CustomPayload.Id<GuiRequestPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("gui_request"));
    public static final PacketCodec<RegistryByteBuf, GuiRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, GuiRequestPayload::request,
            PacketCodecs.STRING, GuiRequestPayload::targetUuid,
            GuiRequestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
