package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Industrial roller drum: merges seamlessly along its own axis when placed end-to-end, hiding
 * the internal bearing journal caps and collar seams between adjacent coaxial drums.
 */
public class PlantDrumBlock extends RotatedPillarBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty NEG = BooleanProperty.create("neg");
    public static final BooleanProperty POS = BooleanProperty.create("pos");
    private static final VoxelShape X = Shapes.or(box(0, 3, 1, 16, 13, 15), box(0, 1, 3, 16, 15, 13), box(0, 2, 2, 16, 14, 14));
    private static final VoxelShape Y = Shapes.or(box(1, 0, 3, 15, 16, 13), box(3, 0, 1, 13, 16, 15), box(2, 0, 2, 14, 16, 14));
    private static final VoxelShape Z = Shapes.or(box(3, 1, 0, 13, 15, 16), box(1, 3, 0, 15, 13, 16), box(2, 2, 0, 14, 14, 16));

    public PlantDrumBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(NEG, false).setValue(POS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED, NEG, POS);
    }

    private static Direction negDir(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.WEST;
            case Y -> Direction.DOWN;
            case Z -> Direction.NORTH;
        };
    }

    private static Direction posDir(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
    }

    private boolean coaxial(Direction.Axis axis, BlockState neighbour) {
        return neighbour.is(this) && neighbour.getValue(AXIS) == axis;
    }

    public BlockState joinedState(BlockState state, LevelReader level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        BlockPos np = pos.relative(negDir(axis));
        BlockPos pp = pos.relative(posDir(axis));
        return state
            .setValue(NEG, level.hasChunkAt(np) && coaxial(axis, level.getBlockState(np)))
            .setValue(POS, level.hasChunkAt(pp) && coaxial(axis, level.getBlockState(pp)));
    }

    public BlockState joinedInLayout(BlockState state, BlockPos pos, Map<BlockPos, BlockState> placed) {
        Direction.Axis axis = state.getValue(AXIS);
        BlockState nb = placed.get(pos.relative(negDir(axis)));
        BlockState pb = placed.get(pos.relative(posDir(axis)));
        return state
            .setValue(NEG, nb != null && coaxial(axis, nb))
            .setValue(POS, pb != null && coaxial(axis, pb));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        return base == null ? null : joinedState(base, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        if (direction == negDir(axis)) return state.setValue(NEG, coaxial(axis, neighbour));
        if (direction == posDir(axis)) return state.setValue(POS, coaxial(axis, neighbour));
        return state;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(FORMED) || super.propagatesSkylightDown(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? 1.0F : super.getShadeBrightness(state, level, pos);
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
