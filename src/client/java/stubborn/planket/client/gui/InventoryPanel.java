package stubborn.planket.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.item.CreativeModeTabs.INVENTORY_BACKGROUND;

/**
 * The permanent survival-inventory panel displayed beside the creative screen.
 *
 * <p>The slots in this panel are temporary client-side wrappers. Just like
 * vanilla's inventory-tab implementation, they are inserted directly into
 * {@link AbstractContainerMenu#slots}; {@code addSlot} must not be used here,
 * because it also grows the menu's synchronization tracking lists.</p>
 */
public final class InventoryPanel {
    public static final int WIDTH = 195;
    public static final int HEIGHT = 136;

    private final Minecraft minecraft;
    private final CreativeModeInventoryScreen.ItemPickerMenu menu;
    private final Player player;
    private final SimpleContainer creativeContainer;
    private final List<Slot> panelSlots = new ArrayList<>();

    private int slotOffsetX;
    private int slotOffsetY;
    private @Nullable Slot destroyItemSlot;
    private boolean initialized;

    public InventoryPanel(
            Minecraft minecraft,
            CreativeModeInventoryScreen.ItemPickerMenu menu,
            Player player,
            SimpleContainer creativeContainer
    ) {
        this.minecraft = minecraft;
        this.menu = menu;
        this.player = player;
        this.creativeContainer = creativeContainer;
    }

    /**
     * Creates the slots once. Repeated screen initialization (for example a
     * resize) reuses them unless their menu-relative coordinates changed.
     */
    public void init(int slotOffsetX, int slotOffsetY) {
        boolean layoutChanged = this.initialized
                && (this.slotOffsetX != slotOffsetX || this.slotOffsetY != slotOffsetY);

        if (layoutChanged) {
            removeSlots();
            this.initialized = false;
        }

        this.slotOffsetX = slotOffsetX;
        this.slotOffsetY = slotOffsetY;

        if (!this.initialized) {
            createSlots();
            this.initialized = true;
        } else {
            attachSlots();
        }
    }

    private void createSlots() {
        AbstractContainerMenu inventoryMenu = this.player.inventoryMenu;

        for (int index = 5; index < inventoryMenu.slots.size(); index++) {
            int x;
            int y;

            if (index < 9) {
                int armorIndex = index - 5;
                x = 54 + armorIndex / 2 * 54;
                y = 6 + armorIndex % 2 * 27;
            } else if (index == 45) {
                x = 35;
                y = 20;
            } else {
                int inventoryIndex = index - 9;
                x = 9 + inventoryIndex % 9 * 18;
                y = index >= 36
                        ? 112
                        : 54 + inventoryIndex / 9 * 18;
            }

            addTemporarySlot(new CreativeModeInventoryScreen.SlotWrapper(
                    inventoryMenu.getSlot(index),
                    index,
                    x + this.slotOffsetX,
                    y + this.slotOffsetY
            ));
        }

        this.destroyItemSlot = new Slot(
                this.creativeContainer,
                0,
                173 + this.slotOffsetX,
                112 + this.slotOffsetY
        ) {
            @Override
            public void set(@NonNull ItemStack stack) {
                super.set(ItemStack.EMPTY);
            }

            @Override
            public @NonNull ItemStack getItem() {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean mayPickup(@NonNull Player player) {
                return false;
            }
        };

        addTemporarySlot(this.destroyItemSlot);
    }

    private void attachSlots() {
        for (Slot slot : this.panelSlots) {
            if (!this.menu.slots.contains(slot)) {
                this.menu.slots.add(slot);
            }
        }
    }

    private void addTemporarySlot(Slot slot) {
        this.menu.slots.add(slot);
        this.panelSlots.add(slot);
    }

    private void removeSlots() {
        this.menu.slots.removeAll(this.panelSlots);
        this.panelSlots.clear();
        this.destroyItemSlot = null;
    }

    public void render(GuiGraphics graphics, int screenX, int screenY, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                INVENTORY_BACKGROUND,
                screenX, screenY,
                0.0F, 0.0F,
                256,
                256,
                256,
                256
        );

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                screenX + 73, screenY + 6, screenX + 105, screenY + 49,
                20,
                0.0625F,
                (float) mouseX, (float) mouseY,
                this.player
        );
    }

    /**
     * Routes every click on a panel wrapper to its real player-inventory slot.
     * Temporary wrappers added with slots.add do not receive a menu index, so
     * allowing the normal non-inventory-tab branch to use slot.index would
     * address the wrong ItemPickerMenu slot.
     */
    public boolean handleSlotClicked(@Nullable Slot slot, int mouseButton, ClickType clickType) {
        if (!ownsSlot(slot)) {
            return false;
        }

        if (clickType == ClickType.QUICK_CRAFT) {
            return false;
        }

        if (slot == this.destroyItemSlot) {
            if (clickType == ClickType.QUICK_MOVE) {
                for (int index = 0; index < this.player.inventoryMenu.getItems().size(); index++) {
                    this.player.inventoryMenu.getSlot(index).set(ItemStack.EMPTY);
                    this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, index);
                }
            } else {
                this.menu.setCarried(ItemStack.EMPTY);
            }

            return true;
        }

        if (!(slot instanceof CreativeModeInventoryScreen.SlotWrapper wrapper)) {
            return true;
        }

        if (!slot.mayPickup(this.player)) {
            return true;
        }

        int targetIndex = wrapper.target.index;

        if (clickType == ClickType.THROW) {
            if (!this.player.canDropItems() || !slot.hasItem()) {
                return true;
            }

            ItemStack dropped = slot.remove(
                    mouseButton == 0 ? 1 : slot.getItem().getMaxStackSize()
            );

            ItemStack remainder = slot.getItem();

            this.player.drop(dropped, true);
            this.minecraft.gameMode.handleCreativeModeItemDrop(dropped);
            this.minecraft.gameMode.handleCreativeModeItemAdd(remainder, targetIndex);

            return true;
        }

        this.player.inventoryMenu.clicked(targetIndex, mouseButton, clickType, this.player);
        this.player.inventoryMenu.broadcastChanges();

        return true;
    }

    public boolean ownsSlot(@Nullable Slot slot) {
        return slot != null && this.panelSlots.contains(slot);
    }

    public @Nullable Slot getDestroyItemSlot() {
        return this.destroyItemSlot;
    }

    public void removed() {
        removeSlots();
        this.initialized = false;
    }

    private static int getTargetIndex(Slot slot) {
        if (slot instanceof CreativeModeInventoryScreen.SlotWrapper wrapper) {
            return wrapper.target.index;
        }
        return -1;
    }
}
