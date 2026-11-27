package block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Checker-plate stairs; a multiblock part that hides once the machine forms. */
public class SteelStairsBlock extends StairBlock {
    public SteelStairsBlock(BlockState base, Properties properties) {
        super(base, properties);
        registerDefaultState(defaultBlockState().setValue(PlantFrameBlock.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PlantFrameBlock.FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return state.getValue(PlantFrameBlock.FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL; }
}
