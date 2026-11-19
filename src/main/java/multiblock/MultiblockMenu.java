package multiblock;

import blockentity.EnergyHatchBlockEntity;
import blockentity.HatchBlockEntity;
import blockentity.PlantControllerBlockEntity;
import init.ModMenus;
import item.MoldSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class MultiblockMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, CONTROLLER_HEIGHT = 190, HATCH_HEIGHT = 172, INVENTORY_X = 8;
    public static final int HATCH_X = 63, HATCH_Y = 21, MOLD_X = 152, MOLD_Y = 21;
    public enum Kind { PLANT, MILL, INPUT, OUTPUT, ENERGY }
    public enum Status { CHECKING, INCOMPLETE, NO_MOLD, IDLE, BLOCKED, LOW_POWER, RUNNING }

    private final Kind kind;
    private final BlockEntity target;
    private final int machineSlots;
    private final DataSlot status = DataSlot.standalone();
    private final DataSlot progress = DataSlot.standalone();
    private final DataSlot parallel = DataSlot.standalone();
    private final DataSlot length = DataSlot.standalone();
    // Data slots travel as 16-bit values, so EU readings are split into two halves.
    private final DataSlot energyLow = DataSlot.standalone(), energyHigh = DataSlot.standalone();
    private final DataSlot capacityLow = DataSlot.standalone(), capacityHigh = DataSlot.standalone();

    public static void open(Player player, BlockEntity target) {
        if (player instanceof ServerPlayer server) {
            Kind kind = kindOf(target);
            server.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new MultiblockMenu(id, inventory, target),
                target.getBlockState().getBlock().getName()), buffer -> buffer.writeEnum(kind));
        }
    }

    private static Kind kindOf(BlockEntity target) {
        return switch (target) {
            case PlantControllerBlockEntity plant -> plant.isMill() ? Kind.MILL : Kind.PLANT;
            case HatchBlockEntity hatch -> hatch.isOutput() ? Kind.OUTPUT : Kind.INPUT;
            case EnergyHatchBlockEntity ignored -> Kind.ENERGY;
            default -> throw new IllegalArgumentException("Not a multiblock terminal: " + target);
        };
    }

    public MultiblockMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(Kind.class), null);
    }

    public MultiblockMenu(int id, Inventory inventory, BlockEntity target) {
        this(id, inventory, kindOf(target), target);
    }

    private MultiblockMenu(int id, Inventory playerInventory, Kind kind, BlockEntity target) {
        super(ModMenus.MULTIBLOCK.get(), id);
        this.kind = kind;
        this.target = target;
        if (kind == Kind.INPUT || kind == Kind.OUTPUT) {
            var items = target instanceof HatchBlockEntity hatch ? hatch.getInventory() : new ItemStacksResourceHandler(9);
            for (int index = 0; index < items.size(); index++) {
                addSlot(new ResourceHandlerSlot(items, items::set, index, HATCH_X + index % 3 * 18, HATCH_Y + index / 3 * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return kind == Kind.INPUT && super.mayPlace(stack); }
                });
            }
        } else if (kind == Kind.MILL) {
            var mold = target instanceof PlantControllerBlockEntity plant ? plant.moldSlot() : new ItemStacksResourceHandler(1);
            addSlot(new MoldSlot(mold, mold::set, 0, MOLD_X, MOLD_Y));
        }
        machineSlots = slots.size();
        addStandardInventorySlots(playerInventory, INVENTORY_X, inventoryY(kind));
        addDataSlot(status);
        addDataSlot(progress);
        addDataSlot(parallel);
        addDataSlot(length);
        addDataSlot(energyLow);
        addDataSlot(energyHigh);
        addDataSlot(capacityLow);
        addDataSlot(capacityHigh);
        updateReadings();
    }

    public static boolean isController(Kind kind) { return kind == Kind.PLANT || kind == Kind.MILL; }
    public static int height(Kind kind) { return isController(kind) ? CONTROLLER_HEIGHT : HATCH_HEIGHT; }
    public static int inventoryY(Kind kind) { return height(kind) - 82; }

    private void updateReadings() {
        if (target instanceof PlantControllerBlockEntity plant) {
            Status current;
            if (plant.isScanning()) current = Status.CHECKING;
            else if (!plant.isFormed()) current = Status.INCOMPLETE;
            else if (plant.isWorking()) current = plant.isBlocked() ? Status.BLOCKED : plant.isStarved() ? Status.LOW_POWER : Status.RUNNING;
            else current = plant.isMill() && plant.getMold().isEmpty() ? Status.NO_MOLD : Status.IDLE;
            status.set(current.ordinal());
            progress.set(plant.getProgress());
            parallel.set(plant.isFormed() ? plant.getParallel() : 0);
            length.set(PlantControllerBlockEntity.targetLength(plant.getBlockState()));
            long[] stored = plant.hatchEnergy();
            setEnergy(stored[0], stored[1]);
        } else if (target instanceof EnergyHatchBlockEntity hatch) {
            parallel.set(hatch.parallel());
            setEnergy(hatch.getEnergyStorage().getEnergyStored(), hatch.getEnergyStorage().getMaxEnergyStored());
        }
    }

    private void setEnergy(long stored, long max) {
        int value = (int) Math.min(stored, Integer.MAX_VALUE), limit = (int) Math.min(max, Integer.MAX_VALUE);
        energyLow.set(value & 0xFFFF);
        energyHigh.set(value >>> 16);
        capacityLow.set(limit & 0xFFFF);
        capacityHigh.set(limit >>> 16);
    }

    @Override
    public void broadcastChanges() {
        updateReadings();
        super.broadcastChanges();
    }

    public Kind kind() { return kind; }
    public Status status() { return Status.values()[status.get()]; }
    public int progress() { return progress.get(); }
    public int parallel() { return parallel.get(); }
    public int length() { return length.get(); }
    public int energy() { return energyHigh.get() << 16 | energyLow.get() & 0xFFFF; }
    public int capacity() { return capacityHigh.get() << 16 | capacityLow.get() & 0xFFFF; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(moving, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (kind == Kind.INPUT || kind == Kind.MILL) {
                if (!moveItemStackTo(moving, 0, machineSlots, false)) return ItemStack.EMPTY;
            } else {
                int hotbar = slots.size() - 9;
                if (!(index < hotbar ? moveItemStackTo(moving, hotbar, slots.size(), false)
                    : moveItemStackTo(moving, machineSlots, hotbar, false))) return ItemStack.EMPTY;
            }
        }
        slot.setByPlayer(moving.isEmpty() ? ItemStack.EMPTY : moving);
        slot.onTake(player, moving);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (target == null) return true;
        var level = target.getLevel();
        var pos = target.getBlockPos();
        return level == player.level() && !target.isRemoved() && level.hasChunkAt(pos) && level.getBlockEntity(pos) == target
            && stillValid(ContainerLevelAccess.create(level, pos), player, target.getBlockState().getBlock());
    }
}
