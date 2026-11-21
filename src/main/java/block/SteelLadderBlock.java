package block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Steel ladder that can attach to non-full structural blocks and hides once a multiblock forms. */
public class SteelLadderBlock extends LadderBlock {
    public SteelLadderBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PlantFrameBlock.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PlantFrameBlock.FORMED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return true; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return state.getValue(PlantFrameBlock.FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL; }
}
