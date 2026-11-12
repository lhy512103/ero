package blockentity;

import block.ModBlocks;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Energy hatch: where a large machine takes its power from the cables. Its voltage tier decides
 * how many operations the machine runs side by side: MV 16, HV 64. Swapping the hatch for a
 * higher tier is how an existing machine keeps up with a later stage.
 */
public class EnergyHatchBlockEntity extends MachineBlockEntity implements EnergyDisplay {
    private final EUEnergyStorageImpl storage;
    private final int parallel;

    public EnergyHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_HATCH.get(), pos, state);
        boolean hv = state.is(ModBlocks.ENERGY_HATCH_HV.get());
        this.storage = new EUEnergyStorageImpl(hv ? 80_000 : 20_000, hv ? EUVoltage.HV : EUVoltage.MV);
        this.parallel = hv ? 64 : 16;
    }

    /** Operations a machine powered through this hatch runs in parallel. */
    public int parallel() { return parallel; }

    @Override public String getMachineName() { return "energy_hatch"; }
    @Override public EUEnergyStorage getEnergyStorage() { return storage; }
    public EUEnergyStorageImpl storage() { return storage; }
    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", storage.getEnergyStored());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(storage::setEnergy);
    }
}
