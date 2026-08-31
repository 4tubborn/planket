package stubborn.planket.client.gui.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
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

        // General Cate
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

        // Functions Subcate
        SubCategoryBuilder functions = entries
                .startSubCategory(Component.translatable("options.planket.subcategory.functions"))
                .setExpanded(true);

        functions.add(
                entries.startBooleanToggle(
                                Component.translatable("options.planket.enable_scroll_memory"),
                                config.enableScrollMemory
                        )
                        .setDefaultValue(PlanketConfig.DEFAULT_SCROLL_MEMORY)
                        .setTooltip(Component.translatable("options.planket.enable_scroll_memory.tooltip"))
                        .setSaveConsumer(value -> config.enableScrollMemory = value)
                        .build()
        );

        functions.add(
                entries.startBooleanToggle(
                                Component.translatable("options.planket.ignore_right_tabs"),
                                config.ignoreRightTabs
                        )
                        .setDefaultValue(PlanketConfig.DEFAULT_IGNORE_RIGHT_TABS)
                        .setTooltip(Component.translatable("options.planket.ignore_right_tabs.tooltip"))
                        .setSaveConsumer(value -> config.ignoreRightTabs = value)
                        .build()
        );

        general.addEntry(functions.build());

        // Advanced Cate
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