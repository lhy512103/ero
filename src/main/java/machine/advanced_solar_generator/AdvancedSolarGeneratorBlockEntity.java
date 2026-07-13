package machine.advanced_solar_generator;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import network.performance.MachinePriority;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AdvancedSolarGeneratorBlockEntity extends MachineBlockEntity implements EnergyDisplay {

    private final EUEnergyStorageImpl energyStorage;
    private final long generationRate;
    private final EUVoltage voltage;
    private boolean isGenerating = false;

    public AdvancedSolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_SOLAR_GENERATOR.get(), pos, state);
        this.voltage = EUVoltage.MV;
        this.generationRate = 8;
        this.energyStorage = new EUEnergyStorageImpl(1000, voltage);
    }
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        boolean wasGenerating = isGenerating;
        isGenerating = false;

        if (tickCount % 20 == 0 || wasGenerating != isGenerating) {
            if (level.canSeeSky(worldPosition.above()) && level.getOverworldClockTime() % 24000 < 12000) {
                if (!energyStorage.isFull()) {
                    isGenerating = true;
                }
            }
        }

        if (isGenerating && !energyStorage.isFull()) {
            energyStorage.addEnergy(generationRate);
        }

        if (wasGenerating != isGenerating) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(MachineBlock.RUNNING, isGenerating));
            setChanged();
        }
    }
    @Override
    public String getMachineName() {
        return "高级太阳能发电机";
    }

    public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    public EUVoltage getVoltage() { return voltage; }
    public long getGenerationRate() { return generationRate; }
    public boolean isWorking() { return isGenerating; }
    @Override
    public MachinePriority getMachinePriority() {
        return MachinePriority.CRITICAL;
    }
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
    }
    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(e -> energyStorage.setEnergy(e));
    }

    @Override protected boolean canReceiveEU() { return false; }
    @Override protected boolean canExtractEU() { return true; }
}
