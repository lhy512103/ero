package machine.storage;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** MFE：HV，容量 4000000 EU。 */
public class MfeBlockEntity extends StorageBlockEntity {
    public MfeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MFE.get(), pos, state, EUVoltage.HV, 4000000, "MFE", "MFE");
    }
}
