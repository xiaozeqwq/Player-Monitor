package com.xiaoze.playermonitor.server;

import com.xiaoze.playermonitor.PlayerMonitor;
import com.xiaoze.playermonitor.network.payload.RequestReportPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/** Hooks player join/leave and server lifecycle events. */
public final class ServerLifecycle {
    private ServerLifecycle() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            if (PlayerMonitor.config().reportOnJoin) {
                server.execute(() -> ServerPlayNetworking.send(player, RequestReportPayload.INSTANCE));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            ServerDataStore.remove(uuid);
            ServerDataStore.closeGui(uuid);
            AuthManager.revoke(uuid);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerDataStore.clear();
            AuthManager.clear();
        });
    }
}
