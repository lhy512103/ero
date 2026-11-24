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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Full-block structural casing or deck plate whose outer edge trims hide wherever a compatible
 * block touches it, so adjacent blocks merge into one continuous steel surface.
 */
public class ConnectedStructureBlock extends StructureBlock {
    private final Predicate<BlockState> joins;

    public ConnectedStructureBlock(Properties properties, Predicate<BlockState> joins) {
        super(properties);
        this.joins = joins;
        BlockState state = defaultBlockState().setValue(FORMED, false);
        for (BooleanProperty prop : PipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(prop, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        for (BooleanProperty prop : PipeBlock.PROPERTY_BY_DIRECTION.values()) builder.add(prop);
    }

    public boolean joinsWith(BlockState neighbour) {
        return joins.test(neighbour);
    }

    public BlockState joinedState(LevelReader level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                level.hasChunkAt(neighbour) && joinsWith(level.getBlockState(neighbour)));
        }
        return state;
    }

    public BlockState joinedInLayout(BlockState state, BlockPos pos, Map<BlockPos, BlockState> placed) {
        for (Direction direction : Direction.values()) {
            BlockState neighbour = placed.get(pos.relative(direction));
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighbour != null && joinsWith(neighbour));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return joinedState(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), joinsWith(neighbour));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(FORMED) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
