package stubborn.planket.client.mixin;

import mezz.jei.gui.overlay.IngredientListOverlay;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// JEI compat
@Mixin(IngredientListOverlay.class)
public class IngredientListOverlayMixin {

    @Inject(
            method = "isListDisplayed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void planket$hideInCreativeInventory(
            CallbackInfoReturnable<Boolean> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof CreativeModeInventoryScreen) {
            cir.setReturnValue(false);
        }
    }
}