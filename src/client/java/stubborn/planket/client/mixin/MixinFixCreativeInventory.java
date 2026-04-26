package stubborn.planket.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stubborn.planket.client.config.PlanketConfig;

@Mixin(CreativeModeInventoryScreen.class)
public class MixinFixCreativeInventory {

    @Shadow
    protected void selectTab(CreativeModeTab creativeModeTab) {

    }
    @Shadow private boolean scrolling;

    //隐藏type.INVENTORY，为了兼容性，不直接删除而是隐藏
    @Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true)
    private void hideInventoryTab(GuiGraphics guiGraphics, int i, int j, CreativeModeTab creativeModeTab, CallbackInfo ci) {
        if (!PlanketConfig.getInstance().enableInventoryTab &&
                creativeModeTab.getType() == CreativeModeTab.Type.INVENTORY) {
            ci.cancel();
        }
    }

    @Inject(method = "checkTabClicked", at = @At("HEAD"), cancellable = true)
    private void disableInventoryTabClick(CreativeModeTab tab, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        if (!PlanketConfig.getInstance().enableInventoryTab &&
                tab.getType() == CreativeModeTab.Type.INVENTORY) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
    private void blockSelectInventoryTab(CreativeModeTab tab, CallbackInfo ci) {
        if (!PlanketConfig.getInstance().enableInventoryTab &&
                tab.getType() == CreativeModeTab.Type.INVENTORY) {
            // 改为第一个非 Inventory 标签（通常是“建筑方块”等）
            CreativeModeTab defaultTab = CreativeModeTabs.getDefaultTab();
            if (defaultTab.getType() == CreativeModeTab.Type.INVENTORY) {
                // 极端情况：万一默认也是 Inventory，遍历找一个 CATEGORY 类型
                defaultTab = CreativeModeTabs.tabs().stream()
                        .filter(t -> t.getType() != CreativeModeTab.Type.INVENTORY)
                        .findFirst().orElse(tab);
            }
            this.selectTab(defaultTab);
            ci.cancel();
        }
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void hideInventoryTabTooltip(GuiGraphics guiGraphics, CreativeModeTab tab, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (!PlanketConfig.getInstance().enableInventoryTab &&
                tab.getType() == CreativeModeTab.Type.INVENTORY) {
            cir.setReturnValue(false);
        }
    }
    //隐藏好了

    //修复原版滑块鼠标释放的bug
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void fixScrollDragTabSwitch(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        // 只处理左键，且当正在拖动滑块时
        if (event.button() == 0 && this.scrolling) {
            this.scrolling = false;           // 重置拖动状态
            cir.setReturnValue(true);         // 直接消费事件，不再触发标签检测
        }
    }
}
