package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PlantCasingBlock extends Block {
    private static final BooleanProperty[] JOINS = {BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST};

    public PlantCasingBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty join : JOINS) state = state.setValue(join, false);
        registerDefaultState(state);
    }

    public static boolean joins(BlockState state) {
        return state.getBlock() instanceof PlantCasingBlock || state.getBlock() instanceof PlantControllerBlock
            || state.is(ModBlocks.ITEM_INPUT_HATCH.get()) || state.is(ModBlocks.ITEM_OUTPUT_HATCH.get())
            || state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(JOINS); }

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
