package stubborn.planket.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//修复

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryFixMixin extends AbstractContainerScreen {

    protected CreativeInventoryFixMixin() {
        super(null, null, null);
    }

    @Shadow
    protected void selectTab(CreativeModeTab creativeModeTab) {

    }
    @Shadow private boolean scrolling;
    //@Shadow private int leftPos;
    //@Shadow private int topPos;
    //@Shadow private int imageWidth;
    @Unique
    private int originalTopPos;
    @Unique
    private int originalImageHeight;


    @Inject(method = "init", at = @At("HEAD"))
    private void captureRightPanelTop(CallbackInfo ci) {
        this.originalTopPos = this.topPos;
        this.originalImageHeight = this.imageHeight;
    }

    //修复原版滑块鼠标释放的bug
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void fixScrollDragTabSwitch(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        // 只处理左键，且当正在拖动滑块时
        if (event.button() == 0 && this.scrolling) {
            this.scrolling = false;           // 重置拖动状态
            cir.setReturnValue(true);         // 直接消费事件，不再触发标签检测
        }
    }

    /*@Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void moveEffectsToRightPanel(EffectsInInventory effects, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int oldLeft = this.leftPos;
        int oldTop  = this.topPos;

        // 将状态效果移到右侧面板上方 20 像素处
        this.leftPos = this.leftPos + this.imageWidth;
        this.topPos  = this.originalTopPos + this.originalImageHeight + 80;

        effects.render(guiGraphics, mouseX, mouseY);

        this.leftPos = oldLeft;
        this.topPos  = oldTop;
    }*/

    //针对模组的适配
}
