package com.xiaoze.playermonitor;

import com.xiaoze.playermonitor.client.ClientNetworking;
import com.xiaoze.playermonitor.client.ClientReportManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Client entrypoint: registers receivers and schedules the initial report upload. */
public class PlayerMonitorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientReportManager.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientReportManager.onDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(ClientReportManager::tick);
    }
}
