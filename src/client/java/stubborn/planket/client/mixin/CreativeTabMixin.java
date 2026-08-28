package stubborn.planket.client.mixin;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;

//隐藏type.INVENTORY
@Mixin(CreativeModeTab.class)
public abstract class CreativeTabMixin {

    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void onShouldDisplay(CallbackInfoReturnable<Boolean> cir) {
        CreativeModeTab instance = (CreativeModeTab) (Object) this;

        // 举例：如果你想隐藏原版的 INVENTORY（物品栏）标签页
        // 我们可以直接通过 CreativeModeTabs 里注册的 Key 来安全比对
        if (instance.getType() == CreativeModeTab.Type.INVENTORY) {
            cir.setReturnValue(false);
        }

        // 或者如果你有自定义标签页的实例，也可以直接比对：
        // if (instance == MyModTabs.MY_CUSTOM_TAB) { cir.setReturnValue(false); }
    }
}