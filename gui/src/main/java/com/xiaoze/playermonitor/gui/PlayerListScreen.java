package com.xiaoze.playermonitor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Lists every player that has uploaded a report and opens the detail view. */
public class PlayerListScreen extends Screen {
    private final Screen parent;
    private int lastVersion = -1;

    public PlayerListScreen(Screen parent) {
        super(Text.literal("Player Monitor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        int y = 40;
        int bottomLimit = height - 60;
        for (ClientGuiState.PlayerEntry entry : ClientGuiState.players()) {
            if (y > bottomLimit) {
                break;
            }
            String label = entry.name() + "  (" + entry.uuid().substring(0, 8) + ")";
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
                ClientGuiState.requestReport(entry.uuid());
                if (client != null) {
                    client.setScreen(new PlayerDetailScreen(this, entry.uuid(), entry.name()));
                }
            }).dimensions(width / 2 - 120, y, 240, 20).build());
            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> {
            ClientGuiState.requestRefresh();
            rebuild();
        }).dimensions(10, height - 30, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), button -> {
            if (client != null) {
                client.setScreen(GuiConfigScreen.create(this));
            }
        }).dimensions(100, height - 30, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(width - 90, height - 30, 80, 20).build());
    }

    @Override
    public void tick() {
        if (ClientGuiState.version() != lastVersion) {
            lastVersion = ClientGuiState.version();
            rebuild();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
        if (ClientGuiState.players().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No player reports available."), width / 2, 28, 0xAAAAAA);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
