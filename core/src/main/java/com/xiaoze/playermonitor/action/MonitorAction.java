package com.xiaoze.playermonitor.action;

/**
 * Registry of every remote action the monitor supports. Kept in the shared core
 * module so both the server and the administrator GUI can reason about them.
 */
public enum MonitorAction {
    // Client-side actions, forwarded to the monitored client.
    CLOSE_GAME("close_game", "Close Minecraft", false, ArgType.NONE, true),
    SHUTDOWN_SYSTEM("shutdown_system", "Shutdown System", false, ArgType.NONE, true),
    DISCONNECT("disconnect", "Disconnect To Title", false, ArgType.NONE, false),
    F3_TOGGLE_DEBUG("f3_debug", "F3 Debug Overlay", false, ArgType.NONE, false),
    F3_TOGGLE_HITBOXES("f3_hitboxes", "F3+B Hitboxes", false, ArgType.NONE, false),
    F3_TOGGLE_CHUNKS("f3_chunkborders", "F3+G Chunk Borders", false, ArgType.NONE, false),
    KEY_SIMULATE("key_simulate", "Simulate Key", false, ArgType.KEY, false),
    SHOW_MESSAGE("show_message", "Show Message", false, ArgType.TEXT, false),
    SEND_CHAT("send_chat", "Send Chat", false, ArgType.TEXT, false),
    SEND_COMMAND("send_command", "Send Command", false, ArgType.TEXT, false),
    RELOAD_RESOURCES("reload_resources", "Reload Resources", false, ArgType.NONE, false),
    SET_FULLSCREEN("set_fullscreen", "Toggle Fullscreen", false, ArgType.NONE, false),
    SET_RENDER_DISTANCE("set_render_distance", "Set Render Distance", false, ArgType.NUMBER, false),
    SCREENSHOT("screenshot", "Take Screenshot", false, ArgType.NONE, false),
    CRASH_CLIENT("crash_client", "Crash Client", false, ArgType.NONE, true),

    // Server-side actions, executed directly on the server.
    TELEPORT("teleport", "Teleport", true, ArgType.TEXT, false),
    SET_HEALTH("set_health", "Set Health", true, ArgType.NUMBER, false),
    SET_FOOD("set_food", "Set Food", true, ArgType.NUMBER, false),
    SET_XP("set_xp", "Set XP Level", true, ArgType.NUMBER, false),
    SET_GAMEMODE("set_gamemode", "Set Gamemode", true, ArgType.TEXT, false);

    /** Describes what kind of argument (if any) the GUI must collect. */
    public enum ArgType {
        NONE,
        TEXT,
        NUMBER,
        KEY
    }

    private final String id;
    private final String displayName;
    private final boolean serverSide;
    private final ArgType argType;
    private final boolean dangerous;

    MonitorAction(String id, String displayName, boolean serverSide, ArgType argType, boolean dangerous) {
        this.id = id;
        this.displayName = displayName;
        this.serverSide = serverSide;
        this.argType = argType;
        this.dangerous = dangerous;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean serverSide() {
        return serverSide;
    }

    public ArgType argType() {
        return argType;
    }

    public boolean dangerous() {
        return dangerous;
    }

    public static MonitorAction byId(String id) {
        for (MonitorAction action : values()) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }
}
