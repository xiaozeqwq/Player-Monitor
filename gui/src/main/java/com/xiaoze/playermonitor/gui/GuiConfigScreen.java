package com.xiaoze.playermonitor.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Builds the Cloth Config settings screen for the GUI companion mod. */
public final class GuiConfigScreen {
    private GuiConfigScreen() {
    }

    public static Screen create(Screen parent) {
        GuiConfig config = GuiConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Player Monitor Settings"));

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(entries.startBooleanToggle(Text.literal("Auto open GUI"), config.autoOpen)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Open the monitor GUI automatically when the server authorizes this client."))
                .setSaveConsumer(value -> config.autoOpen = value)
                .build());

        general.addEntry(entries.startBooleanToggle(Text.literal("Show raw JSON"), config.showRawJson)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Also display the unprocessed JSON report."))
                .setSaveConsumer(value -> config.showRawJson = value)
                .build());

        general.addEntry(entries.startIntField(Text.literal("Detail max lines"), config.detailMaxLines)
                .setDefaultValue(200)
                .setMin(20)
                .setMax(2000)
                .setTooltip(Text.literal("Maximum number of report lines shown in the detail view."))
                .setSaveConsumer(value -> config.detailMaxLines = value)
                .build());

        builder.setSavingRunnable(GuiConfig::save);
        return builder.build();
    }
}
