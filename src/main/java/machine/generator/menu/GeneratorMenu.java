package machine.generator.menu;

import init.ModMenus;
import machine.generator.GeneratorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 普通发电机 Menu：2 燃料槽、1 充电槽
 */
public class GeneratorMenu extends AbstractContainerMenu {

    private final GeneratorBlockEntity blockEntity;

    // 同步数据索引：0=burnTime, 1=totalBurnTime, 2=energy(low), 3=energy(high), 4=maxEnergy(low), 5=maxEnergy(high)
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private long energy = 0;
    private long maxEnergy = 0;

    public GeneratorMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf data) {
        this(containerId, playerInv, (GeneratorBlockEntity) playerInv.player.level().getBlockEntity(
            data.readBlockPos()));
    }

    public GeneratorMenu(int containerId, Inventory playerInv, GeneratorBlockEntity blockEntity) {
        super(ModMenus.GENERATOR.get(), containerId);
        this.blockEntity = blockEntity;

        // 燃料槽1 (46, 18)、燃料槽2 (46, 54)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 0, 46, 18));
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 1, 46, 54));
        // 充电槽 (103, 36)
        addSlot(new ResourceHandlerSlot(blockEntity.getItemHandler(),
            blockEntity.getItemHandler()::set, 2, 103, 36));

        // 玩家物品栏 (8, 84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 (8, 140)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 140));
        }

        addDataSlots(new net.minecraft.world.inventory.SimpleContainerData(6) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getBurnTime();
                    case 1 -> blockEntity.getTotalBurnTime();
                    case 2 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (blockEntity.getEnergyStorage().getEnergyStored() >>> 32);
                    case 4 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 5 -> (int) (blockEntity.getEnergyStorage().getMaxEnergyStored() >>> 32);
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> burnTime = value;
                    case 1 -> totalBurnTime = value;
                    case 2 -> energy = (energy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 3 -> energy = (energy & 0xFFFFFFFFL) | ((long) value << 32);
                    case 4 -> maxEnergy = (maxEnergy & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                    case 5 -> maxEnergy = (maxEnergy & 0xFFFFFFFFL) | ((long) value << 32);
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
            int machineSlots = 3;
            if (index < machineSlots) {
                if (!this.moveItemStackTo(slotStack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            } else {
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

    public GeneratorBlockEntity getBlockEntity() { return blockEntity; }
    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
    public long getEnergy() { return energy; }
    public long getMaxEnergy() { return maxEnergy; }
    public boolean isBurning() { return burnTime > 0; }
}
