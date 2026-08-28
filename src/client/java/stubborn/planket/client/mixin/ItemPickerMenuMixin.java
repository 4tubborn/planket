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
import stubborn.planket.client.util.CreativeGridUtil;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class ItemPickerMenuMixin extends AbstractContainerMenu {

    protected ItemPickerMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @ModifyConstant(
            method = "calculateRowCount",
            constant = @Constant(intValue = 5)
    )
    private int planket$modifyVisibleRows(int original) {
        return CreativeGridUtil.getVisibleRows();
    }

    @ModifyConstant(
            method = "canScroll",
            constant = @Constant(intValue = 45)
    )
    private int planket$modifyVisibleSlotCapacity(int original) {
        return CreativeGridUtil.getVisibleSlotCount();
    }

    @ModifyConstant(
            method = "scrollTo",
            constant = @Constant(intValue = 5)
    )
    private int planket$modifyVisibleRows2(int original) {
        return CreativeGridUtil.getVisibleRows();
    }
}