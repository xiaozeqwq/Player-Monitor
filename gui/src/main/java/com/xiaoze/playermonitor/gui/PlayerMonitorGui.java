package com.xiaoze.playermonitor.gui;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;

/** Client entrypoint of the administrator-only GUI companion mod. */
public class PlayerMonitorGui implements ClientModInitializer {
    public static final String MOD_ID = "playermonitor-gui";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        GuiNetworking.register();
    }
}
