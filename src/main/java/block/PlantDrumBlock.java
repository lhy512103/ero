package block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlantDrumBlock extends RotatedPillarBlock {
    private static final VoxelShape X = Shapes.or(box(0, 3, 1, 16, 13, 15), box(0, 1, 3, 16, 15, 13), box(0, 2, 2, 16, 14, 14));
    private static final VoxelShape Y = Shapes.or(box(1, 0, 3, 15, 16, 13), box(3, 0, 1, 13, 16, 15), box(2, 0, 2, 14, 16, 14));
    private static final VoxelShape Z = Shapes.or(box(3, 1, 0, 13, 15, 16), box(1, 3, 0, 15, 13, 16), box(2, 2, 0, 14, 14, 16));

    public PlantDrumBlock(Properties properties) { super(properties); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }
}
