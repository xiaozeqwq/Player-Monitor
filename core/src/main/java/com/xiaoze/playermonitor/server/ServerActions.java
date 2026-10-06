package com.xiaoze.playermonitor.server;

import com.xiaoze.playermonitor.action.MonitorAction;
import com.xiaoze.playermonitor.network.payload.MonitorActionPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;

/**
 * Executes server-side actions and forwards client-side actions to the target player.
 */
public final class ServerActions {
    private ServerActions() {
    }

    public static void executeServerSide(MinecraftServer server, ServerPlayerEntity target, MonitorAction action, String args) {
        switch (action) {
            case TELEPORT -> {
                double[] c = parseCoordinates(args);
                if (c != null) {
                    ServerWorld world = target.getServerWorld();
                    target.teleport(world, c[0], c[1], c[2], target.getYaw(), target.getPitch());
                }
            }
            case SET_HEALTH -> target.setHealth((float) parseDouble(args, target.getHealth()));
            case SET_FOOD -> target.getHungerManager().setFoodLevel((int) parseDouble(args, target.getHungerManager().getFoodLevel()));
            case SET_XP -> target.setExperienceLevel((int) parseDouble(args, target.experienceLevel));
            case SET_GAMEMODE -> {
                GameMode mode = GameMode.byName(args.trim().toLowerCase());
                target.changeGameMode(mode);
            }
            default -> {
                // Not a server-side action; nothing to do here.
            }
        }
    }

    public static void forwardToClient(ServerPlayerEntity target, MonitorAction action, String args) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                target, new MonitorActionPayload(action.id(), args == null ? "" : args));
    }

    private static double[] parseCoordinates(String args) {
        if (args == null) {
            return null;
        }
        String[] parts = args.trim().split("[\\s,]+");
        if (parts.length < 3) {
            return null;
        }
        try {
            return new double[]{
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double parseDouble(String args, double fallback) {
        if (args == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(args.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
