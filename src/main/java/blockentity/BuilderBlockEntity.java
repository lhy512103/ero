package blockentity;

import block.ModBlocks;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Builder I: placed next to the controller of a large machine, it puts plant casing into every
 * free position of the machine's shell, 16 blocks per second for {@link #COST} EU each. It takes
 * casing by hand, from hoppers, or asks the pipe network for exactly as much as is still missing.
 * Hatches are placed by the player first; the builder fills in everything else.
 */
public class BuilderBlockEntity extends MachineBlockEntity implements EnergyDisplay {
    public static final int ROUND = 5, BLOCKS_PER_ROUND = 4;
    public static final long COST = 4;

    private final EUEnergyStorageImpl storage = new EUEnergyStorageImpl(4000, EUVoltage.LV);
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(9) {
        @Override
        public boolean isValid(int index, ItemResource resource) { return isCasing(resource); }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) { setChanged(); }
    };
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    private int wait, missing;

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUILDER.get(), pos, state);
    }

    private static boolean isCasing(ItemResource resource) { return resource.getItem() == ModBlocks.PLANT_CASING.get().asItem(); }

    @Override public String getMachineName() { return "builder"; }
    @Override public EUEnergyStorage getEnergyStorage() { return storage; }
    @Override public ItemStacksResourceHandler getItemHandler() { return inventory; }
    @Override public int[] getInputSlots() { return range(0, 9); }
    @Override public int[] getOutputSlots() { return new int[0]; }
    @Override protected boolean canExtractEU() { return false; }
    @Override public boolean acceptsAutomationInput(int slot, ItemResource resource) { return isCasing(resource); }

    /** Asks the pipe network for casing only while the machine next to it still lacks more than the builder holds. */
    @Override
    public int requestSlot(ItemResource resource) {
        return isCasing(resource) && missing > held() ? slotFor(resource, range(0, 9)) : -1;
    }

    public int held() {
        int total = 0;
        for (int slot = 0; slot < inventory.size(); slot++) total += inventory.getAmountAsInt(slot);
        return total;
    }

    @Nullable
    public PlantControllerBlockEntity controller() {
        if (level == null) return null;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof PlantControllerBlockEntity controller) return controller;
        }
        return null;
    }

    public Component describe() {
        PlantControllerBlockEntity controller = controller();
        if (controller == null) return Component.translatable("message.iuce.builder.no_controller");
        if (controller.isFormed()) return Component.translatable("message.iuce.builder.done");
        return Component.translatable("message.iuce.builder.status", controller.missing().size(), held());
    }

    private boolean takeCasing() {
        for (int slot = 0; slot < inventory.size(); slot++) {
            int amount = inventory.getAmountAsInt(slot);
            if (amount <= 0) continue;
            ItemResource resource = inventory.getResource(slot);
            inventory.set(slot, amount == 1 ? ItemResource.EMPTY : resource, amount - 1);
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || tickCount % ROUND != 0) return;
        PlantControllerBlockEntity controller = controller();
        if (controller == null || controller.isFormed()) {
            queue.clear();
            missing = 0;
            return;
        }
        if (queue.isEmpty()) {
            if (wait-- > 0) return;
            wait = 4;   // look at the whole structure again at most once a second
            List<BlockPos> free = controller.missing();
            free.remove(worldPosition);
            queue.addAll(free);
            missing = queue.size();
            if (queue.isEmpty()) return;
        }
        int placed = 0;
        while (placed < BLOCKS_PER_ROUND && !queue.isEmpty()) {
            BlockPos pos = queue.peek();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced()) {
                queue.poll();
                continue;
            }
            if (storage.getEnergyStored() < COST || !takeCasing()) break;
            storage.removeEnergy(COST);
            level.setBlock(pos, ModBlocks.PLANT_CASING.get().defaultBlockState(), Block.UPDATE_ALL);
            queue.poll();
            placed++;
        }
        missing = queue.size();
        if (placed > 0) controller.markStructureDirty();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", storage.getEnergyStored());
        inventory.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(storage::setEnergy);
        inventory.deserialize(input);
    }
}
