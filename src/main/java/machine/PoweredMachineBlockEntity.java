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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * Plumbing shared by powered machines that run one job at a time: energy buffer, item slots,
 * a progress counter, the overclocker count, the RUNNING block state, and saving all of it.
 * Subclasses write only their recipe logic in {@link #tick()}.
 * <p>
 * Save format: {@code energy}, {@code progress}, {@code maxProgress}, items, plus whatever
 * {@link #saveExtra} adds. This matches the per-machine classes it replaces.
 * </p>
 */
public abstract class PoweredMachineBlockEntity extends MachineBlockEntity implements EnergyDisplay, ProcessingDisplay {

    protected final EUEnergyStorageImpl energyStorage;
    protected final EUVoltage voltage;
    protected final ItemStacksResourceHandler itemHandler;
    protected int progress = 0;
    protected int maxProgress;

    private final int upgradeStart;
    private final int upgradeCount;
    private final int baseProcessTime;
    @javax.annotation.Nullable private OverclockerUpgradeItem.Effect overclock;

    /**
     * @param slots        total item slots, upgrade slots included
     * @param upgradeStart index of the first upgrade slot; every slot from here on is an upgrade slot
     */
    protected PoweredMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, EUVoltage voltage,
                                        long bufferCapacity, int slots, int upgradeStart, int baseProcessTime) {
        super(type, pos, state);
        this.voltage = voltage;
        this.upgradeStart = upgradeStart;
        this.upgradeCount = slots - upgradeStart;
        this.baseProcessTime = baseProcessTime;
        this.maxProgress = baseProcessTime;
        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);
        this.itemHandler = new ItemStacksResourceHandler(slots) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                if (index >= PoweredMachineBlockEntity.this.upgradeStart) overclock = null;
                setChanged();
            }
        };
    }

    /** Combined effect of the installed overclockers, recomputed only after an upgrade slot changes. */
    protected OverclockerUpgradeItem.Effect overclock() {
        if (overclock == null) overclock = OverclockerUpgradeItem.effect(this);
        return overclock;
    }

    protected int overclockedTime(int ticks) {
        return overclock().time(ticks);
    }

    protected long overclockedEnergy(long energyPerTick) {
        return overclock().energy(energyPerTick);
    }

    /** Makes the block's RUNNING state follow {@link #isWorking()}. */
    protected void updateRunningState() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(MachineBlock.RUNNING) && state.getValue(MachineBlock.RUNNING) != isWorking()) {
            level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, isWorking()));
        }
    }

    /** Extra values to save besides energy, progress and items. */
    protected void saveExtra(ValueOutput output) { }

    /** Counterpart of {@link #saveExtra}; also the place to drop cached recipes. */
    protected void loadExtra(ValueInput input) { }

    public boolean isWorking() { return progress > 0; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public EUVoltage getVoltage() { return voltage; }

    @Override public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    @Override public ItemStacksResourceHandler getItemHandler() { return itemHandler; }
    @Override public int getUpgradeSlotStart() { return upgradeStart; }
    @Override public int getUpgradeSlotCount() { return upgradeCount; }
    @Override public boolean isProcessing() { return progress > 0; }

    @Override
    public float getProcessingProgress() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        saveExtra(output);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energyStorage::setEnergy);
        progress = input.getInt("progress").orElse(0);
        maxProgress = input.getInt("maxProgress").orElse(baseProcessTime);
        itemHandler.deserialize(input);
        overclock = null;
        loadExtra(input);
    }
}
