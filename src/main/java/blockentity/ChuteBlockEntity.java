package blockentity;

import block.ChuteBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** One buffered stack; every {@link #INTERVAL} ticks it is refilled from above and passed on. */
public class ChuteBlockEntity extends BlockEntity {
    /** Ticks between transfers; a whole stack moves each time (320 items per second at most). */
    public static final int INTERVAL = 4;

    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };
    private int cooldown;

    public ChuteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHUTE.get(), pos, state);
    }

    public ItemStacksResourceHandler getInventory() { return inventory; }

    public void tick() {
        if (level == null || level.isClientSide() || ++cooldown < INTERVAL) return;
        cooldown = 0;
        if (inventory.getAmountAsInt(0) == 0) pull();
        if (inventory.getAmountAsInt(0) > 0) push();
    }

    private void pull() {
        var above = level.getCapability(Capabilities.Item.BLOCK, worldPosition.above(), Direction.DOWN);
        if (above != null) {
            for (int slot = 0; slot < above.size(); slot++) {
                ItemResource resource = above.getResource(slot);
                if (resource.isEmpty() || above.getAmountAsInt(slot) <= 0) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int taken = above.extract(slot, resource, resource.getMaxStackSize(), transaction);
                    if (taken > 0 && inventory.insert(0, resource, taken, transaction) == taken) {
                        transaction.commit();
                        return;
                    }
                }
            }
            return;
        }
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition.above()), ItemEntity::isAlive)) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            int taken;
            try (Transaction transaction = Transaction.openRoot()) {
                taken = inventory.insert(0, ItemResource.of(stack), stack.getCount(), transaction);
                transaction.commit();
            }
            if (taken <= 0) continue;
            if (taken >= stack.getCount()) {
                entity.discard();
            } else {
                ItemStack rest = stack.copy();
                rest.shrink(taken);
                entity.setItem(rest);
            }
            return;
        }
    }

    private void push() {
        Direction facing = getBlockState().getValue(ChuteBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        ItemResource resource = inventory.getResource(0);
        int amount = inventory.getAmountAsInt(0);
        var target = level.getCapability(Capabilities.Item.BLOCK, targetPos, facing.getOpposite());
        if (target != null) {
            try (Transaction transaction = Transaction.openRoot()) {
                int moved = target.insert(resource, amount, transaction);
                if (moved > 0 && inventory.extract(0, resource, moved, transaction) == moved) transaction.commit();
            }
            return;
        }
        // nothing to insert into: let the items fall out, e.g. onto a conveyor
        if (!level.getBlockState(targetPos).isCollisionShapeFullBlock(level, targetPos)) {
            inventory.set(0, ItemResource.EMPTY, 0);
            level.addFreshEntity(new ItemEntity(level, targetPos.getX() + 0.5, targetPos.getY() + 0.3, targetPos.getZ() + 0.5,
                resource.toStack(amount), 0, 0, 0));
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && inventory.getAmountAsInt(0) > 0) {
            ItemStack stack = inventory.getResource(0).toStack(inventory.getAmountAsInt(0));
            inventory.set(0, ItemResource.EMPTY, 0);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
    }
}
