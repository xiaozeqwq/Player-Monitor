package com.xiaoze.playermonitor.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.xiaoze.playermonitor.PlayerMonitor;
import com.xiaoze.playermonitor.config.ModConfig;
import com.xiaoze.playermonitor.network.payload.OpenGuiPayload;
import com.xiaoze.playermonitor.network.payload.ServerDataPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Registers the /playermonitor command tree. */
public final class MonitorCommand {
    private MonitorCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralCommandNode<ServerCommandSource> node = dispatcher.register(
                    literal("playermonitor")
                            // Admin (OP level 4) verifies with the configured password. Not usable from console.
                            .then(literal("verify")
                                    .requires(source -> source.getPlayer() != null && source.hasPermissionLevel(4))
                                    .then(argument("password", StringArgumentType.greedyString())
                                            .executes(MonitorCommand::verify)))
                            // Console-only helper to set or rotate the password hash.
                            .then(literal("setpassword")
                                    .requires(source -> source.getEntity() == null)
                                    .then(argument("password", StringArgumentType.greedyString())
                                            .executes(MonitorCommand::setPassword)))
                            // Console-only report dump: /playermonitor <player> info
                            .then(argument("player", EntityArgumentType.player())
                                    .then(literal("info")
                                            .requires(source -> source.getEntity() == null)
                                            .executes(MonitorCommand::info))));

            // Convenience alias without the "playermonitor" prefix.
            dispatcher.register(literal("pmonitor").redirect(node));
        });
    }

    private static int verify(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("[PlayerMonitor] This command is player-only."));
            return 0;
        }
        String password = StringArgumentType.getString(context, "password");
        ModConfig config = PlayerMonitor.config();
        if (config.passwordHash == null || config.passwordHash.isEmpty()) {
            context.getSource().sendError(Text.literal("[PlayerMonitor] No password set. Use the console command /playermonitor setpassword <password>."));
            return 0;
        }
        if (!AuthManager.verify(password, config.passwordHash)) {
            context.getSource().sendError(Text.literal("[PlayerMonitor] Verification failed."));
            return 0;
        }
        if (!ServerPlayNetworking.canSend(player, OpenGuiPayload.ID)
                || !ServerPlayNetworking.canSend(player, ServerDataPayload.ID)) {
            context.getSource().sendError(Text.literal("[PlayerMonitor] This client does not have the PlayerMonitor GUI mod installed."));
            return 0;
        }
        AuthManager.authorize(player.getUuid());
        ServerDataStore.openGui(player.getUuid());
        ServerPlayNetworking.send(player, OpenGuiPayload.INSTANCE);
        ServerDataStore.sendList(context.getSource().getServer(), player);
        player.sendMessage(Text.literal("[PlayerMonitor] Verified. Opening monitor GUI."), false);
        return 1;
    }

    private static int setPassword(CommandContext<ServerCommandSource> context) {
        String password = StringArgumentType.getString(context, "password");
        ModConfig config = PlayerMonitor.config();
        config.passwordHash = AuthManager.sha256(password);
        config.save();
        context.getSource().sendFeedback(() -> Text.literal("[PlayerMonitor] Password hash updated."), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ServerDataStore.PlayerReport report = ServerDataStore.getReport(player.getUuid());
        if (report == null) {
            context.getSource().sendError(Text.literal("[PlayerMonitor] No report available for " + player.getName().getString() + "."));
            return 0;
        }
        // Console-only output: write the raw report to the server log.
        PlayerMonitor.LOGGER.info("[PlayerMonitor] Report for {} ({}):\n{}",
                report.name(), report.uuid(), report.json());
        context.getSource().sendFeedback(() -> Text.literal("[PlayerMonitor] Report for " + report.name() + " printed to console."), false);
        return 1;
    }
}
