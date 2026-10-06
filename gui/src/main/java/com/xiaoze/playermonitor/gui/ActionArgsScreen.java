package com.xiaoze.playermonitor.gui;

import com.xiaoze.playermonitor.action.MonitorAction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Collects a single argument for an action before it is sent to the server. */
public class ActionArgsScreen extends Screen {
    private final Screen parent;
    private final String uuid;
    private final String playerName;
    private final MonitorAction action;

    private TextFieldWidget field;
    private String hint = "";

    public ActionArgsScreen(Screen parent, String uuid, String playerName, MonitorAction action) {
        super(Text.literal(action.displayName()));
        this.parent = parent;
        this.uuid = uuid;
        this.playerName = playerName;
        this.action = action;
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20, Text.literal("argument"));
        field.setMaxLength(512);
        addDrawableChild(field);
        setInitialFocus(field);

        addDrawableChild(ButtonWidget.builder(Text.literal("Send"), button -> send())
                .dimensions(width / 2 - 100, height / 2 + 16, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 + 4, height / 2 + 16, 96, 20).build());

        switch (action.argType()) {
            case NUMBER -> hint = "Enter a number.";
            case KEY -> hint = "Enter a key translation key, for example key.jump.";
            case TEXT -> hint = action == MonitorAction.TELEPORT
                    ? "Enter coordinates as: x y z"
                    : "Enter text.";
            default -> hint = "";
        }
    }

    private void send() {
        ClientGuiState.sendAction(uuid, action.id(), field.getText());
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Target: " + playerName), width / 2, height / 2 - 28, 0xAAAAAA);
        if (!hint.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), width / 2, height / 2 + 40, 0xFFFF80);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
