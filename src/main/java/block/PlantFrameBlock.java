package block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlantFrameBlock extends RotatedPillarBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    // H section: flanges facing north and south of the y model, rotated with the axis.
    private static final VoxelShape Y = net.minecraft.world.phys.shapes.Shapes.or(box(0, 0, 0, 16, 16, 3), box(0, 0, 13, 16, 16, 16), box(6.5, 0, 3, 9.5, 16, 13));
    private static final VoxelShape Z = net.minecraft.world.phys.shapes.Shapes.or(box(0, 0, 0, 16, 3, 16), box(0, 13, 0, 16, 16, 16), box(6.5, 3, 0, 9.5, 13, 16));
    private static final VoxelShape X = net.minecraft.world.phys.shapes.Shapes.or(box(0, 0, 0, 16, 3, 16), box(0, 13, 0, 16, 16, 16), box(0, 3, 6.5, 16, 13, 9.5));

    public PlantFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }
}
