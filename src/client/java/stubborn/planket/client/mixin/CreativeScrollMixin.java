package stubborn.planket.client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stubborn.planket.client.handler.CreativeScrollManager;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScrollMixin {

    @Unique
    private boolean planket$initializing;
    //在init时做标记，防止selectTab误触发
    @Inject(
            method = "init",
            at = @At("HEAD")
    )
    private void planket$initHead(CallbackInfo ci) {
        planket$initializing = true;
    }

    @Inject(
            method = "init",
            at = @At("TAIL")
    )
    private void planket$initTail(CallbackInfo ci) {
        planket$initializing = false;
    }

    @Shadow
    private float scrollOffs;

    @Shadow
    private static CreativeModeTab selectedTab;

    @Inject(
            method = "selectTab",
            at = @At("HEAD")
    )
    private void planket$saveScroll(
            CreativeModeTab tab,
            CallbackInfo ci
    ) {
        if(planket$initializing) {return;}
        CreativeScrollManager.set(
                selectedTab,
                this.scrollOffs
        );
        CreativeScrollManager.updateLastTab(selectedTab);
    }

    @Inject(
            method = "selectTab",
            at = @At("TAIL")
    )
    private void planket$restoreScroll(
            CreativeModeTab tab,
            CallbackInfo ci
    ) {
        float scroll = CreativeScrollManager.get(tab);

        this.scrollOffs = scroll;

        (((CreativeModeInventoryScreen) (Object) this).menu)
                .scrollTo(scroll);
    }

    @Inject(
            method = "removed",
            at = @At("HEAD")
    )
    private void planket$saveScrollOnClose(CallbackInfo ci) {
        CreativeScrollManager.set(
                selectedTab,
                this.scrollOffs
        );
    }
}