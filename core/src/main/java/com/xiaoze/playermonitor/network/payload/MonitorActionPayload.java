package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server-to-client instruction that the monitored client must execute locally
 * (for example closing the game, simulating a key press or taking a screenshot).
 */
public record MonitorActionPayload(String actionId, String argsJson) implements CustomPayload {
    public static final CustomPayload.Id<MonitorActionPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("action"));
    public static final PacketCodec<RegistryByteBuf, MonitorActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, MonitorActionPayload::actionId,
            PacketCodecs.STRING, MonitorActionPayload::argsJson,
            MonitorActionPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
