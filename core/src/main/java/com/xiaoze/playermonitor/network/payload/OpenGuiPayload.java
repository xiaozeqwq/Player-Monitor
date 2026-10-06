package com.xiaoze.playermonitor.network.payload;

import com.xiaoze.playermonitor.PlayerMonitor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server asks an authorized administrator client to open the monitor GUI. */
public record OpenGuiPayload() implements CustomPayload {
    public static final OpenGuiPayload INSTANCE = new OpenGuiPayload();
    public static final CustomPayload.Id<OpenGuiPayload> ID =
            new CustomPayload.Id<>(PlayerMonitor.id("open_gui"));
    public static final PacketCodec<RegistryByteBuf, OpenGuiPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
