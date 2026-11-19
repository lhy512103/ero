package block;

import blockentity.ModBlockEntities;
import blockentity.RepairBenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Electric repair bench. Right-click with a damaged item to put it in, with an empty hand to take it back. */
public class RepairBenchBlock extends MachineBlock {
    public RepairBenchBlock(Properties properties) {
        super(properties, RepairBenchBlockEntity::new, ModBlockEntities.REPAIR_BENCH);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!RepairBenchBlockEntity.repairable(stack) || !(level.getBlockEntity(pos) instanceof RepairBenchBlockEntity bench)
                || bench.getItemHandler().getAmountAsInt(0) > 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        bench.getItemHandler().set(0, ItemResource.of(stack), 1);
        player.setItemInHand(hand, ItemStack.EMPTY);
        player.sendOverlayMessage(bench.describe());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof RepairBenchBlockEntity bench)) return InteractionResult.SUCCESS;
        var inventory = bench.getItemHandler();
        // the finished item first; sneaking takes back an unfinished one
        int slot = inventory.getAmountAsInt(1) > 0 ? 1 : player.isShiftKeyDown() && inventory.getAmountAsInt(0) > 0 ? 0 : -1;
        if (slot >= 0) {
            ItemStack taken = inventory.getResource(slot).toStack(1);
            inventory.set(slot, ItemResource.EMPTY, 0);
            if (!player.getInventory().add(taken)) player.drop(taken, false);
        }
        player.sendOverlayMessage(bench.describe());
        return InteractionResult.SUCCESS;
    }
}
