package block;
import blockentity.PlantTankEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public class PlantTankBlock extends Block implements EntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public PlantTankBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(FORMED, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(FORMED); }
    @Override public RenderShape getRenderShape(BlockState state) { return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override public PlantTankEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlantTankEntity(pos,state); }
}
