package com.xiaoze.playermonitor.gui;

import com.google.gson.JsonObject;
import com.xiaoze.playermonitor.action.MonitorAction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.util.List;

/** Detail view for a single player: flattened report, screenshot preview and action buttons. */
public class PlayerDetailScreen extends Screen {
    private final Screen parent;
    private final String uuid;
    private final String playerName;

    private int lastVersion = -1;
    private Identifier textureId;
    private int textureWidth;
    private int textureHeight;

    public PlayerDetailScreen(Screen parent, String uuid, String playerName) {
        super(Text.literal("Player: " + playerName));
        this.parent = parent;
        this.uuid = uuid;
        this.playerName = playerName;
    }

    @Override
    protected void init() {
        rebuild();
        ensureTexture();
    }

    private void rebuild() {
        clearChildren();
        ClientGuiState.requestReport(uuid);

        int colX = width / 2 + 8;
        int colY = 36;
        int perColumn = Math.max(1, (height - 80) / 20);
        int index = 0;
        for (MonitorAction action : MonitorAction.values()) {
            int column = index / perColumn;
            int row = index % perColumn;
            int x = colX + column * 104;
            int y = colY + row * 20;
            if (x + 100 > width) {
                break;
            }
            addDrawableChild(ButtonWidget.builder(Text.literal(action.displayName()), button -> onAction(action))
                    .dimensions(x, y, 100, 18).build());
            index++;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(width / 2 + 8, height - 28, 100, 18).build());
    }

    private void onAction(MonitorAction action) {
        if (client == null) {
            return;
        }
        if (action.argType() == MonitorAction.ArgType.NONE) {
            ClientGuiState.sendAction(uuid, action.id(), "");
        } else {
            client.setScreen(new ActionArgsScreen(this, uuid, playerName, action));
        }
    }

    private void ensureTexture() {
        byte[] bytes = ClientGuiState.screenshot(uuid);
        if (bytes == null || textureId != null || client == null) {
            return;
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            textureId = PlayerMonitorGui.id("shot_" + uuid.replace('-', '_'));
            client.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
        } catch (Throwable ignored) {
            textureId = null;
        }
    }

    private void resetTexture() {
        if (textureId != null && client != null) {
            client.getTextureManager().destroyTexture(textureId);
        }
        textureId = null;
    }

    @Override
    public void tick() {
        if (ClientGuiState.version() != lastVersion) {
            lastVersion = ClientGuiState.version();
            resetTexture();
            ensureTexture();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        JsonObject report = ClientGuiState.report(uuid);
        if (report == null) {
            context.drawTextWithShadow(textRenderer, Text.literal("Waiting for report..."), 10, 30, 0xAAAAAA);
            return;
        }

        int maxLines = GuiConfig.get().detailMaxLines;
        List<String> lines = ClientGuiState.flatten(report.get("report"), maxLines);
        int y = 30;
        int lineHeight = 10;
        int bottom = height - 40;
        for (String line : lines) {
            if (y > bottom) {
                break;
            }
            context.drawTextWithShadow(textRenderer, Text.literal(trim(line, 110)), 8, y, 0xE0E0E0);
            y += lineHeight;
        }

        if (textureId != null) {
            int previewW = 96;
            int previewH = 54;
            int px = width / 2 + 8;
            int py = height - 28 - previewH - 4;
            try {
                context.drawTexture(textureId, px, py, 0.0F, 0.0F, previewW, previewH, textureWidth, textureHeight);
            } catch (Throwable ignored) {
                // Texture might not be ready yet.
            }
        }
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    @Override
    public void close() {
        resetTexture();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
