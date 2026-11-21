package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Studded plastic toy brick: can be placed in all six directions (studs pointing away from the
 * clicked face) and merges its edges seamlessly with adjacent same-colour, same-facing bricks.
 */
public class ToyBrickBlock extends ColoredPlasticBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    // Local-space horizontal edges (when facing == UP these match world N/E/S/W).
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final BooleanProperty[] EDGES = {NORTH, EAST, SOUTH, WEST};

    public ToyBrickBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.UP)
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, NORTH, EAST, SOUTH, WEST);
    }

    /**
     * Maps the four model-local horizontal edges (N, E, S, W) to world directions for the given
     * blockstate rotation (up: 0/0, down: x180, north: x90, east: x90 y90, south: x90 y180, west: x90 y270).
     */
    private static Direction[] worldEdges(Direction facing) {
        return switch (facing) {
            case UP -> new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            case DOWN -> new Direction[]{Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
            case NORTH -> new Direction[]{Direction.DOWN, Direction.EAST, Direction.UP, Direction.WEST};
            case EAST -> new Direction[]{Direction.DOWN, Direction.SOUTH, Direction.UP, Direction.NORTH};
            case SOUTH -> new Direction[]{Direction.DOWN, Direction.WEST, Direction.UP, Direction.EAST};
            case WEST -> new Direction[]{Direction.DOWN, Direction.NORTH, Direction.UP, Direction.SOUTH};
        };
    }

    private boolean connectsTo(BlockState self, BlockState neighbor) {
        return neighbor.is(this)
            && neighbor.getValue(COLOR) == self.getValue(COLOR)
            && neighbor.getValue(FACING) == self.getValue(FACING);
    }

    private BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        Direction[] dirs = worldEdges(state.getValue(FACING));
        for (int i = 0; i < 4; i++) {
            BlockPos neighborPos = pos.relative(dirs[i]);
            boolean connected = level.hasChunkAt(neighborPos) && connectsTo(state, level.getBlockState(neighborPos));
            state = state.setValue(EDGES[i], connected);
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockItemStateProperties props = context.getItemInHand().get(DataComponents.BLOCK_STATE);
        DyeColor color = props != null ? props.get(COLOR) : null;
        BlockState state = defaultBlockState()
            .setValue(COLOR, color != null ? color : DyeColor.WHITE)
            .setValue(FACING, context.getClickedFace());
        return withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        Direction[] dirs = worldEdges(state.getValue(FACING));
        for (int i = 0; i < 4; i++) {
            if (dirs[i] == direction) {
                return state.setValue(EDGES[i], connectsTo(state, neighbor));
            }
        }
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        return false;
    }
}
