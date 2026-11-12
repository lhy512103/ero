package machine.induction_furnace;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.InductionFurnaceGuiProvider;
import machine.PoweredMachineBlockEntity;
import machine.induction_furnace.menu.InductionFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import network.performance.MachinePriority;
import recipe.MachineRecipe;
import recipe.RecipeHelper;

import javax.annotation.Nullable;

/**
 * 感应炉：MV，缓冲 8000 EU。槽位：2 输入 + 1 输出 + 4 升级，运行 {@code alloy_furnace} 配方，
 * 两个输入的摆放顺序不限。紧贴电炉时为电炉提供加速、并行和电力（见电炉）。
 */
public class InductionFurnaceBlockEntity extends PoweredMachineBlockEntity implements InductionFurnaceGuiProvider {

    /** 每台相邻感应炉给电炉增加的速度 */
    public static final double SPEED_BOOST_PER_FURNACE = 0.5;
    /** 每台相邻感应炉给电炉增加的并行数 */
    public static final int PARALLEL_PER_FURNACE = 1;

    private static final int INPUT_SLOT_1 = 0;
    private static final int INPUT_SLOT_2 = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final Component DISPLAY_NAME = Component.translatable("block.iuce.induction_furnace");

    @Nullable
    private MachineRecipe cachedRecipe;
    private ItemStack cachedOutput = ItemStack.EMPTY;
    private ItemResource cachedInput1 = ItemResource.EMPTY;
    private ItemResource cachedInput2 = ItemResource.EMPTY;

    public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTION_FURNACE.get(), pos, state, EUVoltage.MV, 8000, 7, 3, 200);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        processUpgrades();

        boolean wasWorking = isWorking();
        ItemResource inputRes1 = itemHandler.getResource(INPUT_SLOT_1);
        ItemResource inputRes2 = itemHandler.getResource(INPUT_SLOT_2);
        int input1Amount = itemHandler.getAmountAsInt(INPUT_SLOT_1);
        int input2Amount = itemHandler.getAmountAsInt(INPUT_SLOT_2);

        if (!inputRes1.isEmpty() && input1Amount > 0 && !inputRes2.isEmpty() && input2Amount > 0) {
            if (!inputRes1.equals(cachedInput1) || !inputRes2.equals(cachedInput2)) {
                cachedInput1 = inputRes1;
                cachedInput2 = inputRes2;
                ItemStack stack1 = inputRes1.toStack(1);
                ItemStack stack2 = inputRes2.toStack(1);
                cachedRecipe = RecipeHelper.findMachineRecipe(level, "alloy_furnace", stack1, stack2);
                if (cachedRecipe == null) cachedRecipe = RecipeHelper.findMachineRecipe(level, "alloy_furnace", stack2, stack1);
                cachedOutput = cachedRecipe != null ? cachedRecipe.getOutput().copy() : ItemStack.EMPTY;
            }

            if (cachedRecipe != null && !cachedOutput.isEmpty()) {
                maxProgress = overclockedTime(cachedRecipe.getProcessTime());
                long energyPerTick = overclockedEnergy(cachedRecipe.getEnergyPerTick());

                boolean stalled = input1Amount < cachedRecipe.getInputCount()
                    || input2Amount < cachedRecipe.getInput2Count()
                    || energyStorage.removeEnergy(energyPerTick) <= 0;
                if (stalled) {
                    if (wasWorking) {
                        updateRunningState();
                        setChanged();
                    }
                    return;
                }

                progress++;
                if (progress >= maxProgress && tryProcessOutput()) progress = 0;
            } else {
                progress = 0;
                cachedOutput = ItemStack.EMPTY;
                cachedRecipe = null;
            }
        } else {
            progress = 0;
            clearCache();
        }

        if (wasWorking != isWorking()) {
            updateRunningState();
            setChanged();
        }
    }

    private boolean tryProcessOutput() {
        if (cachedRecipe == null || cachedOutput.isEmpty()) return false;

        int input1Count = cachedRecipe.getInputCount();
        int input2Count = cachedRecipe.getInput2Count();
        if (itemHandler.getAmountAsInt(INPUT_SLOT_1) < input1Count) return false;
        if (itemHandler.getAmountAsInt(INPUT_SLOT_2) < input2Count) return false;

        ItemResource outputRes = ItemResource.of(cachedOutput);
        ItemResource currentOutput = itemHandler.getResource(OUTPUT_SLOT);
        if (!currentOutput.isEmpty() && !currentOutput.equals(outputRes)) return false;
        if (itemHandler.getAmountAsInt(OUTPUT_SLOT) + cachedOutput.getCount() > cachedOutput.getMaxStackSize()) return false;

        try (Transaction tx = Transaction.openRoot()) {
            if (itemHandler.extract(INPUT_SLOT_1, itemHandler.getResource(INPUT_SLOT_1), input1Count, tx) < input1Count) return false;
            if (itemHandler.extract(INPUT_SLOT_2, itemHandler.getResource(INPUT_SLOT_2), input2Count, tx) < input2Count) return false;
            if (itemHandler.insert(OUTPUT_SLOT, outputRes, cachedOutput.getCount(), tx) < cachedOutput.getCount()) return false;
            tx.commit();
        }
        return true;
    }

    private void clearCache() {
        cachedRecipe = null;
        cachedOutput = ItemStack.EMPTY;
        cachedInput1 = ItemResource.EMPTY;
        cachedInput2 = ItemResource.EMPTY;
    }

    /** 为相邻电炉提供能量，返回实际提供的量 */
    public long provideEnergy(long maxTransfer) {
        long available = Math.min(energyStorage.getEnergyStored(), maxTransfer);
        if (available <= 0) return 0;
        energyStorage.removeEnergy(available);
        return available;
    }

    @Override public String getMachineName() { return "感应炉"; }
    @Override public Component getDisplayName() { return DISPLAY_NAME; }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InductionFurnaceMenu(containerId, playerInventory, this);
    }

    @Override public int[] getInputSlots() { return new int[]{INPUT_SLOT_1, INPUT_SLOT_2}; }
    @Override public int[] getOutputSlots() { return new int[]{OUTPUT_SLOT}; }
    @Override public MachinePriority getMachinePriority() { return MachinePriority.HIGH; }

    @Override
    protected void loadExtra(ValueInput input) {
        clearCache();
    }
}
