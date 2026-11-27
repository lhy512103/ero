package block;

import com.mojang.serialization.MapCodec;
import machine.utility.Utility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
 * A fluid pipe. It carries no fluid of its own: it only joins a machine to a utility supplier, so
 * a boiler can feed machines that are not touching it. The block remembers which {@link Utility}
 * it belongs to so a run of pipes carries one utility and never mixes two.
 */
public class UtilityPipeBlock extends Block {

    /** Only used when a data pack or a structure places the block by codec; it always comes back as a steam pipe. */
    public static final MapCodec<UtilityPipeBlock> CODEC = BlockBehaviour.simpleCodec(
        properties -> new UtilityPipeBlock(Utility.STEAM, properties));

    /** Indexed by {@link Direction#get3DDataValue()}: down, up, north, south, west, east. */
    private static final BooleanProperty[] SIDES = {BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST};
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape[] arms = {Block.box(4, 0, 4, 12, 4, 12), Block.box(4, 12, 4, 12, 16, 12), Block.box(4, 4, 0, 12, 12, 4),
            Block.box(4, 4, 12, 12, 12, 16), Block.box(0, 4, 4, 4, 12, 12), Block.box(12, 4, 4, 16, 12, 12)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = Block.box(4, 4, 4, 12, 12, 12);
            for (int side = 0; side < 6; side++) if ((mask & (1 << side)) != 0) shape = Shapes.or(shape, arms[side]);
            SHAPES[mask] = shape;
        }
    }

    private final Utility utility;

    public UtilityPipeBlock(Utility utility, Properties properties) {
        super(properties);
        this.utility = utility;
        BlockState state = defaultBlockState();
        for (BooleanProperty side : SIDES) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    public Utility utility() { return utility; }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES);
    }

    /** Pipes of the same utility join each other; any machine or supplier joins from that side. */
    private static boolean connects(Utility utility, BlockState neighbour) {
        if (neighbour.getBlock() instanceof UtilityPipeBlock pipe) return pipe.utility == utility;
        return neighbour.getBlock() instanceof MachineBlock || neighbour.getBlock() instanceof CreativeUtilitySourceBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(SIDES[direction.get3DDataValue()],
                connects(utility, context.getLevel().getBlockState(context.getClickedPos().relative(direction))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(SIDES[direction.get3DDataValue()], connects(utility, neighbourState));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int side = 0; side < 6; side++) if (state.getValue(SIDES[side])) mask |= 1 << side;
        return SHAPES[mask];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) { return true; }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0f; }
}
