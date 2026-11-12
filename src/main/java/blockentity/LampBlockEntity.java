package blockentity;

import block.MachineBlock;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Electric lamp: full brightness for 1 EU per tick, dark without power. */
public class LampBlockEntity extends MachineBlockEntity implements EnergyDisplay {
    private static final int PERIOD = 20;
    private final EUEnergyStorageImpl storage = new EUEnergyStorageImpl(200, EUVoltage.LV);

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
    }

    @Override public String getMachineName() { return "lamp"; }
    @Override public EUEnergyStorage getEnergyStorage() { return storage; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || tickCount % PERIOD != 0) return;
        // pays a second in advance, so the light does not flicker between deliveries
        boolean lit = storage.getEnergyStored() >= PERIOD;
        if (lit) storage.removeEnergy(PERIOD);
        BlockState state = getBlockState();
        if (state.getValue(MachineBlock.RUNNING) != lit) level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, lit));
    }

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
