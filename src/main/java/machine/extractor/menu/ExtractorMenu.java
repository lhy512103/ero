package machine.extractor.menu;

import init.ModMenus;
import item.UpgradeItem;
import item.UpgradeSlot;
import machine.extractor.ExtractorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class ExtractorMenu extends AbstractContainerMenu {

    private final ExtractorBlockEntity blockEntity;
    private final int processSlots;

    private final int[] progress;
    private final int[] maxProgress;
    private long energy = 0;
    private long maxEnergy = 0;

    public ExtractorMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (ExtractorBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public ExtractorMenu(int containerId, Inventory playerInv, ExtractorBlockEntity blockEntity) {
        super(ModMenus.EXTRACTOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.processSlots = blockEntity.getProcessSlots();

        this.progress = new int[processSlots];
        this.maxProgress = new int[processSlots];

        int inputStart = 0;
        int outputStart = processSlots;
        int upgradeStart = 2 * processSlots;

        // 输入槽 (79, 17)、输出槽 (79, 61)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, inputStart, 79, 17));
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, outputStart, 79, 61));

        // 升级插件槽 1-4: (152, 9) (152, 28) (152, 48) (152, 67)
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart, 152, 9));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 1, 152, 28));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 2, 152, 48));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 3, 152, 67));

        // 玩家物品栏 (8, 87)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 87 + row * 18));
            }
        }
        // 快捷栏 (8, 143)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 143));
        }

        // 数据同步
        int dataCount = processSlots * 2 + 4;
        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(dataCount) {
            @Override public int get(int index) {
                if (index < processSlots) return blockEntity.getProgress(index);
                if (index < processSlots * 2) return blockEntity.getMaxProgress(index - processSlots);
                return switch (index - processSlots * 2) {
                    case 0 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 2 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index < processSlots) {
                    progress[index] = value;
                } else if (index < processSlots * 2) {
                    maxProgress[index - processSlots] = value;
                } else {
                    switch (index - processSlots * 2) {
                        case 0 -> energy = (energy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                        case 1 -> energy = (energy & 0xFFFFFFFFL) | ((long) value << 32);
                        case 2 -> maxEnergy = (maxEnergy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                        case 3 -> maxEnergy = (maxEnergy & 0xFFFFFFFFL) | ((long) value << 32);
                    }
                }
            }
            @Override public int getCount() { return dataCount; }
        });
    }

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
            } else {
                if (slotStack.getItem() instanceof UpgradeItem) {
                    if (!this.moveItemStackTo(slotStack, processSlots * 2, machineSlots, false)) {
                        if (!this.moveItemStackTo(slotStack, 0, processSlots, false)) return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, 0, processSlots, false)) return ItemStack.EMPTY;
                }
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

    public ExtractorBlockEntity getBlockEntity() { return blockEntity; }
    public int getProgress(int slot) { return progress[slot]; }
    public int getMaxProgress(int slot) { return maxProgress[slot]; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public int getProcessSlots() { return processSlots; }
}
