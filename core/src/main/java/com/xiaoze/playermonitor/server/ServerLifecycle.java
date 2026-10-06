package com.xiaoze.playermonitor.server;

import com.xiaoze.playermonitor.PlayerMonitor;
import com.xiaoze.playermonitor.network.payload.RequestReportPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** Hooks player join/leave and server lifecycle events. */
public final class ServerLifecycle {
    private ServerLifecycle() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            boolean clientHasMod = ServerPlayNetworking.canSend(player, RequestReportPayload.ID);

            // Enforce the mod requirement: clients without the mod are disconnected with a clear message.
            if (PlayerMonitor.config().requireClientMod && !clientHasMod) {
                player.networkHandler.disconnect(Text.literal(PlayerMonitor.config().missingModMessage));
                return;
            }

            // Only ask clients that actually registered the payload.
            if (PlayerMonitor.config().reportOnJoin && clientHasMod) {
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
