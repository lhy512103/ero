package block;

import blockentity.BuilderBlockEntity;
import blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Builder I. Right-click with plant casing to load it, with an empty hand to see how much is still missing. */
public class BuilderBlock extends MachineBlock {
    public BuilderBlock(Properties properties) {
        super(properties, BuilderBlockEntity::new, ModBlockEntities.BUILDER);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModBlocks.PLANT_CASING.get().asItem()) || !(level.getBlockEntity(pos) instanceof BuilderBlockEntity builder)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int moved;
        try (Transaction transaction = Transaction.openRoot()) {
            moved = builder.getItemHandler().insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
        if (!player.hasInfiniteMaterials()) stack.shrink(moved);
        player.sendOverlayMessage(builder.describe());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BuilderBlockEntity builder) player.sendOverlayMessage(builder.describe());
        return InteractionResult.SUCCESS;
    }
}
