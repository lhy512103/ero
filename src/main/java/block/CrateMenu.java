package block;

import init.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 27-slot (3x9) storage menu for the plastic crate. */
public class CrateMenu extends AbstractContainerMenu {
    public static final int ROWS = 3;
    public static final int COLS = 9;
    public static final int CRATE_SLOTS = ROWS * COLS;

    private final Container container;

    public CrateMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CRATE_SLOTS));
    }

    public CrateMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenus.CRATE.get(), containerId);
        checkContainerSize(container, CRATE_SLOTS);
        this.container = container;
        container.startOpen(playerInventory.player);

        // Crate 3x9 slots
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                this.addSlot(new Slot(container, col + row * COLS, 8 + col * 18, 18 + row * 18));
            }
        }
        // Player inventory 3x9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar 1x9
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < CRATE_SLOTS) {
                if (!this.moveItemStackTo(stack, CRATE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, CRATE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
