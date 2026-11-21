package block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Pipe handrail that joins like a fence; a multiblock part that hides once the machine forms. */
public class SteelHandrailBlock extends FenceBlock {
    public SteelHandrailBlock(Properties properties) {
        super(properties);
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
