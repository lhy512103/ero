package block;

import blockentity.FactoryHubBlockEntity;
import blockentity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/** Factory hub: right-click with a wanted product to hand it in, otherwise opens the rating screen. */
public class FactoryHubBlock extends BaseEntityBlock {
    public static final MapCodec<FactoryHubBlock> CODEC = BlockBehaviour.simpleCodec(FactoryHubBlock::new);

    public FactoryHubBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FactoryHubBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, ModBlockEntities.FACTORY_HUB.get(), (l, pos, s, hub) -> hub.tick());
    }

    /** The hub belongs to whoever placed it, and keeps its own chunk loaded so a rating can finish while the player is away. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (by instanceof Player player && level.getBlockEntity(pos) instanceof FactoryHubBlockEntity hub) {
            hub.setOwner(player.getUUID(), player.getGameProfile().name());
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server) server.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FactoryHubBlockEntity hub) || !hub.accepts(stack.getItem())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int moved;
        try (Transaction transaction = Transaction.openRoot()) {
            moved = hub.getIntake().insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
        if (!player.hasInfiniteMaterials()) stack.shrink(moved);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FactoryHubBlockEntity hub) player.openMenu(hub, pos);
        return InteractionResult.SUCCESS;
    }
}
