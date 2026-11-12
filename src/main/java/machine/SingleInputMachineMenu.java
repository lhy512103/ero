package machine;

import item.UpgradeItem;
import item.UpgradeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Menu shared by every {@link SingleInputMachineBlockEntity}: input (79,17), output (79,61),
 * four upgrade slots down the right edge, and the player inventory. Synced data is progress,
 * max progress and energy; subclasses can append their own values through the extra-data hooks.
 */
public abstract class SingleInputMachineMenu<T extends SingleInputMachineBlockEntity> extends AbstractContainerMenu {

    protected final T blockEntity;
    private final int processSlots;
    private final int[] progress;
    private final int[] maxProgress;
    private long energy;
    private long maxEnergy;

    protected SingleInputMachineMenu(MenuType<?> type, int containerId, Inventory playerInv, T blockEntity) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.processSlots = blockEntity.getProcessSlots();
        this.progress = new int[processSlots];
        this.maxProgress = new int[processSlots];

        var inventory = blockEntity.getItemHandler();
        int upgradeStart = 2 * processSlots;
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, 0, 79, 17));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, processSlots, 79, 61));
        int[] upgradeY = {9, 28, 48, 67};
        for (int i = 0; i < 4; i++) addSlot(new UpgradeSlot(inventory, inventory::set, upgradeStart + i, 152, upgradeY[i]));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 87 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 143));

        int base = processSlots * 2;
        int dataCount = base + 4 + extraDataCount();
        addDataSlots(new SimpleContainerData(dataCount) {
            @Override public int get(int index) {
                if (index < processSlots) return blockEntity.getProgress(index);
                if (index < base) return blockEntity.getMaxProgress(index - processSlots);
                long stored = blockEntity.getEnergyStorage().getEnergyStored();
                long capacity = blockEntity.getEnergyStorage().getMaxEnergyStored();
                return switch (index - base) {
                    case 0 -> (int) (stored & 0xFFFFFFFFL);
                    case 1 -> (int) (stored >>> 32);
                    case 2 -> (int) (capacity & 0xFFFFFFFFL);
                    case 3 -> (int) (capacity >>> 32);
                    default -> getExtraData(index - base - 4);
                };
            }
            @Override public void set(int index, int value) {
                if (index < processSlots) { progress[index] = value; return; }
                if (index < base) { maxProgress[index - processSlots] = value; return; }
                switch (index - base) {
                    case 0 -> energy = (energy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 1 -> energy = (energy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 2 -> maxEnergy = (maxEnergy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 3 -> maxEnergy = (maxEnergy & 0xFFFFFFFFL) | ((long) value << 32);
                    default -> setExtraData(index - base - 4, value);
                }
            }
            @Override public int getCount() { return dataCount; }
        });
    }

    /** Number of additional synced integers. Called from the constructor, so it must not read subclass fields. */
    protected int extraDataCount() { return 0; }

    /** Server side: current value of extra integer {@code index}. */
    protected int getExtraData(int index) { return 0; }

    /** Client side: receives extra integer {@code index}. */
    protected void setExtraData(int index, int value) { }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            int machineSlots = processSlots * 2 + 4;
            if (index < machineSlots) {
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else if (slotStack.getItem() instanceof UpgradeItem) {
                if (!this.moveItemStackTo(slotStack, processSlots * 2, machineSlots, false)
                        && !this.moveItemStackTo(slotStack, 0, processSlots, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(slotStack, 0, processSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (slotStack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    public T getBlockEntity() { return blockEntity; }
    public int getProgress(int slot) { return progress[slot]; }
    public int getMaxProgress(int slot) { return maxProgress[slot]; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public int getProcessSlots() { return processSlots; }
}
