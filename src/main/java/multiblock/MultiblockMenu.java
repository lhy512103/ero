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
    /** Where the target sits; on the client the panel reads the controller's layout from there. */
    private net.minecraft.core.BlockPos pos;
    private final DataSlot status = DataSlot.standalone();
    private final DataSlot progress = DataSlot.standalone();
    private final DataSlot parallel = DataSlot.standalone();
    private final DataSlot length = DataSlot.standalone();
    // Data slots travel as 16-bit values, so EU readings are split into two halves.
    private final DataSlot energyLow = DataSlot.standalone(), energyHigh = DataSlot.standalone();
    private final DataSlot capacityLow = DataSlot.standalone(), capacityHigh = DataSlot.standalone();
    /** Kinds of structure block shown in the side panel; per kind: stored, fitted, wrong. */
    public static final int KINDS = 24;
    private final DataSlot[] stored = new DataSlot[KINDS], fitted = new DataSlot[KINDS], wrong = new DataSlot[KINDS];
    // panel buttons
    public static final int BUILD = 0, DISMANTLE = 1, SHORTER = 2, LONGER = 3;
    /** The store's slots, in panel coordinates on the materials page. */
    public static final int STORE_X = 8, STORE_Y = 22, STORE_COLS = 9;
    private final DataSlot kindCount = DataSlot.standalone();
    private int storeStart;
    /** Set by the client screen: the store's slots only show on the materials page. */
    public boolean storeShown;
    /** Set by the client screen: the mold slot only shows on the machine page. */
    public boolean machineShown = true;

    /** One slot of the material store; slots past the store's kinds stay hidden and empty. */
    public final class StoreSlot extends ResourceHandlerSlot {
        private final ItemStacksResourceHandler store;
        StoreSlot(ItemStacksResourceHandler store, int index, int x, int y) { super(store, store::set, index, x, y); this.store = store; }
        private boolean in() { return getSlotIndex() < store.size() && getSlotIndex() < kindCount(); }
        public int index() { return getSlotIndex(); }
        @Override public boolean isActive() { return in() && (target != null || storeShown); }
        @Override protected ItemStack getStackCopy() { return getSlotIndex() < store.size() ? super.getStackCopy() : ItemStack.EMPTY; }
        @Override protected void setStackCopy(ItemStack stack) { if (getSlotIndex() < store.size()) super.setStackCopy(stack); }
        @Override public boolean mayPlace(ItemStack stack) { return in() && super.mayPlace(stack); }
        @Override public boolean mayPickup(Player player) { return in() && super.mayPickup(player); }
        @Override public int getMaxStackSize() { return in() ? super.getMaxStackSize() : 0; }
        @Override public int getMaxStackSize(ItemStack stack) { return in() ? super.getMaxStackSize(stack) : 0; }
    }

    public int kindCount() { return kindCount.get(); }

    public static final int DEPOSIT = 100, WITHDRAW = 200, DEPOSIT_ALL = 300, WITHDRAW_ALL = 400;

    public static void open(Player player, BlockEntity target) {
        if (player instanceof ServerPlayer server) {
            Kind kind = kindOf(target);
            server.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new MultiblockMenu(id, inventory, target),
                target.getBlockState().getBlock().getName()), buffer -> { buffer.writeEnum(kind); buffer.writeBlockPos(target.getBlockPos()); });
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
        this.pos = buffer.readBlockPos();
    }

    public MultiblockMenu(int id, Inventory inventory, BlockEntity target) {
        this(id, inventory, kindOf(target), target);
    }

    private MultiblockMenu(int id, Inventory playerInventory, Kind kind, BlockEntity target) {
        super(ModMenus.MULTIBLOCK.get(), id);
        this.kind = kind;
        this.target = target;
        this.pos = target == null ? net.minecraft.core.BlockPos.ZERO : target.getBlockPos();
        if (kind == Kind.INPUT || kind == Kind.OUTPUT) {
            var items = target instanceof HatchBlockEntity hatch ? hatch.getInventory() : new ItemStacksResourceHandler(9);
            for (int index = 0; index < items.size(); index++) {
                addSlot(new ResourceHandlerSlot(items, items::set, index, HATCH_X + index % 3 * 18, HATCH_Y + index / 3 * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return kind == Kind.INPUT && super.mayPlace(stack); }
                });
            }
        } else if (kind == Kind.MILL) {
            var mold = target instanceof PlantControllerBlockEntity plant ? plant.moldSlot() : new ItemStacksResourceHandler(1);
            addSlot(new MoldSlot(mold, mold::set, 0, MOLD_X, MOLD_Y) {
                @Override public boolean isActive() { return target != null || machineShown; }
            });
        }
        storeStart = slots.size();
        if (isController(kind)) {
            ItemStacksResourceHandler store = target instanceof PlantControllerBlockEntity plant ? plant.materials()
                : new ItemStacksResourceHandler(KINDS) {
                    @Override protected int getCapacity(int index, net.neoforged.neoforge.transfer.item.ItemResource resource) { return Integer.MAX_VALUE; }
                };
            for (int i = 0; i < KINDS; i++) addSlot(new StoreSlot(store, i, STORE_X + i % STORE_COLS * 18, STORE_Y + i / STORE_COLS * 18));
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
        addDataSlot(kindCount);
        for (int i = 0; i < KINDS; i++) {
            addDataSlot(stored[i] = DataSlot.standalone());
            addDataSlot(fitted[i] = DataSlot.standalone());
            addDataSlot(wrong[i] = DataSlot.standalone());
        }
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
            long[] energy = plant.hatchEnergy();
            setEnergy(energy[0], energy[1]);
            var store = plant.materials();
            kindCount.set(Math.min(KINDS, store.size()));
            int i = 0;
            for (var entry : plant.progressByKind().entrySet()) {
                if (i >= KINDS) break;
                stored[i].set(store.count(entry.getKey()));
                fitted[i].set(entry.getValue()[1]);
                wrong[i].set(entry.getValue()[2]);
                i++;
            }
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

    public int stored(int kind) { return stored[kind].get() & 0xFFFF; }
    public int fitted(int kind) { return fitted[kind].get() & 0xFFFF; }
    public int wrong(int kind) { return wrong[kind].get() & 0xFFFF; }
    public net.minecraft.core.BlockPos pos() { return pos; }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(target instanceof PlantControllerBlockEntity plant) || !(player instanceof ServerPlayer server)) return false;
        switch (button) {
            case BUILD -> {
                int[] r = plant.build(player);
                server.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    r[1] > 0 ? "message.iuce.plant.built_missing" : "message.iuce.plant.built", r[0], r[1]));
            }
            case DISMANTLE -> {
                int[] r = plant.dismantle(player);
                server.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    r[1] + r[2] > 0 ? "message.iuce.plant.dismantled_overflow" : "message.iuce.plant.dismantled", r[0], r[1], r[2]));
            }
            case SHORTER -> plant.stepLength(-1);
            case LONGER -> plant.stepLength(1);
            default -> {
                int index = button % 100, action = button - index;
                var kinds = new java.util.ArrayList<>(plant.needs().keySet());
                if (index >= kinds.size()) return false;
                var item = kinds.get(index);
                int stack = item.getDefaultMaxStackSize();
                switch (action) {
                    case DEPOSIT -> plant.deposit(player, item, stack);
                    case WITHDRAW -> plant.withdraw(player, item, stack);
                    case DEPOSIT_ALL -> plant.deposit(player, item, Integer.MAX_VALUE);
                    case WITHDRAW_ALL -> plant.withdraw(player, item, Integer.MAX_VALUE);
                    default -> { return false; }
                }
            }
        }
        return true;
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
            if (isController(kind) && moveItemStackTo(moving, storeStart, machineSlots, false)) {
                // went into the material store
            } else if (kind == Kind.INPUT || kind == Kind.MILL) {
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
