package stubborn.planket.client.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stubborn.planket.client.config.PlanketConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeInventoryScreen extends AbstractContainerScreen {

    @Shadow private EditBox searchBox; // 对应源码第 17 行 [cite: 17]
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow protected abstract boolean checkTabClicked(CreativeModeTab tab, double d, double e);
    @Shadow private boolean hasClickedOutside;
    @Shadow private @Nullable List<Slot> originalSlots;
    @Shadow private @Nullable Slot destroyItemSlot;
    @Shadow static final SimpleContainer CONTAINER = new SimpleContainer(45);
    @Shadow private boolean scrolling;

    @Shadow protected abstract void selectTab(CreativeModeTab creativeModeTab);
    @Shadow private float scrollOffs;

    @Unique
    public int actualImageWidth = this.imageWidth * 2 ;
    @Unique
    public int originalTopPos;

    public MixinCreativeInventoryScreen(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void includeRightPanelInBounds(double d, double e, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        boolean bl = d < (double)i || e < (double)j || d >= (double)(i + 2 * this.imageWidth) || e >= (double)(j + this.imageHeight);
        this.hasClickedOutside = bl && !this.checkTabClicked(selectedTab, d, e);
        cir.setReturnValue(this.hasClickedOutside);
    }

    @Unique
    private static final Class<?> slotWrapperClass;
    @Unique
    private static final Constructor<?> slotWrapperConstructor;
    @Unique
    private static Field fieldSlotWrapperTarget;

    static {
        try {
            slotWrapperClass = Class.forName(
                    "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper"
            );
            slotWrapperConstructor = slotWrapperClass.getDeclaredConstructor(
                    Slot.class, int.class, int.class, int.class
            );
            slotWrapperConstructor.setAccessible(true);
            fieldSlotWrapperTarget = slotWrapperClass.getDeclaredField("target");
            fieldSlotWrapperTarget.setAccessible(true);
            SimpleContainer container = CONTAINER; // CONTAINER 是你已经 @Shadow 的字段
            Field itemsField = SimpleContainer.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            NonNullList<ItemStack> newList = NonNullList.createWithCapacity(108);
            for (int i = 0; i < 108; i++) newList.add(ItemStack.EMPTY);
            itemsField.set(container, newList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access SlotWrapper", e);
        }
    }

    @Unique
    private int getSlotWrapperIndex(Slot slot) {
        if (slotWrapperClass == null || !slotWrapperClass.isInstance(slot)) {
            return -1;   // 不是 SlotWrapper（比如垃圾桶），直接返回 -1
        }
        try {
            Slot targetSlot = (Slot) fieldSlotWrapperTarget.get(slot);
            return targetSlot.index;
        } catch (IllegalAccessException e) {
            return -1;
        }
    }

    @Unique
    private Slot createWrapper(Slot target, int index, int x, int y) throws Exception {
        return (Slot) slotWrapperConstructor.newInstance(target, index, x, y);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;selectTab(Lnet/minecraft/world/item/CreativeModeTab;)V", shift = At.Shift.BEFORE))
    private void captureOriginalTopPos(CallbackInfo ci) {
        this.originalTopPos = this.topPos;   // 此时 topPos 是原版 136 高度的居中值，且未被我们的逻辑改动过
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void initCustomGrid(CallbackInfo ci) {
        // 原有左移逻辑
        int offset = 100;
        this.leftPos -= offset;
        if (this.searchBox != null) {
            this.searchBox.setX(this.leftPos + 82);
        }
    }

    @Unique
    private static Slot createCustomCreativeSlot(SimpleContainer container, int index, int x, int y) throws Exception {
        // 反射获取 CustomCreativeSlot 的构造器（私有内部类）
        Class<?> customSlotClass = Class.forName(
                "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$CustomCreativeSlot"
        );
        Constructor<?> ctor = customSlotClass.getDeclaredConstructor(
                Container.class, int.class, int.class, int.class
        );
        ctor.setAccessible(true);
        return (Slot) ctor.newInstance(container, index, x, y);
    }

    @Unique
    private static final Identifier INVENTORY_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_inventory.png");

    @Redirect(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void redirectLeftBackground(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier texture,
                                        int x, int y, float u, float v, int width, int height, int texWidth, int texHeight) {
        int rows = PlanketConfig.getInstance().creativeRows;
        if (rows <= 5) {
            // 保留原版完整纹理
            guiGraphics.blit(pipeline, texture, x, y, u, v, width, height, texWidth, texHeight);
        } else {
            drawDynamicLeftBg(guiGraphics, pipeline, texture, x, y);
        }
    }

    @Unique
    private void drawDynamicLeftBg(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier texture, int leftX, int topY) {
        int rows = PlanketConfig.getInstance().creativeRows;
        // 纹理切片坐标
        int texTopHeight = 89;          // 顶部区域（标题、搜索框、前5行物品背景）
        int texRowHeight = 18;          // 每额外一行的背景高度
        int texBottomHeight = 6;        // 底部收尾
        int texWidth = 194;             // 有效纹理宽度

        int currentY = topY;

        // 1. 顶部区域 (0,0) - 宽 194，高 89
        guiGraphics.blit(pipeline, texture,
                leftX, currentY,
                0, 0,
                texWidth, texTopHeight,
                256, 256);
        currentY += texTopHeight;

        // 2. 额外物品行（第5行到第 rows 行）
        int extraRows = rows - 4;
        for (int r = 0; r < extraRows; r++) {
            guiGraphics.blit(pipeline, texture,
                    leftX, currentY,
                    0, 89,               // 单行背景纹理坐标
                    texWidth, texRowHeight,
                    256, 256);
            currentY += texRowHeight;
        }

        // 3. 底部收尾 (0,129) - 宽 194，高 7
        guiGraphics.blit(pipeline, texture,
                leftX, currentY,
                0, 129,
                texWidth, 7,
                256, 256);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void renderPermanentInventoryBackground(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {

        // 使用源码第 428 行相同的参数结构
        // 参数: Pipeline, Texture, x, y, u, v, width, height, textureWidth, textureHeight
        int x = this.leftPos + this.imageWidth;
        int y = this.originalTopPos;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                INVENTORY_TEXTURE,
                x, y,
                0.0F, 0.0F,
                256, 256,
                256, 256
        );

        if (this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x + 73 , y + 6, x + 105, y + 49, 20, 0.0625F, (float)i, (float)j, this.minecraft.player);
        }
    }


    /**
     * 映射常驻 Slot 到右侧面板，每次切换标签页都重新new，因为原版会在切换页面时clear
     */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void addPermanentSlots(CreativeModeTab tab, CallbackInfo ci) {
        Player player = this.minecraft.player;
        if (player == null || this.menu == null || slotWrapperConstructor == null) return;

        // ★ 清除上一次残留的右侧面板槽位（包括旧的垃圾桶）
        this.menu.slots.removeIf(slot -> slot.x >= this.imageWidth);
        // 同时把销毁槽引用置空，避免指向被移除的对象
        this.destroyItemSlot = null;

        rebuildLeftPanel(player);

        InventoryMenu survivalMenu = player.inventoryMenu;
        AbstractContainerMenuAccessor menuAccessor = (AbstractContainerMenuAccessor) this.menu;

        // 偏移起点：主面板宽度 (195)
        int xOffset = this.imageWidth;
        int deltaY = this.originalTopPos - this.topPos;

        try{
            AbstractContainerMenu abstractContainerMenu = this.minecraft.player.inventoryMenu;
            if (this.originalSlots == null) {
                this.originalSlots = ImmutableList.copyOf(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots);
            }

            //((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.clear();
            //原版的浆糊逻辑
            for(int i = 0; i < abstractContainerMenu.slots.size(); ++i) {int n;int j;
                if (i >= 5 && i < 9) {
                    int k = i - 5;
                    int l = k / 2;
                    int m = k % 2;
                    n = 54 + l * 54;
                    j = 6 + m * 27;
                } else if (i >= 0 && i < 5) {
                    n = -2000;
                    j = -2000;
                } else if (i == 45) {
                    n = 35;
                    j = 20;
                } else {
                    int k = i - 9;
                    int l = k % 9;
                    int m = k / 9;
                    n = 9 + l * 18;
                    if (i >= 36) {
                        j = 112;
                    } else {
                        j = 54 + m * 18;
                    }
                }

                Slot slot = (Slot) slotWrapperConstructor.newInstance((Slot)abstractContainerMenu.slots.get(i), i, n + xOffset, j + deltaY);

                // 用你的 Accessor 添加
                ((AbstractContainerMenuAccessor)this.menu).callAddSlot(slot);

                //Slot slot = new CreativeModeInventoryScreen.SlotWrapper((Slot)abstractContainerMenu.slots.get(i), i, xOffset+n, j);
                //menuAccessor.callAddSlot(createWrapper(survivalMenu.getSlot(i), i, xOffset + 9 + j * 18, 54 + i * 18));
                //((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.add(slot);
            }
            // 在 selectTab 的循环之后
            this.destroyItemSlot = new Slot(CONTAINER, 0, 173 + xOffset, 112 + deltaY) {
                @Override
                public void set(ItemStack stack) {
                    // 核心：强制设为空，实现销毁效果
                    super.set(ItemStack.EMPTY);
                }

                @Override
                public ItemStack getItem() {
                    // 渲染隔离：强制返回空。即便 ItemPickerMenu 想给你填东西，渲染器在这里也拿不到数据
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean mayPickup(Player player) {
                    // 只能进不能出
                    return false;
                }

                @Override
                public boolean isActive() {
                    // 确保非 INVENTORY 标签页下它也参与交互逻辑
                    return true;
                }
            };

            ((AbstractContainerMenuAccessor)this.menu).callAddSlot(this.destroyItemSlot);

            //this.destroyItemSlot = new Slot(CONTAINER, 0, 173 + xOffset, 112);
            //this.menu.slots.add(this.destroyItemSlot);
            //((AbstractContainerMenuAccessor)this.menu).callAddSlot(this.destroyItemSlot);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Unique
    private void rebuildLeftPanel(Player player) {
        this.menu.slots.removeIf(s -> s.x < this.imageWidth);

        int rows = PlanketConfig.getInstance().creativeRows;
        // 添加 rows 行物品槽
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                try {
                    Slot slot = createCustomCreativeSlot(
                            CONTAINER,
                            row * 9 + col,
                            9 + col * 18,
                            18 + row * 18
                    );
                    ((AbstractContainerMenuAccessor) this.menu).callAddSlot(slot);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 不可见热键栏，放置于屏幕外，不参与布局
        for (int col = 0; col < 9; col++) {
            Slot invisibleHotbar = new Slot(player.getInventory(), col, 9 + col * 18, -2000) {
                @Override public boolean isActive() { return false; }
            };
            ((AbstractContainerMenuAccessor) this.menu).callAddSlot(invisibleHotbar);
        }

        ((CreativeModeInventoryScreen.ItemPickerMenu) this.menu).scrollTo(this.scrollOffs);

        int newImageHeight = this.imageHeight;
        if (rows > 5) {
            newImageHeight = 18 + rows * 18 + 6;   // 6格/行 + 间隔 + 标签边距
        }

        // 只有当高度发生变化时才调整垂直位置，避免每次切标签都跳动
        if (this.imageHeight != newImageHeight) {
            this.imageHeight = newImageHeight;
            // 重新居中：根据当前窗口高度和新的界面高度计算 topPos
            // 注意：leftPos 已在 init 中调整，topPos 需要在父类 init 后修正
            this.topPos = (this.height - this.imageHeight) / 2;
        }
    }
    //滑块适配>=6行数
    @Overwrite
    public boolean insideScrollbar(double d, double e) {
        int i = this.leftPos;
        int j = this.topPos;
        int k = i + 175;
        int l = j + 18;
        int m = k + 14;
        int visibleHeight = this.imageHeight - 18 - 4; // 与背景匹配的可用高度
        int n = l + visibleHeight;
        return d >= (double)k && e >= (double)l && d < (double)m && e < (double)n;
    }

    @Overwrite
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double d, double e) {
        if (this.scrolling) {
            int top = this.topPos + 18;
            int visibleHeight = this.imageHeight - 18 - 4;   // 与 insideScrollbar 统一
            int bottom = top + visibleHeight;
            this.scrollOffs = ((float)mouseButtonEvent.y() - (float)top - 7.5F) / ((float)(bottom - top) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
            return true;
        }
        return super.mouseDragged(mouseButtonEvent, d, e);
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(intValue = 112, ordinal = 0))
    private int dynamicScrollbarHeightRender(int original) {
        return this.imageHeight - 18 - 4;
    }
    //滑块结束
    //禁用快捷栏
    @Redirect(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getQuickcraftHeader(I)I"))
    private int disableQuickcraftSync(int header) {
        return 0; // 条件 AbstractContainerMenu.getQuickcraftHeader(j) == 2 永远为假
    }
    //重新实现背包槽位shift与垃圾桶逻辑
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(Slot slot, int slotIndex, int mouseButton, ClickType clickType, CallbackInfo ci) {
        // 1. 搜索框光标（原版逻辑）
        /*if (this.isCreativeSlot(slot)) {
            this.searchBox.moveCursorToEnd(false);
            this.searchBox.setHighlightPos(0);
        }*/

        // 2. 垃圾桶处理
        if (slot == this.destroyItemSlot && slot.container == CONTAINER) {
            if (clickType == ClickType.QUICK_MOVE) {
                for (int k = 0; k < this.minecraft.player.inventoryMenu.slots.size(); k++) {
                    this.minecraft.player.inventoryMenu.getSlot(k).set(ItemStack.EMPTY);
                    this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, k);
                }
            } else {
                if (this.minecraft.player != null) {
                    this.minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
                }
            }
            ci.cancel();
            return;
        }

        // 3. 核心：如果是右侧常驻面板的槽位且是 QuickMove
        if (slot != null && slot.x >= this.imageWidth && clickType == ClickType.QUICK_MOVE) {
            int realIndex = getSlotWrapperIndex(slot);
            if (realIndex == -1) {
                ci.cancel();
                return;
            }

            if (this.minecraft.player != null) {
                this.minecraft.player.inventoryMenu.clicked(realIndex, mouseButton, clickType, this.minecraft.player);
                this.minecraft.player.inventoryMenu.broadcastChanges();
            }
            ci.cancel();
        }


        // 注意：其他所有情况（左侧物品网格、普通点击、投掷等）都不 cancel，原版逻辑会完美执行
    }
    //同步到生存模式物品栏，但是有点屎山，可能会改
    @Inject(method = "slotClicked", at = @At("TAIL"))
    private void onSlotClickedTail(Slot slot, int slotIndex, int mouseButton, ClickType clickType, CallbackInfo ci) {
        // 只处理右侧常驻面板的有效槽位（排除垃圾桶）
        if (slot != null && slot.x >= this.imageWidth && slot != this.destroyItemSlot) {
            int realIndex = getSlotWrapperIndex(slot);
            if (realIndex != -1 && this.minecraft.player != null) {
                this.minecraft.player.inventoryMenu.broadcastChanges();
            }
        }
    }

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

