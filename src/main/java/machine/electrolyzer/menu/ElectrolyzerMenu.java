package machine.electrolyzer.menu;

import init.ModMenus;
import item.FluidSlot;
import item.UpgradeItem;
import item.UpgradeSlot;
import machine.electrolyzer.ElectrolyzerBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 电解机 Menu：1 流体输入槽、3 流体输出槽、4 升级槽
 */
public class ElectrolyzerMenu extends AbstractContainerMenu {

    private final ElectrolyzerBlockEntity blockEntity;

    // 同步数据
    private long energy = 0;
    private long maxEnergy = 0;
    private int progress = 0;
    private int maxProgress = 0;
    private int fluid = 0;
    private int maxFluid = 0;

    public ElectrolyzerMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (ElectrolyzerBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public ElectrolyzerMenu(int containerId, Inventory playerInv, ElectrolyzerBlockEntity blockEntity) {
        super(ModMenus.ELECTROLYZER.get(), containerId);
        this.blockEntity = blockEntity;

        int fluidInputSlot = 0;
        int fluidOutput1Slot = 1;
        int fluidOutput2Slot = 2;
        int fluidOutput3Slot = 3;
        int upgradeStart = 4;

        // 流体输入槽 (79, 21) — 使用 Menu 同步的 fluid 数据判断
        addSlot(new FluidSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, fluidInputSlot, 79, 21,
            () -> fluid > 0 ? new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET) : ItemStack.EMPTY));

        // 流体输出槽 1 (47, 56)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, fluidOutput1Slot, 47, 56));
        // 流体输出槽 2 (79, 56)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, fluidOutput2Slot, 79, 56));
        // 流体输出槽 3 (111, 56)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, fluidOutput3Slot, 111, 56));

        // 升级插件槽 1-4: (152, 8) (152, 27) (152, 47) (152, 66)
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart, 152, 8));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 1, 152, 27));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 2, 152, 47));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 3, 152, 66));

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

        // 数据同步：8 (energy low/high, maxEnergy low/high, progress, maxProgress, fluid, maxFluid)
        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(8) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 2 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    case 4 -> blockEntity.getProgress();
                    case 5 -> blockEntity.getMaxProgress();
                    case 6 -> blockEntity.getStoredFluid();
                    case 7 -> blockEntity.getMaxStoredFluid();
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
                    case 6 -> fluid = value;
                    case 7 -> maxFluid = value;
                }
            }
            @Override public int getCount() { return 8; }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            int machineSlots = 8; // 1 input + 3 output + 4 upgrades
            if (index < machineSlots) {
                // 从机器槽位移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
                // 从玩家物品栏移动到机器槽位
                if (slotStack.getItem() instanceof UpgradeItem) {
                    // 尝试放入升级槽
                    if (!this.moveItemStackTo(slotStack, 4, machineSlots, false)) {
                        // 升级槽满了，尝试放入输入槽
                        if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                    }
                } else {
                    // 普通物品放入输入槽
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
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

    public ElectrolyzerBlockEntity getBlockEntity() { return blockEntity; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getFluid() { return fluid; }
    public int getMaxFluid() { return maxFluid; }
}
