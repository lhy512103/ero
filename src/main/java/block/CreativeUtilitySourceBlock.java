package block;

import com.mojang.serialization.MapCodec;
import machine.utility.CreativeUtilitySourceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Creative-only block that hands out every utility at once. It is meant for building and for the
 * regression tests, where a boiler and a cooling tower would only add setup.
 */
public class CreativeUtilitySourceBlock extends BaseEntityBlock {

    public static final MapCodec<CreativeUtilitySourceBlock> CODEC = BlockBehaviour.simpleCodec(CreativeUtilitySourceBlock::new);

    public CreativeUtilitySourceBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeUtilitySourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != blockentity.ModBlockEntities.CREATIVE_UTILITY_SOURCE.get()) return null;
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof blockentity.TickableBlockEntity tickable) tickable.tick();
        };
    }
}
