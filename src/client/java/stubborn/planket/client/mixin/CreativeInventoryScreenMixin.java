package stubborn.planket.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stubborn.planket.client.gui.InventoryPanel;
import stubborn.planket.client.handler.KeyHandler;
import stubborn.planket.client.util.CreativeGridUtil;
import stubborn.planket.client.util.ScreenInterface;
import stubborn.planket.client.config.PlanketConfig;
import static stubborn.planket.client.PlanketClient.LOGGER;


//增加inventory，重构左侧item行。
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractContainerScreen implements ScreenInterface {

    @Shadow private EditBox searchBox; // 搜索框
    @Shadow public static CreativeModeTab selectedTab;

    @Shadow protected abstract boolean checkTabClicked(CreativeModeTab tab, double d, double e);
    @Shadow private boolean hasClickedOutside;
    @Shadow private @Nullable Slot destroyItemSlot;
    @Shadow public static final SimpleContainer CONTAINER = new SimpleContainer(45);
    @Shadow private boolean scrolling;

    @Shadow private float scrollOffs;

    @Unique
    public int originalTopPos;

    @Unique
    private @Nullable InventoryPanel planket$inventoryPanel;

    @Unique
    private boolean planket$inventoryQuickCrafting;

    @Unique
    private int planket$quickCraftStartData;

    static {
        NonNullList<ItemStack> newList = NonNullList.createWithCapacity(PlanketConfig.maxCreativeRows * 9);
        for (int i = 0; i < PlanketConfig.maxCreativeRows * 9; i++) newList.add(ItemStack.EMPTY);
        CONTAINER.items = newList;
    }

    public CreativeInventoryScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean includeRightPanelInBounds(boolean original, double mouseX, double mouseY, int left, int top) {
        int inventoryX = left + this.imageWidth;
        int inventoryY = this.originalTopPos;

        boolean insideInventoryPanel =
                mouseX >= inventoryX
                        && mouseX < inventoryX + InventoryPanel.WIDTH
                        && mouseY >= inventoryY
                        && mouseY < inventoryY + InventoryPanel.HEIGHT;

        boolean result = original && !insideInventoryPanel;

        this.hasClickedOutside = result;
        return result;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void planket$prepareInventoryPanel(CallbackInfo ci) {
        Player player = this.minecraft.player;
        if (player != null && this.planket$inventoryPanel == null) {
            this.planket$inventoryPanel = new InventoryPanel(
                    this.minecraft,
                    (CreativeModeInventoryScreen.ItemPickerMenu)this.menu,
                    player,
                    CONTAINER
            );
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;selectTab(Lnet/minecraft/world/item/CreativeModeTab;)V", shift = At.Shift.BEFORE))
    private void captureOriginalTopPos(CallbackInfo ci) {
        this.originalTopPos = this.topPos;   // 此时 topPos 是原版 136 高度的居中值，且未被我们的逻辑改动过
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void initCustomGrid(CallbackInfo ci) {
        // 左移逻辑
        int offset = 100;
        this.leftPos -= offset;

        Player player = this.minecraft.player;
        if (player != null) {
            rebuildLeftPanel(player);
        }

        //搜索框复位
        if (this.searchBox != null) {
            this.searchBox.setX(this.leftPos + 82);
            // 完美对齐原版：当前真正居中后的 topPos + 6
            this.searchBox.setY(this.topPos + 6);
        }

        if (this.planket$inventoryPanel != null) {
            this.planket$inventoryPanel.init(this.imageWidth, this.originalTopPos - this.topPos);
            this.destroyItemSlot = this.planket$inventoryPanel.getDestroyItemSlot();
        }

        this.planket$updateFabricPageButtons();
    }

    @Inject(method = "removed", at = @At(value = "HEAD"))
    private void planket$removePanel(CallbackInfo ci) {
        planket$removeInventoryPanel();
    }

    @Unique
    private static Slot createCustomCreativeSlot(int index, int x, int y) {
        // 同样直接 new
        return new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeInventoryScreenMixin.CONTAINER, index, x, y);
    }

    @WrapWithCondition(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean wrapLeftBackground(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight) {
        if (CreativeGridUtil.getVisibleRows() <= 5) {
            return true; // 执行原版 blit
        }

        drawDynamicLeftBg(guiGraphics, pipeline, texture, x, y);
        return false; // 阻止原版 blit
    }

    @Unique
    private void drawDynamicLeftBg(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier texture, int leftX, int topY) {
        int rows = CreativeGridUtil.getVisibleRows();
        // 纹理切片坐标
        int texTopHeight = 89;          // 顶部区域（标题、搜索框、前5行物品背景）
        int texRowHeight = 18;          // 每额外一行的背景高度
        int texBottomHeight = 8;        // 底部收尾
        int texWidth = 195;             // 有效纹理宽度

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

        currentY -= 1;

        // 3. 底部收尾 (0,128) - 宽 194，高 8
        guiGraphics.blit(pipeline, texture,
                leftX, currentY,
                0, 128,
                texWidth, texBottomHeight,
                256, 256);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void renderPermanentInventoryBackground(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        if (this.planket$inventoryPanel != null) {
            this.planket$inventoryPanel.render(
                    guiGraphics,
                    this.leftPos + this.imageWidth,
                    this.originalTopPos,
                    i,
                    j
            );
        }
    }


    @Unique
    private void rebuildLeftPanel(Player player) {
        this.menu.slots.removeIf(s -> s.x < this.imageWidth);

        int rows = CreativeGridUtil.getVisibleRows();
        // 添加 rows 行物品槽
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                try {
                    Slot slot = createCustomCreativeSlot(
                            row * 9 + col,
                            9 + col * 18,
                            18 + row * 18
                    );
                    this.menu.slots.add(slot);
                } catch (Exception e) {
                    LOGGER.error("Errors when trying to rebuild left tab panel: ", e);
                }
            }
        }

        // 不可见热键栏，放置于屏幕外，不参与布局
        for (int col = 0; col < 9; col++) {
            Slot invisibleHotbar = new Slot(player.getInventory(), col, 9 + col * 18, -2000) {
                @Override public boolean isActive() { return false; }
            };
            this.menu.slots.add(invisibleHotbar);
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
    @ModifyConstant(
            method = "insideScrollbar",
            constant = @Constant(intValue = 112)
    )
    private int modifyScrollbarHeight(int original) {
        return this.imageHeight - 18 - 6;
    }

    @WrapMethod(method = "mouseDragged")
    private boolean planket$mouseDragged(MouseButtonEvent event, double dragX, double dragY, Operation<Boolean> original) {
        if (!this.scrolling) {
            return original.call(event, dragX, dragY);
        }

        int top = this.topPos + 18;
        int visibleHeight = this.imageHeight - 18 - 6;
        int bottom = top + visibleHeight;

        this.scrollOffs =
                ((float) event.y() - (float) top - 7.5F)
                        / ((float) (bottom - top) - 15.0F);

        this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);

        ((CreativeModeInventoryScreen.ItemPickerMenu) this.menu)
                .scrollTo(this.scrollOffs);

        return true;
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(intValue = 112, ordinal = 0))
    private int dynamicScrollbarHeightRender(int original) {
        return this.imageHeight - 18 - 6;
    }
    //滑块结束
    //禁用快捷栏
    /*@ModifyArg(
            method = "slotClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;getSlot(I)Lnet/minecraft/world/inventory/Slot;"
            ),
            index = 0
    )
    private int planket$redirectHotbarSlotIndex(int slotIndex) {
        if (slotIndex >= 45 && slotIndex < 54) {
            return PlanketConfig.getInstance().creativeRows * 9 + (slotIndex - 45);
        }

        return slotIndex;
    }*/

    @ModifyConstant(
            method = "slotClicked",
            constant = @Constant(intValue = 45)
    )
    private int planket$modifyHotbarStartIndex(int original) {
        return CreativeGridUtil.getHotbarStartIndex();
    }

    /*@ModifyExpressionValue(method = "slotClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getQuickcraftHeader(I)I"))
    private int disableQuickcraftSync(int original) {
        return 0;
    }*/

    //重新实现背包槽位shift与垃圾桶逻辑
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void planket$onSlotClicked(@Nullable Slot slot, int slotIndex, int mouseButton, ClickType clickType, CallbackInfo ci) {
        if (this.planket$inventoryPanel == null) {
            return;
        }

        if (clickType == ClickType.QUICK_CRAFT) {
            planket$handleInventoryQuickCraft(slot, mouseButton, ci);
            return;
        }

        if (this.planket$inventoryPanel.handleSlotClicked(
                slot,
                mouseButton,
                clickType
        )) {
            ci.cancel();
        }
    }

    @Unique
    private void planket$handleInventoryQuickCraft(
            @Nullable Slot slot,
            int quickCraftData,
            CallbackInfo ci
    ) {
        Player player = this.minecraft.player;
        if (player == null) {
            return;
        }

        int header = AbstractContainerMenu.getQuickcraftHeader(quickCraftData);

        if (header == 0) {
            this.planket$inventoryQuickCrafting = false;
            this.planket$quickCraftStartData = quickCraftData;
            return;
        }

        if (header == 1
                && slot instanceof CreativeModeInventoryScreen.SlotWrapper wrapper
                && this.planket$inventoryPanel.ownsSlot(slot)) {

            if (!this.planket$inventoryQuickCrafting) {
                player.inventoryMenu.clicked(
                        -999,
                        this.planket$quickCraftStartData,
                        ClickType.QUICK_CRAFT,
                        player
                );

                this.planket$inventoryQuickCrafting = true;
            }

            player.inventoryMenu.clicked(
                    wrapper.target.index,
                    quickCraftData,
                    ClickType.QUICK_CRAFT,
                    player
            );

            ci.cancel();
            return;
        }

        if (header == 2 && this.planket$inventoryQuickCrafting) {
            player.inventoryMenu.clicked(
                    -999,
                    quickCraftData,
                    ClickType.QUICK_CRAFT,
                    player
            );

            player.inventoryMenu.broadcastChanges();
            this.planket$inventoryQuickCrafting = false;

            // 不 cancel：
            // ItemPickerMenu 也需要收到自己的 END。
        }
    }

    @Override
    public int planket$getInventoryTopPos() {
        return this.originalTopPos;
    }
    @Override
    public int planket$getInventoryWidth() { return InventoryPanel.WIDTH; }
    @Override
    public int planket$getInventoryHeight() { return InventoryPanel.HEIGHT; }

    // 注入到 keyPressed 方法的首部
    @WrapMethod(method = "keyPressed")
    private boolean planket$keyPressed(KeyEvent event, Operation<Boolean> original) {
        if (KeyHandler.handleKeyPressed((CreativeModeInventoryScreen) (Object) this, event)) {
            return true;
        }

        return original.call(event);
    }

    @Unique
    private void planket$removeInventoryPanel() {
        if (this.planket$inventoryPanel == null) {
            return;
        }

        this.planket$inventoryPanel.removed();
        this.planket$inventoryPanel = null;
        this.destroyItemSlot = null;
    }

    /* In net.fabricmc.fabric.mixin.itemgroup.client.CreativeModeInventoryScreenMixin
    int xpos = leftPos + 171;
    int ypos = topPos + 4;

    CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
    addRenderableWidget(new FabricCreativeGuiComponents.ItemGroupButtonWidget(xpos + 10, ypos, FabricCreativeGuiComponents.Type.NEXT, self));
    addRenderableWidget(new FabricCreativeGuiComponents.ItemGroupButtonWidget(xpos, ypos, FabricCreativeGuiComponents.Type.PREVIOUS, self));
    */

    @Unique
    private void planket$updateFabricPageButtons() {
        int xPos = this.leftPos + 171;
        int yPos = this.topPos + 4;

        for (GuiEventListener child : this.children()) {
            if (!(child instanceof FabricCreativeGuiComponents.ItemGroupButtonWidget button)) {
                continue;
            }

            FabricCreativeGuiComponents.Type type =
                    ((ItemGroupButtonWidgetAccessor) button).planket$getType();

            button.setY(yPos);

            switch (type) {
                case PREVIOUS -> button.setX(xPos);
                case NEXT -> button.setX(xPos + 10);
                default -> LOGGER.error("Invalid button type: {}", type);
            }
        }
    }
}
