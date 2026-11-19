package block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;

/** Energy hatch for multiblock plants; hides its block geometry while part of a formed 3D plant. */
public class EnergyHatchBlock extends MachineBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public EnergyHatchBlock(Properties properties,
                            BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory,
                            Supplier<? extends BlockEntityType<?>> blockEntityTypeSupplier) {
        super(properties, blockEntityFactory, blockEntityTypeSupplier);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }
}
