package machine.planter.menu;

import init.ModMenus;
import item.UpgradeItem;
import item.UpgradeSlot;
import machine.planter.PlanterBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 种植机 Menu：1 培育槽、1 土壤槽、1 输出槽、4 升级槽
 */
public class PlanterMenu extends AbstractContainerMenu {

    private final PlanterBlockEntity blockEntity;

    // 同步数据
    private long energy = 0;
    private long maxEnergy = 0;
    private boolean isDaytime = false;
    private int progress = 0;
    private int maxProgress = 0;
    private int clockTime = 0;
    // private int timeModeOrdinal = 0; // 0=NORMAL, 1=ETERNAL_DAY, 2=ETERNAL_NIGHT

    public PlanterMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (PlanterBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public PlanterMenu(int containerId, Inventory playerInv, PlanterBlockEntity blockEntity) {
        super(ModMenus.PLANTER.get(), containerId);
        this.blockEntity = blockEntity;

        int cultivationSlot = 0;
        int soilSlot = 1;
        int outputSlot = 2;
        int upgradeStart = 3;

        // 培育槽 (62, 13)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, cultivationSlot, 62, 13));

        // 土壤槽 (62, 62)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, soilSlot, 62, 62));

        // 输出槽 (98, 38)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, outputSlot, 98, 38));

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

        // 数据同步：8 (energy low/high, maxEnergy low/high, isDaytime, progress, maxProgress, clockTime)
        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(8) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 2 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    case 4 -> blockEntity.isDaytime() ? 1 : 0;
                    case 5 -> blockEntity.getProgress();
                    case 6 -> blockEntity.getMaxProgress();
                    case 7 -> blockEntity.getClockTime();
                    // case 8 -> blockEntity.getTimeMode().ordinal();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> energy = (energy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 1 -> energy = (energy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 2 -> maxEnergy = (maxEnergy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 3 -> maxEnergy = (maxEnergy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 4 -> isDaytime = value == 1;
                    case 5 -> progress = value;
                    case 6 -> maxProgress = value;
                    case 7 -> clockTime = value;
                    // case 8 -> timeModeOrdinal = value;
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
            int machineSlots = 7; // 1 cultivation + 1 soil + 1 output + 4 upgrades
            if (index < machineSlots) {
                // 从机器槽位移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
                // 从玩家物品栏移动到机器槽位
                if (slotStack.getItem() instanceof UpgradeItem) {
                    // 尝试放入升级槽
                    if (!this.moveItemStackTo(slotStack, 3, machineSlots, false)) {
                        // 升级槽满了，尝试放入功能槽
                        if (!this.moveItemStackTo(slotStack, 0, 2, false)) return ItemStack.EMPTY;
                    }
                } else {
                    // 普通物品放入功能槽（培育槽、土壤槽）
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

    public PlanterBlockEntity getBlockEntity() { return blockEntity; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public boolean isDaytime() { return isDaytime; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getClockTime() { return clockTime; }

    /** 获取当前时间阶段名称 */
    public String getPhaseName() {
        if (clockTime < 2000) return "凌晨";
        if (clockTime < 6000) return "清晨";
        if (clockTime < 8000) return "正午";
        if (clockTime < 12000) return "下午";
        return "夜晚";
    }

    /** 获取当前速度倍率 */
    public float getSpeedMultiplier() {
        if (clockTime < 2000) return 1.5f;
        if (clockTime < 6000) return 2.0f;
        if (clockTime < 8000) return 3.0f;
        if (clockTime < 12000) return 2.0f;
        return 1.0f;
    }
}
