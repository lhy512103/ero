package machine.storage.menu;

import init.ModMenus;
import item.ChargeSlot;
import item.EquipmentChargeSlot;
import machine.StorageGuiProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 储电箱 Menu：仅保留玩家物品栏与快捷栏
 */
public class StorageMenu extends AbstractContainerMenu {

    private final StorageGuiProvider blockEntity;

    // 同步数据：energy(low/high), maxEnergy(low/high)
    private long energy = 0;
    private long maxEnergy = 0;

    public StorageMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (StorageGuiProvider) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public StorageMenu(int containerId, Inventory playerInv, StorageGuiProvider blockEntity) {
        super(ModMenus.STORAGE.get(), containerId);
        this.blockEntity = blockEntity;

        // 充电槽1 (35, 41)
        addSlot(new ChargeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 4, 35, 41));
        // 充电槽2 (35, 19)
        addSlot(new ChargeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 5, 35, 19));
        // 装备充电槽 x4: (8, 65) (26, 65) (44, 65) (62, 65)
        addSlot(new EquipmentChargeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 6, 8, 65));
        addSlot(new EquipmentChargeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 7, 26, 65));
        addSlot(new EquipmentChargeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 8, 44, 65));
        addSlot(new EquipmentChargeSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 9, 62, 65));

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

        // 数据同步：4 (energy low/high, maxEnergy low/high)
        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(4) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 2 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> energy = (energy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 1 -> energy = (energy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 2 -> maxEnergy = (maxEnergy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 3 -> maxEnergy = (maxEnergy & 0xFFFFFFFFL) | ((long) value << 32);
                }
            }
            @Override public int getCount() { return 4; }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            int machineSlots = 6; // 2充电槽 + 4装备充电槽
            if (index < machineSlots) {
                // 从机器槽位移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
                // 从玩家物品栏移动到机器槽位
                if (!this.moveItemStackTo(slotStack, 0, machineSlots, false)) return ItemStack.EMPTY;
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

    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
}
