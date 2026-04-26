package stubborn.planket.client.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
                Component.translatable("options.planket.creative_rows").append(": ").append(String.valueOf(config.creativeRows)),
                (config.creativeRows - 1) / 11.0
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("options.planket.creative_rows").append(": ").append(String.valueOf(config.creativeRows)));
            }

            @Override
            protected void applyValue() {
                config.creativeRows = Mth.clamp((int)(this.value * 11 + 1.5), 1, 12);
                config.save();
                updateMessage();
            }

            @Override
            public void onRelease(MouseButtonEvent event) {
                int target = Mth.clamp((int)(this.value * 11 + 1.5), 1, 12);
                if (config.creativeRows != target) {
                    config.creativeRows = target;
                    config.save();
                }
                this.value = (target - 1) / 11.0;
                updateMessage();
                super.onRelease(event);
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