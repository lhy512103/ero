package machine.workbench.menu;

import init.ModMenus;
import item.UpgradeItem;
import item.UpgradeSlot;
import machine.workbench.IndustrialWorkbenchBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 工业工作台 Menu：9 合成输入、1 输出、4 升级、1 供电、玩家物品栏、快捷栏
 */
public class IndustrialWorkbenchMenu extends AbstractContainerMenu {

    private final IndustrialWorkbenchBlockEntity blockEntity;

    // 同步数据
    private long energy = 0;
    private long maxEnergy = 0;
    private int progress = 0;
    private int maxProgress = 0;

    public IndustrialWorkbenchMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (IndustrialWorkbenchBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public IndustrialWorkbenchMenu(int containerId, Inventory playerInv, IndustrialWorkbenchBlockEntity blockEntity) {
        super(ModMenus.INDUSTRIAL_WORKBENCH.get(), containerId);
        this.blockEntity = blockEntity;

        // 合成输入槽 3x3 (29, 13) 间距19像素
        int[][] craftingPositions = {
            {29, 13}, {48, 13}, {67, 13},
            {29, 32}, {48, 32}, {67, 32},
            {29, 51}, {48, 51}, {67, 51}
        };
        for (int i = 0; i < 9; i++) {
            addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
                blockEntity.getItemHandler()::set,
                blockEntity.getCraftingStart() + i,
                craftingPositions[i][0], craftingPositions[i][1]));
        }

        // 输出槽 (124, 32)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set,
            blockEntity.getOutputSlot(), 124, 32));

        // 升级槽 4 格从上往下 (152, 5) (152, 23) (152, 42) (152, 61)
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, blockEntity.getUpgradeSlotStart(), 152, 5));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, blockEntity.getUpgradeSlotStart() + 1, 152, 23));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, blockEntity.getUpgradeSlotStart() + 2, 152, 42));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, blockEntity.getUpgradeSlotStart() + 3, 152, 61));

        // 供电槽 (8, 59)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set,
            blockEntity.getBatterySlot(), 8, 59));

        // 额外物品栏1 9格 (8, 79)
        for (int col = 0; col < 9; col++) {
            addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
                blockEntity.getItemHandler()::set,
                blockEntity.getExtraInventoryStart() + col,
                8 + col * 18, 79));
        }
        // 额外物品栏2 9格 (8, 99)
        for (int col = 0; col < 9; col++) {
            addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
                blockEntity.getItemHandler()::set,
                blockEntity.getExtraInventoryStart() + 9 + col,
                8 + col * 18, 99));
        }

        // 玩家物品栏 (8, 119)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 119 + row * 18));
            }
        }
        // 快捷栏 (8, 175)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 175));
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
            int machineSlots = 33; // 9 crafting + 1 output + 4 upgrade + 1 battery + 18 extra inventory
            if (index < machineSlots) {
                // 从机器槽位移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
                // 从玩家物品栏移动到机器槽位
                if (slotStack.getItem() instanceof UpgradeItem) {
                    // 尝试放入升级槽
                    if (!this.moveItemStackTo(slotStack, 10, 14, false)) {
                        // 升级槽满了，尝试放入额外物品栏
                        if (!this.moveItemStackTo(slotStack, 15, 33, false)) return ItemStack.EMPTY;
                    }
                } else {
                    // 普通物品优先放入额外物品栏，满了放入合成槽
                    if (!this.moveItemStackTo(slotStack, 15, 33, false)) {
                        if (!this.moveItemStackTo(slotStack, 0, 9, false)) return ItemStack.EMPTY;
                    }
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

    public IndustrialWorkbenchBlockEntity getBlockEntity() { return blockEntity; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
}
