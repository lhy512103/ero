package blockentity;

import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Energy hatch (MV): where a large machine takes its power from the cables. Its voltage tier
 * decides how many operations the machine runs side by side.
 */
public class EnergyHatchBlockEntity extends MachineBlockEntity implements EnergyDisplay {
    /** Operations a machine with an MV hatch runs in parallel. */
    public static final int PARALLEL = 16;
    private final EUEnergyStorageImpl storage = new EUEnergyStorageImpl(20_000, EUVoltage.MV);

    public EnergyHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_HATCH.get(), pos, state);
    }

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
