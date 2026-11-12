package machine.storage;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** CESU：MV，容量 300000 EU。 */
public class CesuBlockEntity extends StorageBlockEntity {
    public CesuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CESU.get(), pos, state, EUVoltage.MV, 300000, "CESU", "CESU");
    }
}
