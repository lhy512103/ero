package machine.metal_former.menu;

import init.ModMenus;
import item.MoldItem;
import item.MoldSlot;
import machine.SingleInputMachineMenu;
import machine.metal_former.MetalFormerBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The shared single-input layout plus one mold slot right of the arrow. */
public class MetalFormerMenu extends SingleInputMachineMenu<MetalFormerBlockEntity> {

    /** Position of the mold slot inside the screen. */
    public static final int MOLD_X = 107, MOLD_Y = 39;
    /** Machine slots (input, output, 4 upgrades) and the 36 player slots come first. */
    private static final int PLAYER_START = 6, MOLD_INDEX = 42;

    public MetalFormerMenu(int containerId, Inventory playerInv, MetalFormerBlockEntity blockEntity) {
        super(ModMenus.METAL_FORMER.get(), containerId, playerInv, blockEntity);
        var inventory = blockEntity.getItemHandler();
        addSlot(new MoldSlot(inventory, inventory::set, MetalFormerBlockEntity.MOLD_SLOT, MOLD_X, MOLD_Y));
    }

    public boolean hasMold() { return slots.get(MOLD_INDEX).hasItem(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        boolean fromMold = index == MOLD_INDEX;
        if (!fromMold && !(index >= PLAYER_START && slot.getItem().getItem() instanceof MoldItem)) return super.quickMoveStack(player, index);
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = fromMold ? moveItemStackTo(stack, PLAYER_START, MOLD_INDEX, true)
            : moveItemStackTo(stack, MOLD_INDEX, MOLD_INDEX + 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }
}
