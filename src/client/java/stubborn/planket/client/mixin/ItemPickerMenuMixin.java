package stubborn.planket.client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import stubborn.planket.client.config.PlanketConfig;

//修复scroll
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$ItemPickerMenu")
public abstract class ItemPickerMenuMixin extends AbstractContainerMenu {

    @Shadow public NonNullList<ItemStack> items;

    private static final SimpleContainer CONTAINER = CreativeModeInventoryScreen.CONTAINER;


    protected ItemPickerMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    /**
     * @author 4tubborn
     * @reason 行数由配置决定
     */
    @Overwrite
    public void scrollTo(float f) {
        int rows = PlanketConfig.getInstance().creativeRows;
        int rowIndex = Math.max((int)((double)(f * (float)this.calculateRowCount()) + 0.5), 0);
        for (int j = 0; j < rows; ++j) {
            for (int k = 0; k < 9; ++k) {
                int idx = k + (j + rowIndex) * 9;
                if (idx >= 0 && idx < this.items.size()) {
                    CONTAINER.setItem(k + j * 9, this.items.get(idx));
                } else {
                    CONTAINER.setItem(k + j * 9, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * @author 4tubborn
     * @reason 计算行数适配配置
     */
    @Overwrite
    public int calculateRowCount() {
        return Mth.positiveCeilDiv(this.items.size(), 9) - PlanketConfig.getInstance().creativeRows;
    }

    /**
     * @author 4tubborn
     * @reason 滚动阈值根据配置行数变化
     */
    @Overwrite
    public boolean canScroll() {
        int rows = PlanketConfig.getInstance().creativeRows;
        return this.items.size() > rows * 9;
    }
}