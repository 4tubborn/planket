package stubborn.planket.client.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import stubborn.planket.client.config.PlanketConfig;

public class PlanketConfigScreen extends Screen {
    private final Screen parent;

    public PlanketConfigScreen(Screen parent) {
        super(Component.literal("Planket Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PlanketConfig config = PlanketConfig.getInstance();

        int btnWidth = 200;
        int btnHeight = 20;

        int startX = this.width / 2 - btnWidth / 2;
        int startY = 40;

        int vertPadding = btnHeight + 5;
        int horPadding = 10;
        //为了适配可变的行数，inventory实际上已经不可用了
        /*this.addRenderableWidget(Button.builder(
                        Component.translatable("options.planket.enable_inventory_tab").append(": ")
                                .append(config.enableInventoryTab ? Component.translatable("options.on") : Component.translatable("options.off")),
                        btn -> {
                            config.enableInventoryTab = !config.enableInventoryTab;
                            config.save();
                            btn.setMessage(
                                    Component.translatable("options.planket.enable_inventory_tab").append(": ")
                                    .append(config.enableInventoryTab ? Component.translatable("options.on") : Component.translatable("options.off"))
                            );
                        })
                .pos(startX, startY)
                .size(btnWidth, btnHeight)
                .build()
        );*/

        // 行数滑块
        this.addRenderableWidget(new AbstractSliderButton(
                startX, startY + vertPadding,
                btnWidth, btnHeight,
                (Component.translatable("options.planket.creative_rows").append(": ")
                        .append(String.valueOf(config.creativeRows))),
                (config.creativeRows - 1) / 11.0   // 将 1~12 映射到 0~1
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("options.planket.creative_rows").append(": ")
                        .append(String.valueOf(config.creativeRows)));
            }

            @Override
            protected void applyValue() {
                // value 范围 0~1，转换为 1~12
                config.creativeRows = (int)(this.value * 11 + 1.5); // 四舍五入到最近整数
                if (config.creativeRows < 1) config.creativeRows = 1;
                if (config.creativeRows > 12) config.creativeRows = 12;
                config.save();
                updateMessage();
            }
        });
        //“完成”按钮
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        btn -> this.minecraft.setScreen(parent))
                .pos(this.width / 2 - 100, this.height - 30)
                .size(200, 20)
                .build()
        );
    }
}