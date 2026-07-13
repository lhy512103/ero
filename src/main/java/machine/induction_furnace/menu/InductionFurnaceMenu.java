package machine.induction_furnace.menu;

import init.ModMenus;
import item.UpgradeItem;
import item.UpgradeSlot;
import machine.induction_furnace.InductionFurnaceBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 合金炉 Menu：2 输入槽、1 输出槽、4 升级槽
 */
public class InductionFurnaceMenu extends AbstractContainerMenu {

    private final InductionFurnaceBlockEntity blockEntity;

    // 同步数据
    private long energy = 0;
    private long maxEnergy = 0;
    private int progress = 0;
    private int maxProgress = 0;

    public InductionFurnaceMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (InductionFurnaceBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public InductionFurnaceMenu(int containerId, Inventory playerInv, InductionFurnaceBlockEntity blockEntity) {
        super(ModMenus.INDUCTION_FURNACE.get(), containerId);
        this.blockEntity = blockEntity;

        int input1Slot = 0;
        int input2Slot = 1;
        int outputSlot = 2;
        int upgradeStart = 3;

        // 输入槽 1 (62, 17)、输入槽 2 (98, 17)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, input1Slot, 62, 17));
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, input2Slot, 98, 17));

        // 输出槽 (80, 66)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, outputSlot, 80, 66));

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

        // 数据同步：6 (energy low/high, maxEnergy low/high, progress, maxProgress)
        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(6) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 2 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    case 4 -> blockEntity.getProgress();
                    case 5 -> blockEntity.getMaxProgress();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> energy = (energy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 1 -> energy = (energy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 2 -> maxEnergy = (maxEnergy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 3 -> maxEnergy = (maxEnergy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 4 -> progress = value;
                    case 5 -> maxProgress = value;
                }
            }
            @Override public int getCount() { return 6; }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            int machineSlots = 7; // 2 input + 1 output + 4 upgrades
            if (index < machineSlots) {
                // 从机器槽位移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
                // 从玩家物品栏移动到机器槽位
                if (slotStack.getItem() instanceof UpgradeItem) {
                    // 尝试放入升级槽
                    if (!this.moveItemStackTo(slotStack, 3, machineSlots, false)) {
                        // 升级槽满了，尝试放入输入槽
                        if (!this.moveItemStackTo(slotStack, 0, 2, false)) return ItemStack.EMPTY;
                    }
                } else {
                    // 普通物品放入输入槽
                    if (!this.moveItemStackTo(slotStack, 0, 2, false)) return ItemStack.EMPTY;
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
    public boolean stillValid(Player player) {
        return true;
    }

    public InductionFurnaceBlockEntity getBlockEntity() { return blockEntity; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
}
