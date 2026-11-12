package machine.storage;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** MFSU：EV，容量 40000000 EU。 */
public class MfsuBlockEntity extends StorageBlockEntity {
    public MfsuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MFSU.get(), pos, state, EUVoltage.EV, 40000000, "MFSU", "MFSU");
    }
}
