package machine.process;

import fluid.MachineTank;
import item.UpgradeItem;
import item.UpgradeSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.List;

/**
 * The menu of every process machine. Slots follow {@link ProcessLayout}; energy, progress, the idle
 * reason, the current draw and every tank's contents are synced as container data.
 * <p>
 * The screen shows one {@link Page} at a time. The machine's working slots only take part on the
 * process page and the upgrade slots only on the upgrade page; the server ignores pages entirely.
 * </p>
 */
public class ProcessMachineMenu extends AbstractContainerMenu {
    /** energy low/high, capacity low/high, progress, max progress, reason, short utility, have, need, mold, EU/t. */
    private static final int FIXED_DATA = 12;
    private static final int SHORT_UTILITY = 7, HAVE_SHARES = 8, NEED_SHARES = 9;

    private final ProcessMachineBlockEntity blockEntity;
    private final ProcessMachineDef def;
    private final ProcessLayout layout;
    private final int machineSlots;
    private final int[] data;

    /** The side tabs of the screen, in the order they are drawn. */
    public enum Page { PROCESS, MOLD, UPGRADES, RECIPES, ENERGY, INFO }

    private Page page = Page.PROCESS;

    public ProcessMachineMenu(int containerId, Inventory playerInv, ProcessMachineBlockEntity blockEntity) {
        super(ProcessMachines.MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.def = blockEntity.def();
        this.layout = new ProcessLayout(def);
        var handler = blockEntity.getItemHandler();

        for (int i = 0; i < def.itemsIn(); i++) {
            var pos = layout.inputs.get(i);
            addSlot(processSlot(handler, i, pos));
        }
        for (int i = 0; i < def.itemsOut(); i++) {
            var pos = layout.outputs.get(i);
            addSlot(processSlot(handler, def.outputStart() + i, pos));
        }
        if (layout.catalyst != null) addSlot(processSlot(handler, def.catalystIndex(), layout.catalyst));
        if (layout.card != null) addSlot(processSlot(handler, def.cardIndex(), layout.card));
        for (int i = 0; i < def.upgrades(); i++) {
            var pos = layout.upgrades.get(i);
            addSlot(new UpgradeSlot(handler, handler::set, def.upgradeStart() + i, pos.x(), pos.y()) {
                @Override public boolean isActive() { return page == Page.UPGRADES; }
            });
        }
        this.machineSlots = slots.size();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, layout.inventoryX + col * 18, ProcessLayout.INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, layout.inventoryX + col * 18, ProcessLayout.HOTBAR_Y));

        this.data = new int[FIXED_DATA + def.tankCount() * 2];
        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                var energy = blockEntity.getEnergyStorage();
                return switch (index) {
                    case 0 -> (int) (energy.getEnergyStored() & 0xFFFFFFFFL);
                    case 1 -> (int) (energy.getEnergyStored() >>> 32);
                    case 2 -> (int) (energy.getMaxEnergyStored() & 0xFFFFFFFFL);
                    case 3 -> (int) (energy.getMaxEnergyStored() >>> 32);
                    case 4 -> blockEntity.getProgress();
                    case 5 -> blockEntity.getMaxProgress();
                    case 6 -> Math.max(0, ProcessMachineBlockEntity.REASONS.indexOf(blockEntity.isWorking() ? "" : blockEntity.reason()));
                    case SHORT_UTILITY -> {
                        machine.utility.Utility short_ = blockEntity.shortUtility();
                        yield short_ == null ? 0 : short_.ordinal() + 1;
                    }
                    case 10 -> blockEntity.mold();
                    case 11 -> (int) Math.min(Integer.MAX_VALUE, blockEntity.currentEnergyPerTick());
                    case HAVE_SHARES -> blockEntity.freeShares();
                    case NEED_SHARES -> blockEntity.neededShares();
                    default -> {
                        MachineTank tank = blockEntity.getTanks().get((index - FIXED_DATA) / 2);
                        if ((index - FIXED_DATA) % 2 == 1) yield tank.getAmount();
                        yield tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getResource().getFluid());
                    }
                };
            }

            @Override
            public void set(int index, int value) {
                data[index] = value;
            }

            @Override
            public int getCount() {
                return data.length;
            }
        });
    }

    private ResourceHandlerSlot processSlot(ItemStacksResourceHandler handler, int index, ProcessLayout.Pos pos) {
        return new ResourceHandlerSlot(handler, handler::set, index, pos.x(), pos.y()) {
            @Override public boolean isActive() { return page == Page.PROCESS; }
        };
    }

    public ProcessMachineBlockEntity blockEntity() { return blockEntity; }
    public ProcessMachineDef def() { return def; }
    public Page page() { return page; }
    /** Client only: which page the screen shows, so the slots of the other pages stay out of the way. */
    public void setPage(Page page) { this.page = page; }
    public int mold() { return data[10]; }
    /** EU/t of the recipe running now, overclocking included; 0 while idle. */
    public int energyPerTick() { return data[11]; }

    /** Button {@code n} selects mould or die {@code n} of {@link ProcessMachineBlockEntity#moldItems}. */
    @Override public boolean clickMenuButton(Player player, int action) {
        if (!stillValid(player) || action < 0 || action >= ProcessMachineBlockEntity.moldItems(def.id()).size()) return false;
        blockEntity.setMold(action);
        return true;
    }
    public ProcessLayout layout() { return layout; }

    public long energy() { return (data[0] & 0xFFFFFFFFL) | ((long) data[1] << 32); }
    public long maxEnergy() { return (data[2] & 0xFFFFFFFFL) | ((long) data[3] << 32); }
    public int progress() { return data[4]; }
    public int maxProgress() { return data[5]; }

    /** Why the machine is idle (an entry of {@link ProcessMachineBlockEntity#REASONS}); empty while running. */
    public String reason() {
        List<String> reasons = ProcessMachineBlockEntity.REASONS;
        int index = data[6];
        return index >= 0 && index < reasons.size() ? reasons.get(index) : "";
    }

    /** The utility the machine is short of (read from the synced data), or null. */
    public machine.utility.Utility shortUtility() {
        int index = data[SHORT_UTILITY] - 1;
        return index >= 0 && index < machine.utility.Utility.values().length ? machine.utility.Utility.values()[index] : null;
    }

    /** Shares of {@link #shortUtility()} still free at the suppliers, and how many the machine needs. */
    public int haveShares() { return data[HAVE_SHARES]; }
    public int needShares() { return data[NEED_SHARES]; }

    public Fluid tankFluid(int tank) {
        int id = data[FIXED_DATA + tank * 2];
        return id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
    }

    public int tankAmount(int tank) { return data[FIXED_DATA + tank * 2 + 1]; }

    public int tankCapacity(int tank) {
        int inputs = def.fluidsIn().size();
        return tank < inputs ? def.fluidsIn().get(tank) : def.fluidsOut().get(tank - inputs);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            int upgradeFirst = machineSlots - def.upgrades();
            if (stack.getItem() instanceof UpgradeItem && def.upgrades() > 0) moved = moveItemStackTo(stack, upgradeFirst, machineSlots, false);
            // the catalyst slot sits right after the outputs
            if (!moved && def.catalystSlot() && blockEntity.isCatalyst(stack)) {
                int catalyst = def.itemsIn() + def.itemsOut();
                moved = moveItemStackTo(stack, catalyst, catalyst + 1, false);
            }
            if (!moved && def.itemsIn() > 0) moved = moveItemStackTo(stack, 0, def.itemsIn(), false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64;
    }
}
