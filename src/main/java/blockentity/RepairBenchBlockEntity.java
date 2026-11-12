package blockentity;

import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * Electric repair bench: restores the durability of any damaged tool, weapon or armour piece with
 * electricity, one point per tick for {@link #COST} EU, without using up materials or experience.
 * Slot 0 holds the item being repaired, slot 1 the finished one.
 */
public class RepairBenchBlockEntity extends MachineBlockEntity implements EnergyDisplay {
    public static final long COST = 8;
    private final EUEnergyStorageImpl storage = new EUEnergyStorageImpl(4000, EUVoltage.LV);
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(2) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) { setChanged(); }
    };

    public RepairBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REPAIR_BENCH.get(), pos, state);
    }

    public static boolean repairable(ItemStack stack) { return stack.isDamageableItem() && stack.isDamaged(); }

    @Override public String getMachineName() { return "repair_bench"; }
    @Override public EUEnergyStorage getEnergyStorage() { return storage; }
    @Override public ItemStacksResourceHandler getItemHandler() { return inventory; }
    @Override public int[] getInputSlots() { return new int[]{0}; }
    @Override public int[] getOutputSlots() { return new int[]{1}; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    public boolean acceptsAutomationInput(int slot, ItemResource resource) { return repairable(resource.toStack(1)); }

    @Override
    public int requestSlot(ItemResource resource) {
        return inventory.getAmountAsInt(0) == 0 && repairable(resource.toStack(1)) ? 0 : -1;
    }

    public Component describe() {
        if (inventory.getAmountAsInt(0) > 0) {
            ItemStack stack = inventory.getResource(0).toStack(1);
            return Component.translatable("message.iuce.repair.working", stack.getHoverName(),
                stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage());
        }
        return Component.translatable(inventory.getAmountAsInt(1) > 0 ? "message.iuce.repair.done" : "message.iuce.repair.empty");
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || inventory.getAmountAsInt(0) <= 0) return;
        ItemStack stack = inventory.getResource(0).toStack(1);
        if (repairable(stack)) {
            if (storage.getEnergyStored() < COST) return;
            storage.removeEnergy(COST);
            stack.setDamageValue(stack.getDamageValue() - 1);
            inventory.set(0, ItemResource.of(stack), 1);
        } else if (inventory.getAmountAsInt(1) == 0) {
            inventory.set(0, ItemResource.EMPTY, 0);
            inventory.set(1, ItemResource.of(stack), 1);
        }
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
