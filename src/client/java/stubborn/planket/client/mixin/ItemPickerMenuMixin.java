package stubborn.planket.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import stubborn.planket.client.config.PlanketConfig;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$ItemPickerMenu")
public abstract class ItemPickerMenuMixin extends AbstractContainerMenu {

    @Final
    @Shadow
    public NonNullList<ItemStack> items;

    protected ItemPickerMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @ModifyReturnValue(
            method = "calculateRowCount",
            at = @At("RETURN")
    )
    private int planket$modifyRowCount(int original) {
        int rows = PlanketConfig.getInstance().creativeRows;

        return Math.max(
                Mth.positiveCeilDiv(this.items.size(), 9) - rows,
                0
        );
    }

    @ModifyReturnValue(
            method = "canScroll",
            at = @At("RETURN")
    )
    private boolean planket$modifyCanScroll(boolean original) {
        int rows = PlanketConfig.getInstance().creativeRows;

        return this.items.size() > rows * 9;
    }

    @ModifyConstant(
            method = "scrollTo",
            constant = @Constant(intValue = 5)
    )
    private int planket$modifyVisibleRows(int original) {
        return PlanketConfig.getInstance().creativeRows;
    }
}