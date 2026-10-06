package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Sent by an administrator GUI to the server, asking it to run an action against
 * a target player. The server validates authorization before acting.
 */
public record ActionRequestPayload(String targetUuid, String actionId, String argsJson) implements CustomPayload {
    public static final CustomPayload.Id<ActionRequestPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("action_request"));
    public static final PacketCodec<RegistryByteBuf, ActionRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ActionRequestPayload::targetUuid,
            PacketCodecs.STRING, ActionRequestPayload::actionId,
            PacketCodecs.STRING, ActionRequestPayload::argsJson,
            ActionRequestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
