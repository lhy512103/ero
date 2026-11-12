package blockentity;

import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Energy converter: takes EU of any tier up to EV and pushes it as Forge Energy into every
 * neighbouring block that accepts it, at {@link #FE_PER_EU} FE per EU. This is how the mod's power
 * runs other mods' machines, for example an Applied Energistics energy acceptor.
 */
public class ConverterBlockEntity extends MachineBlockEntity implements EnergyDisplay {
    public static final int FE_PER_EU = 4;
    private final EUEnergyStorageImpl storage = new EUEnergyStorageImpl(32_000, EUVoltage.EV);

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVERTER.get(), pos, state);
    }

    @Override public String getMachineName() { return "converter"; }
    @Override public EUEnergyStorage getEnergyStorage() { return storage; }
    public EUEnergyStorageImpl storage() { return storage; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || storage.getEnergyStored() <= 0) return;
        for (Direction direction : Direction.values()) {
            long stored = storage.getEnergyStored();
            if (stored <= 0) return;
            var target = level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (target == null) continue;
            int offered = (int) Math.min(Integer.MAX_VALUE / 2, stored * FE_PER_EU);
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = target.insert(offered, transaction);
                if (accepted <= 0) continue;
                // a started EU counts as used: never hand out more than was paid for
                storage.removeEnergy((accepted + FE_PER_EU - 1) / FE_PER_EU);
                transaction.commit();
            }
        }
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
