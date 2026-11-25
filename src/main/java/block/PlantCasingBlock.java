package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PlantCasingBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final BooleanProperty[] JOINS = {BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST};

    public PlantCasingBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(FORMED, false);
        for (BooleanProperty join : JOINS) state = state.setValue(join, false);
        registerDefaultState(state);
    }

    public static boolean joins(BlockState state) {
        return state.getBlock() instanceof PlantCasingBlock || state.getBlock() instanceof PlantControllerBlock
            || state.is(ModBlocks.ITEM_INPUT_HATCH.get()) || state.is(ModBlocks.ITEM_OUTPUT_HATCH.get())
            || state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JOINS);
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(FORMED) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(FORMED) || super.propagatesSkylightDown(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    public BlockState joinedState(LevelReader level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            state = state.setValue(JOINS[direction.get3DDataValue()], level.hasChunkAt(neighbour) && joins(level.getBlockState(neighbour)));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) { return joinedState(context.getLevel(), context.getClickedPos()); }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return state.setValue(JOINS[direction.get3DDataValue()], joins(neighbour));
    }
}
