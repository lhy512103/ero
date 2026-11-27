package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
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
 * Structural H-section column / girder: connects seamlessly along its own axis and extends
 * bolted gusset brackets to perpendicular H-beams on any side so columns and cross-girders
 * visibly hinge together.
 */
public class PlantFrameBlock extends RotatedPillarBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final VoxelShape Y = Shapes.or(box(0, 0, 0, 16, 16, 3), box(0, 0, 13, 16, 16, 16), box(6.5, 0, 3, 9.5, 16, 13));
    private static final VoxelShape Z = Shapes.or(box(0, 0, 0, 16, 3, 16), box(0, 13, 0, 16, 16, 16), box(6.5, 3, 0, 9.5, 13, 16));
    private static final VoxelShape X = Shapes.or(box(0, 0, 0, 16, 3, 16), box(0, 13, 0, 16, 16, 16), box(0, 3, 6.5, 16, 13, 9.5));

    public PlantFrameBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(FORMED, false);
        for (BooleanProperty prop : PipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(prop, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
        for (BooleanProperty prop : PipeBlock.PROPERTY_BY_DIRECTION.values()) builder.add(prop);
    }

    protected boolean connectsTo(BlockState self, Direction dir, BlockState neighbour) {
        return neighbour.getBlock() instanceof PlantFrameBlock && !(neighbour.getBlock() instanceof ProcessPipeBlock);
    }

    public BlockState joinedState(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos np = pos.relative(direction);
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                level.hasChunkAt(np) && connectsTo(state, direction, level.getBlockState(np)));
        }
        return state;
    }

    public BlockState joinedInLayout(BlockState state, BlockPos pos, Map<BlockPos, BlockState> placed) {
        for (Direction direction : Direction.values()) {
            BlockState nb = placed.get(pos.relative(direction));
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), nb != null && connectsTo(state, direction, nb));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        return base == null ? null : joinedState(base, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectsTo(state, direction, neighbour));
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
