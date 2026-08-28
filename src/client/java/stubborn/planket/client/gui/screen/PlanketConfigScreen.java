package stubborn.planket.client.gui.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import stubborn.planket.client.config.PlanketConfig;

public final class PlanketConfigScreen {
    private PlanketConfigScreen() {}

    public static Screen create(Screen parent) {
        PlanketConfig config = PlanketConfig.getInstance();

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
                                Math.max(1, PlanketConfig.maxCreativeRows)
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

        ConfigCategory advanced = builder.getOrCreateCategory(
                Component.translatable("options.planket.category.advanced")
        );

        advanced.addEntry(
                entries.startIntSlider(
                                Component.translatable("options.planket.max_creative_rows"),
                                PlanketConfig.maxCreativeRows,
                                1,
                                PlanketConfig.MAX_CREATIVE_ROWS_LIMIT
                        )
                        .setDefaultValue(PlanketConfig.DEFAULT_MAX_CREATIVE_ROWS)
                        .setSaveConsumer(value -> {
                            PlanketConfig.maxCreativeRows = value;
                            config.creativeRows = Mth.clamp(config.creativeRows, 1, value);
                        } )
                        .build()
        );

        builder.setSavingRunnable(config::save);

        return builder.build();
    }
}