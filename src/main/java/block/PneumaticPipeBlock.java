package block;

import com.mojang.serialization.MapCodec;
import logistics.LogisticsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pneumatic pipe: joins logistics interfaces into one network. A pipe has no block entity and does
 * no work; {@link LogisticsManager} only needs to know which interfaces a run of pipes connects.
 */
public class PneumaticPipeBlock extends Block {
    public static final MapCodec<PneumaticPipeBlock> CODEC = BlockBehaviour.simpleCodec(PneumaticPipeBlock::new);
    /** Indexed by {@link Direction#get3DDataValue()}: down, up, north, south, west, east. */
    private static final BooleanProperty[] SIDES = {BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST};
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape[] arms = {Block.box(5, 0, 5, 11, 5, 11), Block.box(5, 11, 5, 11, 16, 11), Block.box(5, 5, 0, 11, 11, 5),
            Block.box(5, 5, 11, 11, 11, 16), Block.box(0, 5, 5, 5, 11, 11), Block.box(11, 5, 5, 16, 11, 11)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = Block.box(5, 5, 5, 11, 11, 11);
            for (int side = 0; side < 6; side++) if ((mask & (1 << side)) != 0) shape = Shapes.or(shape, arms[side]);
            SHAPES[mask] = shape;
        }
    }

    public PneumaticPipeBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty side : SIDES) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES);
    }

    /** A pipe joins other pipes, and interfaces that have their back towards it. */
    private boolean connects(BlockState neighbour, Direction towardsNeighbour) {
        boolean fluid = this instanceof FluidPipeBlock;
        return neighbour.getBlock() instanceof PneumaticPipeBlock && (neighbour.getBlock() instanceof FluidPipeBlock)==fluid && FluidPipeBlock.open(neighbour,towardsNeighbour.getOpposite())
            || (neighbour.getBlock() instanceof LogisticsPortBlock && (fluid ? neighbour.getValue(LogisticsPortBlock.FACING)!=towardsNeighbour.getOpposite() : neighbour.getValue(LogisticsPortBlock.FACING)==towardsNeighbour));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(SIDES[direction.get3DDataValue()],
                connects(context.getLevel().getBlockState(context.getClickedPos().relative(direction)), direction));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(SIDES[direction.get3DDataValue()], FluidPipeBlock.open(state,direction) && connects(neighbourState, direction));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int side = 0; side < 6; side++) if (state.getValue(SIDES[side])) mask |= 1 << side;
        return SHAPES[mask];
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) LogisticsManager.invalidate(level);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        LogisticsManager.invalidate(level);
    }
}
