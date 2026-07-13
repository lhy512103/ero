package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;

public class RubberWood extends RotatedPillarBlock {

    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    public static final BooleanProperty WET = BooleanProperty.create("wet");

    public RubberWood(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(FACING, Direction.NORTH)
            .setValue(WET, false));
    }

    public static BlockBehaviour.Properties defaultProps() {
        return Properties.of()
            .mapColor(MapColor.WOOD)
            .destroyTime(2.0f)
            .sound(SoundType.WOOD)
            .randomTicks();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, FACING, WET);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        return this.defaultBlockState()
            .setValue(AXIS, clickedFace.getAxis())
            .setValue(FACING, clickedFace.getOpposite())
            .setValue(WET, false);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WET) && state.getValue(FACING) != Direction.UP && random.nextInt(9) == 0) {
            level.setBlock(pos, state.setValue(WET, true), 3);
        }
    }
}
