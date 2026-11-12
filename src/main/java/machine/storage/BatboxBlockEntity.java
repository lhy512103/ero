package machine.storage;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 储能箱：LV，容量 40000 EU。 */
public class BatboxBlockEntity extends StorageBlockEntity {
    public BatboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATBOX.get(), pos, state, EUVoltage.LV, 40000, "储能箱", "储能箱");
    }
}
