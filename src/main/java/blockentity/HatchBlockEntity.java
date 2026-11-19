package blockentity;

import block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;

/**
 * Item hatch of a large machine: nine slots the controller reads from (input hatch) or writes to
 * (output hatch). A hatch does no work of its own. From outside, an input hatch only takes items
 * in and an output hatch only lets them out.
 */
public class HatchBlockEntity extends BlockEntity {
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(9) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) { setChanged(); }
    };
    @Nullable private BlockPos controller;

    private final ResourceHandler<ItemResource> external = new ResourceHandler<>() {
        @Override public int size() { return inventory.size(); }
        @Override public ItemResource getResource(int index) { return inventory.getResource(index); }
        @Override public long getAmountAsLong(int index) { return inventory.getAmountAsLong(index); }
        @Override public long getCapacityAsLong(int index, ItemResource resource) { return inventory.getCapacityAsLong(index, resource); }
        @Override public boolean isValid(int index, ItemResource resource) { return !isOutput() && inventory.isValid(index, resource); }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return isOutput() ? 0 : inventory.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return isOutput() ? inventory.extract(index, resource, amount, transaction) : 0;
        }
    };

    public HatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_HATCH.get(), pos, state);
    }

    public boolean isOutput() { return getBlockState().is(ModBlocks.ITEM_OUTPUT_HATCH.get()); }

    /** Full access, for the controller and for players. */
    public ItemStacksResourceHandler getInventory() { return inventory; }

    /** One-way access, for hoppers, pipes and logistics interfaces. */
    public ResourceHandler<ItemResource> external() { return external; }

    public void setController(@Nullable BlockPos controller) { this.controller = controller; }

    /** True when this is an input hatch of a formed machine that can process the item. */
    public boolean wants(ItemResource resource) {
        return !isOutput() && controller != null && level != null && level.isLoaded(controller)
            && level.getBlockEntity(controller) instanceof PlantControllerBlockEntity plant && plant.canProcess(resource);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (int slot = 0; slot < inventory.size(); slot++) {
            int amount = inventory.getAmountAsInt(slot);
            if (amount <= 0) continue;
            ItemStack stack = inventory.getResource(slot).toStack(amount);
            inventory.set(slot, ItemResource.EMPTY, 0);
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
