package com.xiaoze.playermonitor.server;

import com.xiaoze.playermonitor.action.MonitorAction;
import com.xiaoze.playermonitor.network.payload.ActionRequestPayload;
import com.xiaoze.playermonitor.network.payload.GuiRequestPayload;
import com.xiaoze.playermonitor.network.payload.MonitorActionPayload;
import com.xiaoze.playermonitor.network.payload.OpenGuiPayload;
import com.xiaoze.playermonitor.network.payload.ReportChunkPayload;
import com.xiaoze.playermonitor.network.payload.ScreenshotChunkPayload;
import com.xiaoze.playermonitor.util.ChunkAssembler;
import com.xiaoze.playermonitor.util.TextUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** Registers all server-side receivers for the custom payloads. */
public final class ServerNetworking {
    private static final ChunkAssembler REPORTS = new ChunkAssembler();
    private static final ChunkAssembler SCREENSHOTS = new ChunkAssembler();

    private ServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ReportChunkPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            UUID uuid = player.getUuid();
            byte[] assembled = REPORTS.accept(uuid + ":" + payload.reportId(), payload.index(), payload.total(), payload.data());
            if (assembled == null) {
                return;
            }
            String json = TextUtil.fromBytes(assembled);
            context.server().execute(() -> {
                ServerDataStore.updateReport(uuid, player.getName().getString(), json);
                ServerDataStore.pushToAdmins(context.server(), uuid);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ScreenshotChunkPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            UUID uuid = player.getUuid();
            byte[] assembled = SCREENSHOTS.accept(uuid + ":" + payload.shotId(), payload.index(), payload.total(), payload.data());
            if (assembled == null) {
                return;
            }
            context.server().execute(() -> {
                ServerDataStore.storeScreenshot(uuid, payload.shotId(), assembled);
                ServerDataStore.pushScreenshot(context.server(), uuid, payload.shotId());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ActionRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> handleActionRequest(context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(GuiRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> handleGuiRequest(context.player(), payload));
        });
    }

    private static void handleActionRequest(ServerPlayerEntity admin, ActionRequestPayload payload) {
        if (!AuthManager.isAuthorized(admin.getUuid())) {
            admin.sendMessage(Text.literal("[PlayerMonitor] Not authorized. Run /playermonitor verify first."), false);
            return;
        }
        MonitorAction action = MonitorAction.byId(payload.actionId());
        if (action == null) {
            return;
        }
        UUID targetId;
        try {
            targetId = UUID.fromString(payload.targetUuid());
        } catch (IllegalArgumentException e) {
            return;
        }
        ServerPlayerEntity target = admin.getServer().getPlayerManager().getPlayer(targetId);
        if (target == null) {
            admin.sendMessage(Text.literal("[PlayerMonitor] Target player is offline."), false);
            return;
        }
        if (action.serverSide()) {
            ServerActions.executeServerSide(admin.getServer(), target, action, payload.argsJson());
            admin.sendMessage(Text.literal("[PlayerMonitor] Executed " + action.id() + " on " + target.getName().getString()), false);
        } else if (ServerPlayNetworking.canSend(target, MonitorActionPayload.ID)) {
            ServerActions.forwardToClient(target, action, payload.argsJson());
            admin.sendMessage(Text.literal("[PlayerMonitor] Sent " + action.id() + " to " + target.getName().getString()), false);
        } else {
            admin.sendMessage(Text.literal("[PlayerMonitor] " + target.getName().getString() + " does not have the mod installed; client action unavailable."), false);
        }
    }

    private static void handleGuiRequest(ServerPlayerEntity admin, GuiRequestPayload payload) {
        if (!AuthManager.isAuthorized(admin.getUuid())) {
            return;
        }
        switch (payload.request()) {
            case "open" -> {
                if (!ServerPlayNetworking.canSend(admin, OpenGuiPayload.ID)) {
                    admin.sendMessage(Text.literal("[PlayerMonitor] GUI mod not detected on this client."), false);
                    return;
                }
                ServerDataStore.openGui(admin.getUuid());
                ServerPlayNetworking.send(admin, OpenGuiPayload.INSTANCE);
                ServerDataStore.sendList(admin.getServer(), admin);
            }
            case "refresh" -> ServerDataStore.sendList(admin.getServer(), admin);
            case "report" -> {
                try {
                    ServerDataStore.sendReport(admin, UUID.fromString(payload.targetUuid()));
                } catch (IllegalArgumentException ignored) {
                    // Malformed UUID; ignore.
                }
            }
            default -> {
                // Unknown request; ignore.
            }
        }
    }
}
