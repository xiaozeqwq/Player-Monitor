package com.xiaoze.playermonitor.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xiaoze.playermonitor.PlayerMonitor;
import com.xiaoze.playermonitor.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Collects a diagnostic snapshot of the running client. Every section is wrapped in a
 * guarded call so that restricted environments (for example Android) simply report an
 * error entry instead of failing the whole report.
 */
public final class ClientInfoCollector {
    /** Mod ids or name fragments that are commonly associated with cheat clients. */
    private static final String[] CHEAT_KEYWORDS = {
            "wurst", "meteor", "aristois", "impact", "future", "salhack", "konas",
            "interia", "lambda", "rusherhack", "bleachhack", "seppuku", "phobos"
    };

    private ClientInfoCollector() {
    }

    public static JsonObject collect(MinecraftClient client) {
        JsonObject root = new JsonObject();
        JsonArray errors = new JsonArray();
        ModConfig config = PlayerMonitor.config();

        root.addProperty("collectedAt", System.currentTimeMillis());
        guard(root, errors, "meta", ClientInfoCollector::meta);
        guard(root, errors, "mods", ClientInfoCollector::mods);
        guard(root, errors, "resourcePacks", () -> resourcePacks(client));
        guard(root, errors, "launcher", ClientInfoCollector::launcher);
        guard(root, errors, "jvm", ClientInfoCollector::jvm);
        guard(root, errors, "system", ClientInfoCollector::system);
        guard(root, errors, "hardware", () -> hardware(config));
        guard(root, errors, "gameSettings", () -> gameSettings(client));
        guard(root, errors, "shaders", ClientInfoCollector::shaders);
        guard(root, errors, "account", () -> account(client));
        guard(root, errors, "performance", () -> performance(client));
        guard(root, errors, "keybinds", () -> keybinds(client));
        guard(root, errors, "cheatDetection", ClientInfoCollector::cheatDetection);
        if (config.collectProcesses) {
            guard(root, errors, "processes", () -> processes(config));
        }
        if (config.collectClipboard) {
            guard(root, errors, "clipboard", () -> clipboard(client));
        }

        if (!errors.isEmpty()) {
            root.add("errors", errors);
        }
        return root;
    }

    private static void guard(JsonObject root, JsonArray errors, String name, Supplier<JsonElement> supplier) {
        try {
            JsonElement value = supplier.get();
            if (value != null) {
                root.add(name, value);
            }
        } catch (Throwable t) {
            // Never surface the exception to the user; report it to the server only.
            JsonObject error = new JsonObject();
            error.addProperty("section", name);
            error.addProperty("type", t.getClass().getName());
            error.addProperty("message", String.valueOf(t.getMessage()));
            errors.add(error);
        }
    }

    private static JsonObject meta() {
        JsonObject o = new JsonObject();
        o.addProperty("modId", PlayerMonitor.MOD_ID);
        o.addProperty("modVersion", FabricLoader.getInstance().getModContainer(PlayerMonitor.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        o.addProperty("fabricLoader", FabricLoader.getInstance().getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
        return o;
    }

    private static JsonArray mods() {
        JsonArray array = new JsonArray();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            JsonObject mod = new JsonObject();
            mod.addProperty("id", container.getMetadata().getId());
            mod.addProperty("name", container.getMetadata().getName());
            mod.addProperty("version", container.getMetadata().getVersion().getFriendlyString());
            array.add(mod);
        }
        return array;
    }

    private static JsonArray resourcePacks(MinecraftClient client) {
        JsonArray array = new JsonArray();
        Collection<ResourcePackProfile> profiles = client.getResourcePackManager().getEnabledProfiles();
        for (ResourcePackProfile profile : profiles) {
            JsonObject pack = new JsonObject();
            pack.addProperty("name", profile.getDisplayName().getString());
            array.add(pack);
        }
        return array;
    }

    private static JsonObject launcher() {
        JsonObject o = new JsonObject();
        o.addProperty("brand", System.getProperty("minecraft.launcher.brand", "unknown"));
        o.addProperty("version", System.getProperty("minecraft.launcher.version", "unknown"));
        o.addProperty("name", System.getProperty("minecraft.launcher.name", "unknown"));
        return o;
    }

    private static JsonObject jvm() {
        JsonObject o = new JsonObject();
        o.addProperty("javaVersion", System.getProperty("java.version", ""));
        o.addProperty("javaVendor", System.getProperty("java.vendor", ""));
        o.addProperty("vmName", System.getProperty("java.vm.name", ""));
        o.addProperty("javaHome", System.getProperty("java.home", ""));
        o.addProperty("classPath", System.getProperty("java.class.path", ""));

        JsonArray jvmArgs = new JsonArray();
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            jvmArgs.add(mask(arg));
        }
        o.add("jvmArgs", jvmArgs);

        ProcessHandle.current().info().commandLine().ifPresent(cmd -> o.addProperty("commandLine", mask(cmd)));
        return o;
    }

    private static JsonObject system() {
        JsonObject o = new JsonObject();
        o.addProperty("osName", System.getProperty("os.name", ""));
        o.addProperty("osVersion", System.getProperty("os.version", ""));
        o.addProperty("osArch", System.getProperty("os.arch", ""));
        o.addProperty("timezone", System.getProperty("user.timezone", ""));
        o.addProperty("locale", Locale.getDefault().toString());
        o.addProperty("userName", mask(System.getProperty("user.name", "")));
        o.addProperty("userHome", mask(System.getProperty("user.home", "")));
        o.addProperty("processors", Runtime.getRuntime().availableProcessors());
        return o;
    }

    private static JsonObject hardware(ModConfig config) {
        JsonObject o = new JsonObject();
        java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
        o.addProperty("availableProcessors", base.getAvailableProcessors());
        o.addProperty("systemLoadAverage", base.getSystemLoadAverage());

        if (base instanceof com.sun.management.OperatingSystemMXBean os) {
            o.addProperty("cpuLoad", os.getCpuLoad());
            o.addProperty("processCpuLoad", os.getProcessCpuLoad());
            o.addProperty("totalMemory", os.getTotalMemorySize());
            o.addProperty("freeMemory", os.getFreeMemorySize());
        }

        Runtime runtime = Runtime.getRuntime();
        o.addProperty("jvmMaxMemory", runtime.maxMemory());
        o.addProperty("jvmTotalMemory", runtime.totalMemory());
        o.addProperty("jvmFreeMemory", runtime.freeMemory());

        o.addProperty("cpuModel", detectCpuModel());
        o.addProperty("gpuRenderer", glString(GL11.GL_RENDERER));
        o.addProperty("gpuVendor", glString(GL11.GL_VENDOR));
        o.addProperty("glVersion", glString(GL11.GL_VERSION));
        return o;
    }

    private static JsonObject gameSettings(MinecraftClient client) {
        JsonObject o = new JsonObject();
        o.addProperty("renderDistance", client.options.getViewDistance().getValue());
        o.addProperty("fov", client.options.getFov().getValue());
        o.addProperty("gamma", client.options.getGamma().getValue());
        o.addProperty("guiScale", client.options.getGuiScale().getValue());
        o.addProperty("graphicsMode", client.options.getGraphicsMode().getValue().name());
        o.addProperty("masterVolume", client.options.getSoundVolume(SoundCategory.MASTER));
        o.addProperty("language", client.getLanguageManager().getLanguage());
        o.addProperty("fullscreen", client.getWindow().isFullscreen());
        return o;
    }

    private static JsonObject shaders() {
        JsonObject o = new JsonObject();
        boolean iris = FabricLoader.getInstance().isModLoaded("iris");
        boolean optifine = FabricLoader.getInstance().isModLoaded("optifine");
        boolean oculus = FabricLoader.getInstance().isModLoaded("oculus");
        o.addProperty("iris", iris);
        o.addProperty("optifine", optifine);
        o.addProperty("oculus", oculus);
        // Iris exposes the active shader pack through a static API; read it reflectively so
        // the mod still compiles and runs when Iris is absent.
        String pack = readStaticString("net.irisshaders.iris.Iris", "getCurrentPackName");
        o.addProperty("shaderPack", pack == null ? "unknown" : pack);
        return o;
    }

    private static JsonObject account(MinecraftClient client) {
        JsonObject o = new JsonObject();
        o.addProperty("username", client.getSession().getUsername());
        o.addProperty("uuid", String.valueOf(client.getSession().getUuidOrNull()));
        o.addProperty("skinUrl", "https://crafatar.com/renders/head/" + client.getSession().getUuidOrNull() + "?overlay");
        return o;
    }

    private static JsonObject performance(MinecraftClient client) {
        JsonObject o = new JsonObject();
        o.addProperty("fps", client.getCurrentFps());
        o.addProperty("framebufferWidth", client.getWindow().getFramebufferWidth());
        o.addProperty("framebufferHeight", client.getWindow().getFramebufferHeight());
        o.addProperty("scaledWidth", client.getWindow().getScaledWidth());
        o.addProperty("scaledHeight", client.getWindow().getScaledHeight());
        return o;
    }

    private static JsonArray keybinds(MinecraftClient client) {
        JsonArray array = new JsonArray();
        for (KeyBinding binding : client.options.allKeys) {
            JsonObject key = new JsonObject();
            key.addProperty("key", binding.getTranslationKey());
            key.addProperty("bound", binding.getBoundKeyLocalizedText().getString());
            array.add(key);
        }
        return array;
    }

    private static JsonObject cheatDetection() {
        JsonObject o = new JsonObject();
        JsonArray matches = new JsonArray();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            String id = container.getMetadata().getId().toLowerCase(Locale.ROOT);
            String name = container.getMetadata().getName().toLowerCase(Locale.ROOT);
            for (String keyword : CHEAT_KEYWORDS) {
                if (id.contains(keyword) || name.contains(keyword)) {
                    JsonObject match = new JsonObject();
                    match.addProperty("modId", container.getMetadata().getId());
                    match.addProperty("keyword", keyword);
                    matches.add(match);
                }
            }
        }
        o.addProperty("suspicious", !matches.isEmpty());
        o.add("matches", matches);
        return o;
    }

    private static JsonObject processes(ModConfig config) {
        JsonObject o = new JsonObject();
        JsonArray array = new JsonArray();
        ProcessHandle.allProcesses()
                .limit(Math.max(1, config.maxProcessCount))
                .forEach(handle -> {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("pid", handle.pid());
                    ProcessHandle.Info info = handle.info();
                    entry.addProperty("command", mask(info.command().orElse("")));
                    entry.addProperty("commandLine", mask(info.commandLine().orElse("")));
                    handle.parent().ifPresent(parent -> entry.addProperty("parentPid", parent.pid()));
                    array.add(entry);
                });
        o.add("list", array);
        o.addProperty("count", array.size());
        return o;
    }

    private static JsonObject clipboard(MinecraftClient client) {
        JsonObject o = new JsonObject();
        String content = client.keyboard.getClipboard();
        o.addProperty("content", mask(content));
        o.addProperty("length", content == null ? 0 : content.length());
        return o;
    }

    private static String detectCpuModel() {
        try {
            Path cpuinfo = Path.of("/proc/cpuinfo");
            if (Files.exists(cpuinfo)) {
                try (BufferedReader reader = Files.newBufferedReader(cpuinfo)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("model name")) {
                            int idx = line.indexOf(':');
                            if (idx > 0) {
                                return line.substring(idx + 1).trim();
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Not available on this platform.
        }
        return System.getenv("PROCESSOR_IDENTIFIER") == null ? "unknown" : System.getenv("PROCESSOR_IDENTIFIER");
    }

    private static String glString(int name) {
        try {
            String value = GL11.glGetString(name);
            return value == null ? "unknown" : value;
        } catch (Throwable t) {
            return "unavailable";
        }
    }

    private static String readStaticString(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Object result = type.getMethod(methodName).invoke(null);
            return result == null ? null : result.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Optionally masks the current user's home directory and host name. */
    private static String mask(String value) {
        if (value == null || !PlayerMonitor.config().maskPersonalData) {
            return value;
        }
        String home = System.getProperty("user.home", "");
        if (!home.isEmpty()) {
            value = value.replace(home, "~");
        }
        return value;
    }
}
