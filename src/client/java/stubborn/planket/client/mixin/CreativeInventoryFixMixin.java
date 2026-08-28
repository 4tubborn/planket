package stubborn.planket.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

    @Shadow private boolean scrolling;

    public CreativeInventoryFixMixin(AbstractContainerMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    //修复原版滑块鼠标释放的bug
    @WrapMethod(method = "mouseReleased")
    private boolean fixScrollDragTabSwitch(MouseButtonEvent event, Operation<Boolean> original) {
        if (event.button() == 0 && this.scrolling) {
            this.scrolling = false;
            return true;
        }

        return original.call(event);
    }
    //针对模组的适配
}
