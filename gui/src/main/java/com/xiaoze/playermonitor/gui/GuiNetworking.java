package com.xiaoze.playermonitor.gui;

import com.xiaoze.playermonitor.network.payload.OpenGuiPayload;
import com.xiaoze.playermonitor.network.payload.ServerDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Registers the server-to-client receivers that drive the administrator GUI. */
public final class GuiNetworking {
    private GuiNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenGuiPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientGuiState.open(context.client())));

        ClientPlayNetworking.registerGlobalReceiver(ServerDataPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientGuiState.acceptChunk(payload)));
    }
}
