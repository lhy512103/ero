package block;
import blockentity.PlantTankEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
public class PlantTankBlock extends Block implements EntityBlock {
    public PlantTankBlock(Properties properties) { super(properties); }
    @Override public PlantTankEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlantTankEntity(pos,state); }
}
