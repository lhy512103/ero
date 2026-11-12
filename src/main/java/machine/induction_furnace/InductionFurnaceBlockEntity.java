package machine.induction_furnace;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import blockentity.ProcessingDisplay;
import energy.EUEnergyStorageImpl;
import energy.EUEnergyStorage;
import energy.EUVoltage;
import machine.InductionFurnaceGuiProvider;
import machine.induction_furnace.menu.InductionFurnaceMenu;
import network.performance.MachinePriority;
import recipe.MachineRecipe;
import recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/**
 * 感应炉方块实体
 * <p>
 * 功能：
 * 1. 为相邻电炉提供效率加成（速度+50%、并行+1/每个感应炉）
 * 2. 内部缓冲能量可被电炉抽取使用
 * 3. 处理合金配方（双输入 → 单输出）
 * </p>
 */
public class InductionFurnaceBlockEntity extends MachineBlockEntity
    implements EnergyDisplay, ProcessingDisplay, InductionFurnaceGuiProvider {

    /** 每个感应炉提供的速度加成倍率 (50%) */
    public static final double SPEED_BOOST_PER_FURNACE = 0.5;

    /** 每个感应炉增加的并行处理数 */
    public static final int PARALLEL_PER_FURNACE = 1;

    private static final int INPUT_SLOT_1 = 0;
    private static final int INPUT_SLOT_2 = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final long BASE_ENERGY_CONSUMPTION = 3;
    private static final int BASE_PROCESS_TIME = 200;

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final ItemStacksResourceHandler itemHandler;

    private int progress = 0;
    private int maxProgress = BASE_PROCESS_TIME;

    /** 缓存的合金配方 */
    @Nullable
    private MachineRecipe cachedRecipe;
    /** 缓存的输出物品 */
    private ItemStack cachedOutput = ItemStack.EMPTY;
    /** 缓存的输入组合，用于检测输入变化 */
    private ItemResource cachedInput1 = ItemResource.EMPTY;
    private ItemResource cachedInput2 = ItemResource.EMPTY;
    private int cachedOverclockerCount = -1;

    public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTION_FURNACE.get(), pos, state);
        this.voltage = EUVoltage.MV;
        this.energyStorage = new EUEnergyStorageImpl(8000, voltage);

        // 2 输入槽 + 1 输出槽 + 4 升级槽
        this.itemHandler = new ItemStacksResourceHandler(7) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
                if (index > OUTPUT_SLOT) cachedOverclockerCount = -1;
            }
        };
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) {
            return;
        }

        processUpgrades();

        boolean wasWorking = isWorking();

        ItemResource inputRes1 = itemHandler.getResource(INPUT_SLOT_1);
        ItemResource inputRes2 = itemHandler.getResource(INPUT_SLOT_2);
        int input1Amount = itemHandler.getAmountAsInt(INPUT_SLOT_1);
        int input2Amount = itemHandler.getAmountAsInt(INPUT_SLOT_2);

        if (!inputRes1.isEmpty() && input1Amount > 0 && !inputRes2.isEmpty() && input2Amount > 0) {
            // 仅在输入变化时重新查找配方
            if (!inputRes1.equals(cachedInput1) || !inputRes2.equals(cachedInput2)) {
                ItemStack stack1 = inputRes1.toStack(1);
                ItemStack stack2 = inputRes2.toStack(1);
                cachedInput1 = inputRes1;
                cachedInput2 = inputRes2;

                cachedRecipe = RecipeHelper.findMachineRecipe(level, "alloy_furnace", stack1, stack2);
                if (cachedRecipe != null) {
                    cachedOutput = cachedRecipe.getOutput().copy();
                } else {
                    // 尝试交换输入顺序查找
                    cachedRecipe = RecipeHelper.findMachineRecipe(level, "alloy_furnace", stack2, stack1);
                    if (cachedRecipe != null) {
                        cachedOutput = cachedRecipe.getOutput().copy();
                    } else {
                        cachedOutput = ItemStack.EMPTY;
                    }
                }
            }

            if (cachedRecipe != null && !cachedOutput.isEmpty()) {
                if (cachedOverclockerCount < 0) cachedOverclockerCount = item.OverclockerUpgradeItem.countOverclockers(this);
                int processTime = item.OverclockerUpgradeItem.getOverclockedProcessTime(cachedRecipe.getProcessTime(), cachedOverclockerCount);
                long energyPerTick = item.OverclockerUpgradeItem.getOverclockedEnergyConsumption(cachedRecipe.getEnergyPerTick(), cachedOverclockerCount);
                maxProgress = processTime;

                // 检查输入数量是否足够
                if (input1Amount < cachedRecipe.getInputCount() ||
                    input2Amount < cachedRecipe.getInput2Count()) {
                    if (wasWorking) {
                        updateRunningState();
                        setChanged();
                    }
                    return;
                }

                // 尝试消耗能量
                long consumed = energyStorage.removeEnergy(energyPerTick);
                if (consumed <= 0) {
                    if (wasWorking) {
                        updateRunningState();
                        setChanged();
                    }
                    return;
                }

                progress++;

                if (progress >= maxProgress) {
                    if (tryProcessOutput()) {
                        progress = 0;
                    }
                }
            } else {
                progress = 0;
                cachedOutput = ItemStack.EMPTY;
                cachedRecipe = null;
            }
        } else {
            progress = 0;
            cachedRecipe = null;
            cachedOutput = ItemStack.EMPTY;
            cachedInput1 = ItemResource.EMPTY;
            cachedInput2 = ItemResource.EMPTY;
        }

        if (wasWorking != isWorking()) {
            updateRunningState();
            setChanged();
        }
    }

    /**
     * 尝试处理输出
     */
    private boolean tryProcessOutput() {
        if (cachedRecipe == null || cachedOutput.isEmpty()) return false;

        int input1Count = cachedRecipe.getInputCount();
        int input2Count = cachedRecipe.getInput2Count();

        ItemResource inputRes1 = itemHandler.getResource(INPUT_SLOT_1);
        ItemResource inputRes2 = itemHandler.getResource(INPUT_SLOT_2);

        // 检查输入数量
        if (itemHandler.getAmountAsInt(INPUT_SLOT_1) < input1Count) return false;
        if (itemHandler.getAmountAsInt(INPUT_SLOT_2) < input2Count) return false;

        // 检查输出空间
        ItemResource outputRes = ItemResource.of(cachedOutput);
        int currentOutputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);
        if (!itemHandler.getResource(OUTPUT_SLOT).isEmpty() &&
            !itemHandler.getResource(OUTPUT_SLOT).equals(outputRes)) return false;
        if (currentOutputAmount + cachedOutput.getCount() > cachedOutput.getMaxStackSize()) return false;

        try (Transaction tx = Transaction.openRoot()) {
            // 消耗输入1
            int extracted1 = itemHandler.extract(INPUT_SLOT_1, inputRes1, input1Count, tx);
            if (extracted1 < input1Count) return false;

            // 消耗输入2
            int extracted2 = itemHandler.extract(INPUT_SLOT_2, inputRes2, input2Count, tx);
            if (extracted2 < input2Count) return false;

            // 插入输出
            int inserted = itemHandler.insert(OUTPUT_SLOT, outputRes, cachedOutput.getCount(), tx);
            if (inserted < cachedOutput.getCount()) return false;

            tx.commit();
        }

        return true;
    }

    private void updateRunningState() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(MachineBlock.RUNNING)) {
                boolean currentRunning = state.getValue(MachineBlock.RUNNING);
                boolean shouldRun = isWorking();
                if (currentRunning != shouldRun) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, shouldRun));
                }
            }
        }
    }

    /**
     * 向相邻电炉提供能量
     */
    public long provideEnergy(long maxTransfer) {
        long available = Math.min(energyStorage.getEnergyStored(), maxTransfer);
        if (available > 0) {
            energyStorage.removeEnergy(available);
            return available;
        }
        return 0;
    }

    @Override
    public String getMachineName() {
        return "感应炉";
    }

    private static final Component DISPLAY_NAME = Component.literal("合金炉");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InductionFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    public EUEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public int getUpgradeSlotCount() { return 4; }

    @Override
    public int getUpgradeSlotStart() { return OUTPUT_SLOT + 1; }

    @Override
    public int[] getInputSlots() { return new int[]{INPUT_SLOT_1, INPUT_SLOT_2}; }

    @Override
    public int[] getOutputSlots() { return new int[]{OUTPUT_SLOT}; }

    public EUVoltage getVoltage() {
        return voltage;
    }

    public boolean isWorking() {
        return progress > 0;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    @Override
    public float getProcessingProgress() {
        if (maxProgress <= 0 || !isWorking()) return 0f;
        return (float) progress / maxProgress;
    }

    @Override
    public boolean isProcessing() {
        return isWorking();
    }

    @Override
    public MachinePriority getMachinePriority() {
        return MachinePriority.HIGH;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        progress = input.getInt("progress").orElse(0);
        maxProgress = input.getInt("maxProgress").orElse(BASE_PROCESS_TIME);
        itemHandler.deserialize(input);
        // 重置缓存
        cachedRecipe = null;
        cachedOutput = ItemStack.EMPTY;
        cachedInput1 = ItemResource.EMPTY;
        cachedInput2 = ItemResource.EMPTY;
        cachedOverclockerCount = -1;
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }
}
