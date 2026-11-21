package block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A flanged process pipe run along one axis; structural only, it carries nothing. */
public class ProcessPipeBlock extends PlantFrameBlock {
    private static final VoxelShape X = box(0, 3, 3, 16, 13, 13);
    private static final VoxelShape Y = box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape Z = box(3, 3, 0, 13, 13, 16);

    public ProcessPipeBlock(Properties properties) { super(properties); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }
}
