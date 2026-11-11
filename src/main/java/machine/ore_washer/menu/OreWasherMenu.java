package machine.ore_washer.menu;

import init.ModMenus;
import item.UpgradeItem;
import item.UpgradeSlot;
import machine.ore_washer.OreWasherBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 洗矿机 Menu：2 输入槽、1 输出槽、4 升级槽
 */
public class OreWasherMenu extends AbstractContainerMenu {

    private final OreWasherBlockEntity blockEntity;

    // 同步数据
    private long energy = 0;
    private long maxEnergy = 0;
    private int progress = 0;
    private int maxProgress = 0;
    private int water = 0;
    private int maxWater = 0;

    public OreWasherMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (OreWasherBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public OreWasherMenu(int containerId, Inventory playerInv, OreWasherBlockEntity blockEntity) {
        super(ModMenus.ORE_WASHER.get(), containerId);
        this.blockEntity = blockEntity;

        int input1Slot = 0;
        int input2Slot = 1;
        int outputSlot = 2;
        int extraOutputSlot = 3;
        int upgradeStart = 4;

        // 输入槽 1 (51, 54)、输入槽 2 (51, 23)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, input1Slot, 51, 54));
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, input2Slot, 51, 23));

        // 输出槽 (99, 39)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, outputSlot, 99, 39));

        // 副产槽 (99, 62)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, extraOutputSlot, 99, 62));

        // 升级插件槽 1-4: (152, 9) (152, 28) (152, 47) (152, 66)
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart, 152, 9));
        addSlot(new UpgradeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, upgradeStart + 1, 152, 28));
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

        // 数据同步：8 (energy low/high, maxEnergy low/high, progress, maxProgress, water, maxWater)
        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(8) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 2 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    case 4 -> blockEntity.getProgress();
                    case 5 -> blockEntity.getMaxProgress();
                    case 6 -> blockEntity.getStoredWater();
                    case 7 -> blockEntity.getMaxStoredWater();
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
                    case 6 -> water = value;
                    case 7 -> maxWater = value;
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
            int machineSlots = 8; // 2 input + 1 output + 1 extra output + 4 upgrades
            if (index < machineSlots) {
                // 从机器槽位移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
                // 从玩家物品栏移动到机器槽位
                if (slotStack.getItem() instanceof UpgradeItem) {
                    // 尝试放入升级槽
                    if (!this.moveItemStackTo(slotStack, 4, machineSlots, false)) {
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

    public OreWasherBlockEntity getBlockEntity() { return blockEntity; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getWater() { return water; }
    public int getMaxWater() { return maxWater; }

    /**
     * 获取当前流体对应的翻译键（用于 GUI 提示）
     */
    public String getFluidTranslationKey() {
        ItemStack fluidItem = blockEntity.getFluidItem();
        return fluidItem.isEmpty() ? "tooltip.iuce.empty_fluid" : fluidItem.getItem().getDescriptionId();
    }
}
