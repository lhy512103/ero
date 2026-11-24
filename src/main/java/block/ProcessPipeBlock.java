package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flanged process pipe or cylindrical column: runs along its placed axis by default, and when
 * adjacent process pipes / fluid or gas hatches touch it on any side, extends flanged branch arms
 * and an elbow/tee collar hub so bends and junctions hinge smoothly instead of leaving gaps.
 */
public class ProcessPipeBlock extends PlantFrameBlock {
    private static final VoxelShape CORE = box(3.5, 3.5, 3.5, 12.5, 12.5, 12.5);
    private static final VoxelShape[] ARMS = {
        box(3.5, 0, 3.5, 12.5, 3.5, 12.5),
        box(3.5, 12.5, 3.5, 12.5, 16, 12.5),
        box(3.5, 3.5, 0, 12.5, 12.5, 3.5),
        box(3.5, 3.5, 12.5, 12.5, 12.5, 16),
        box(0, 3.5, 3.5, 3.5, 12.5, 12.5),
        box(12.5, 3.5, 3.5, 16, 12.5, 12.5)
    };
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = CORE;
            for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0) s = Shapes.or(s, ARMS[i]);
            SHAPES[mask] = s;
        }
    }

    private final boolean joinHatches;

    public ProcessPipeBlock(Properties properties) {
        this(properties, true);
    }

    public ProcessPipeBlock(Properties properties, boolean joinHatches) {
        super(properties);
        this.joinHatches = joinHatches;
    }

    @Override
    protected boolean connectsTo(BlockState self, Direction dir, BlockState neighbour) {
        if (neighbour.is(this)) return true;
        if (!joinHatches) return false;
        return neighbour.is(ModBlocks.FLUID_INPUT_HATCH.get())
            || neighbour.is(ModBlocks.FLUID_OUTPUT_HATCH.get())
            || neighbour.is(ModBlocks.GAS_OUTPUT_HATCH.get());
    }

    private static int axisMask(Direction.Axis axis) {
        return switch (axis) {
            case Y -> (1 << Direction.DOWN.get3DDataValue()) | (1 << Direction.UP.get3DDataValue());
            case Z -> (1 << Direction.NORTH.get3DDataValue()) | (1 << Direction.SOUTH.get3DDataValue());
            case X -> (1 << Direction.WEST.get3DDataValue()) | (1 << Direction.EAST.get3DDataValue());
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir))) mask |= 1 << dir.get3DDataValue();
        }
        if (mask == 0) mask = axisMask(state.getValue(AXIS));
        else if (!joinHatches) mask |= axisMask(state.getValue(AXIS));
        return SHAPES[mask];
    }
}
