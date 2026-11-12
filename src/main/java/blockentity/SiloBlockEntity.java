package blockentity;

import logistics.SiloInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Silo: 64 stacks of a single kind of item. Right-click with items to put them in, with an empty hand to take a stack. */
public class SiloBlockEntity extends BlockEntity {
    public static final long CAPACITY = 64 * 64;
    private final SiloInventory inventory = new SiloInventory(CAPACITY, this::setChanged);

    public SiloBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILO.get(), pos, state);
    }

    public SiloInventory getInventory() { return inventory; }

    public Component describe() {
        if (inventory.getAmount() <= 0) return Component.translatable("message.iuce.silo.empty");
        return Component.translatable("message.iuce.silo.status", inventory.getResource().toStack(1).getHoverName(),
            inventory.getAmount(), CAPACITY);
    }

    public boolean deposit(Player player, ItemStack stack) {
        int moved;
        try (Transaction transaction = Transaction.openRoot()) {
            moved = inventory.insert(0, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
        if (moved > 0 && !player.hasInfiniteMaterials()) stack.shrink(moved);
        player.sendOverlayMessage(describe());
        return moved > 0;
    }

    public void withdraw(Player player) {
        ItemResource resource = inventory.getResource();
        if (!resource.isEmpty()) {
            int moved;
            try (Transaction transaction = Transaction.openRoot()) {
                moved = inventory.extract(0, resource, resource.getMaxStackSize(), transaction);
                transaction.commit();
            }
            ItemStack taken = resource.toStack(moved);
            if (moved > 0 && !player.getInventory().add(taken)) player.drop(taken, false);
        }
        player.sendOverlayMessage(describe());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        ItemResource resource = inventory.getResource();
        long left = inventory.getAmount();
        inventory.set(ItemResource.EMPTY, 0);
        while (left > 0) {
            int count = (int) Math.min(left, resource.getMaxStackSize());
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, resource.toStack(count)));
            left -= count;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Item", ItemResource.OPTIONAL_CODEC, inventory.getResource());
        output.putLong("Amount", inventory.getAmount());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.set(input.read("Item", ItemResource.OPTIONAL_CODEC).orElse(ItemResource.EMPTY), input.getLong("Amount").orElse(0L));
    }
}
