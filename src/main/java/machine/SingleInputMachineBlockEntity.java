package machine;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import item.OverclockerUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import network.performance.MachinePriority;
import recipe.MachineRecipe;
import recipe.RecipeHelper;

import java.util.Arrays;

/**
 * Shared logic for machines that turn one input stack into one output stack using
 * {@code iuce:machine} recipes: macerator, compressor, extractor, metal former.
 * <p>
 * Slot layout: {@code processSlots} inputs, then {@code processSlots} outputs, then the upgrade slots.
 * A subclass only supplies its recipe type, its name and its menu. The save format
 * ({@code energy}, {@code progress}, {@code maxProgress}, items) is unchanged from the
 * per-machine classes this replaces, so existing worlds load as before.
 * </p>
 */
public abstract class SingleInputMachineBlockEntity extends MachineBlockEntity
        implements EnergyDisplay, ProcessingDisplay, MenuProvider {

    protected final EUEnergyStorageImpl energyStorage;
    protected final EUVoltage voltage;
    protected final int processSlots;
    protected final int upgradeSlots;
    protected final ItemStacksResourceHandler itemHandler;
    private final MachineInventory inventory;
    protected final int[] progress;
    protected final int[] maxProgress;

    private final MachineRecipe[] cachedRecipes;
    private final ItemResource[] cachedInputResources;
    @javax.annotation.Nullable private OverclockerUpgradeItem.Effect overclock;
    /** Energy already collected towards the current tick of work, per process slot. */
    private long[] paid;
    private long starvedAt = -1000;

    protected SingleInputMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, EUVoltage voltage,
                                            int processSlots, int upgradeSlots, long bufferCapacity, int baseProcessTime) {
        this(type, pos, state, voltage, processSlots, upgradeSlots, bufferCapacity, baseProcessTime, 0);
    }

    /** @param extraSlots machine-specific slots placed after the upgrade slots (hidden from automation) */
    protected SingleInputMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, EUVoltage voltage,
                                            int processSlots, int upgradeSlots, long bufferCapacity, int baseProcessTime,
                                            int extraSlots) {
        super(type, pos, state);
        this.voltage = voltage;
        this.processSlots = processSlots;
        this.upgradeSlots = upgradeSlots;
        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);
        this.inventory = new MachineInventory(processSlots * 2 + upgradeSlots + extraSlots);
        this.itemHandler = inventory;
        this.progress = new int[processSlots];
        this.maxProgress = new int[processSlots];
        this.cachedRecipes = new MachineRecipe[processSlots];
        this.cachedInputResources = new ItemResource[processSlots];
        Arrays.fill(maxProgress, baseProcessTime);
    }

    /** The {@code machine_type} of the recipes this machine runs right now. */
    protected abstract String recipeType();

    /** Called after the contents of an item slot changed. */
    protected void slotChanged(int index) { }

    private final class MachineInventory extends ItemStacksResourceHandler {
        private final int slots;

        MachineInventory(int slots) {
            super(slots);
            this.slots = slots;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (index >= 2 * processSlots) overclock = null;
            slotChanged(index);
            setChanged();
        }

        /** Saves written before a machine gained slots hold a shorter list; pad it back to full size. */
        void restoreSize() {
            if (size() >= slots) return;
            net.minecraft.core.NonNullList<ItemStack> stacks = net.minecraft.core.NonNullList.withSize(slots, ItemStack.EMPTY);
            for (int slot = 0; slot < size(); slot++) stacks.set(slot, getResource(slot).toStack(getAmountAsInt(slot)));
            setStacks(stacks);
        }
    }

    /** Drops all progress and cached recipes, e.g. after the recipe type changed. */
    protected void resetProcessing() {
        boolean wasWorking = isWorking();
        Arrays.fill(progress, 0);
        Arrays.fill(cachedRecipes, null);
        Arrays.fill(cachedInputResources, null);
        if (wasWorking) updateRunningState();
        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        processUpgrades();

        boolean wasWorking = isWorking();
        boolean anyWorking = false;

        for (int i = 0; i < processSlots; i++) {
            int outputSlot = processSlots + i;
            ItemResource inputResource = itemHandler.getResource(i);
            int inputAmount = itemHandler.getAmountAsInt(i);

            if (!inputResource.isEmpty() && inputAmount > 0) {
                if (cachedRecipes[i] == null || !inputResource.equals(cachedInputResources[i])) {
                    cachedRecipes[i] = RecipeHelper.findMachineRecipe(level, recipeType(), inputResource.toStack(1));
                    cachedInputResources[i] = inputResource;
                }

                MachineRecipe recipe = cachedRecipes[i];
                if (recipe == null || inputAmount < recipe.getInputCount()) {
                    progress[i] = 0;
                    continue;
                }

                if (overclock == null) overclock = OverclockerUpgradeItem.effect(this);
                maxProgress[i] = overclock.time(recipe.getProcessTime());
                long energyPerTick = overclock.energy(recipe.getEnergyPerTick());

                // 电不够付一整 tick 时先攒着，攒够了才前进一步：供电一半，速度一半
                if (paid == null) paid = new long[processSlots];
                paid[i] += energyStorage.removeEnergy(energyPerTick - paid[i]);
                if (paid[i] < energyPerTick) {
                    starvedAt = tickCount;
                    continue;
                }
                paid[i] = 0;

                progress[i]++;
                anyWorking = true;

                if (progress[i] >= maxProgress[i]) {
                    ItemStack output = recipe.getOutput();
                    try (Transaction tx = Transaction.openRoot()) {
                        int inserted = itemHandler.insert(outputSlot, ItemResource.of(output), output.getCount(), tx);
                        if (inserted > 0) {
                            itemHandler.extract(i, inputResource, recipe.getInputCount(), tx);
                            tx.commit();
                        }
                    }
                    progress[i] = 0;
                }
            } else if (inputResource.isEmpty() || inputAmount <= 0) {
                progress[i] = 0;
                cachedRecipes[i] = null;
                cachedInputResources[i] = ItemResource.EMPTY;
            }
        }

        if (wasWorking != anyWorking) {
            updateRunningState();
            setChanged();
        }
    }

    protected void updateRunningState() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(MachineBlock.RUNNING) && state.getValue(MachineBlock.RUNNING) != isWorking()) {
            level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, isWorking()));
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public int requestSlot(ItemResource resource) {
        return logistics.RecipeInputs.isInput(level, recipeType(), resource) ? slotFor(resource, range(0, processSlots)) : -1;
    }

    @Override
    public String getStatusKey() {
        return tickCount - starvedAt < 40 ? "tooltip.iuce.status.undervoltage" : "";
    }

    public boolean isWorking() {
        for (int value : progress) if (value > 0) return true;
        return false;
    }

    @Override public ItemStacksResourceHandler getItemHandler() { return itemHandler; }
    @Override public int getUpgradeSlotCount() { return upgradeSlots; }
    @Override public int getUpgradeSlotStart() { return 2 * processSlots; }
    @Override public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    @Override public MachinePriority getMachinePriority() { return MachinePriority.HIGH; }

    public EUVoltage getVoltage() { return voltage; }
    public int getProcessSlots() { return processSlots; }
    public int getProgress(int slot) { return progress[slot]; }
    public int getMaxProgress(int slot) { return maxProgress[slot]; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energyStorage::setEnergy);
        int[] savedProgress = input.getIntArray("progress").orElse(new int[processSlots]);
        int[] savedMaxProgress = input.getIntArray("maxProgress").orElse(new int[processSlots]);
        System.arraycopy(savedProgress, 0, progress, 0, Math.min(processSlots, savedProgress.length));
        System.arraycopy(savedMaxProgress, 0, maxProgress, 0, Math.min(processSlots, savedMaxProgress.length));
        itemHandler.deserialize(input);
        inventory.restoreSize();
        overclock = null;
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    public float getProcessingProgress() {
        float best = 0f;
        for (int i = 0; i < processSlots; i++) {
            if (progress[i] > 0 && maxProgress[i] > 0) best = Math.max(best, (float) progress[i] / maxProgress[i]);
        }
        return best;
    }

    @Override
    public boolean isProcessing() { return isWorking(); }
}
