package com.xiaoze.playermonitor.client;

import com.xiaoze.playermonitor.network.payload.MonitorActionPayload;
import com.xiaoze.playermonitor.network.payload.RequestReportPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Registers the client-side receivers used by every monitored player. */
public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MonitorActionPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientActionExecutor.execute(
                        context.client(), payload.actionId(), payload.argsJson())));

        ClientPlayNetworking.registerGlobalReceiver(RequestReportPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientReportManager.requestSend(context.client())));
    }
}
