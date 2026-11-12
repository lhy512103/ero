package block;

import blockentity.HatchBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/** Item hatch of a large machine. Right-click with items to put them in, with an empty hand to take a stack out. */
public class HatchBlock extends BaseEntityBlock {
    public static final MapCodec<HatchBlock> CODEC = BlockBehaviour.simpleCodec(HatchBlock::new);

    public HatchBlock(Properties properties) { super(properties); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HatchBlockEntity(pos, state); }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) || hatch.isOutput()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int moved;
        try (Transaction transaction = Transaction.openRoot()) {
            moved = hatch.getInventory().insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
        if (!player.hasInfiniteMaterials()) stack.shrink(moved);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof HatchBlockEntity hatch)) return InteractionResult.SUCCESS;
        var inventory = hatch.getInventory();
        for (int slot = inventory.size() - 1; slot >= 0; slot--) {
            int amount = inventory.getAmountAsInt(slot);
            if (amount <= 0) continue;
            ItemStack taken = inventory.getResource(slot).toStack(amount);
            inventory.set(slot, ItemResource.EMPTY, 0);
            if (!player.getInventory().add(taken)) player.drop(taken, false);
            break;
        }
        return InteractionResult.SUCCESS;
    }
}
