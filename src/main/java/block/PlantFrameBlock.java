package block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlantFrameBlock extends RotatedPillarBlock {
    private static final VoxelShape X = box(0, 2, 2, 16, 14, 14);
    private static final VoxelShape Y = box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape Z = box(2, 2, 0, 14, 14, 16);

    public PlantFrameBlock(Properties properties) { super(properties); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }
}
