package stubborn.planket.client.gui.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import stubborn.planket.client.config.PlanketConfig;

public final class PlanketConfigScreen {
    private PlanketConfigScreen() {}

    public static Screen create(Screen parent) {
        PlanketConfig config = PlanketConfig.getInstance();
        int rowNum = Math.max(1, PlanketConfig.slotMaxCapacity / 9);

        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Component.translatable("options.planket.title"));

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("options.planket.category.general")
        );

        general.addEntry(
                entries.startIntSlider(
                                Component.translatable("options.planket.creative_rows"),
                                config.creativeRows,
                                1,
                                rowNum
                        )
                        .setDefaultValue(PlanketConfig.DEFAULT_CREATIVE_ROWS)
                        .setSaveConsumer(value -> config.creativeRows = value)
                        .build()
        );

        general.addEntry(
                entries.startBooleanToggle(
                                Component.translatable("options.planket.enable_scroll_memory"),
                                config.enableScrollMemory
                        )
                        .setDefaultValue(PlanketConfig.DEFAULT_SCROLL_MEMORY)
                        .setTooltip(Component.translatable("options.planket.enable_scroll_memory.tooltip"))
                        .setSaveConsumer(value -> config.enableScrollMemory = value)
                        .build()
        );

        builder.setSavingRunnable(config::save);

        return builder.build();
    }
}