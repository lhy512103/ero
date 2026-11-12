package machine;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import network.performance.MachinePriority;

/**
 * Shared logic of every solar generator tier: generates {@code generationRate} EU/t while the block
 * above can see the sky and the overworld clock is in the daytime half. Tiers differ only in
 * voltage, rate, buffer and name.
 */
public abstract class SolarBlockEntity extends MachineBlockEntity implements EnergyDisplay {

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final long generationRate;
    private final String name;
    private boolean isGenerating = false;

    protected SolarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               EUVoltage voltage, long generationRate, long bufferCapacity, String name) {
        super(type, pos, state);
        this.voltage = voltage;
        this.generationRate = generationRate;
        this.name = name;
        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        boolean wasGenerating = isGenerating;
        // Sky and daylight are re-checked every tick while generating, otherwise once a second.
        isGenerating = (wasGenerating || tickCount % 20 == 0)
            && level.canSeeSky(worldPosition.above())
            && level.getOverworldClockTime() % 24000 < 12000
            && !energyStorage.isFull();

        if (isGenerating) energyStorage.addEnergy(generationRate);

        if (wasGenerating != isGenerating) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(MachineBlock.RUNNING, isGenerating));
            setChanged();
        }
    }

    @Override public String getMachineName() { return name; }
    @Override public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    @Override public MachinePriority getMachinePriority() { return MachinePriority.CRITICAL; }

    public EUVoltage getVoltage() { return voltage; }
    public long getGenerationRate() { return generationRate; }
    public boolean isWorking() { return isGenerating; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energyStorage::setEnergy);
    }

    @Override protected boolean canReceiveEU() { return false; }
    @Override protected boolean canExtractEU() { return true; }
}
