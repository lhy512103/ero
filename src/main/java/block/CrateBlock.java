package block;

import blockentity.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CrateBlock extends ColoredPlasticBlock implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty STACKED =
        net.minecraft.world.level.block.state.properties.BooleanProperty.create("stacked");
    public CrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STACKED, false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STACKED);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(STACKED, context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
    }
    @Override protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
            net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, net.minecraft.core.Direction direction,
            BlockPos neighbor, BlockState adjacent, net.minecraft.util.RandomSource random) {
        return direction == net.minecraft.core.Direction.DOWN ? state.setValue(STACKED, adjacent.is(this)) : state;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrateBlockEntity(pos,state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) player.openMenu(crate);
        return InteractionResult.SUCCESS;
    }
}
