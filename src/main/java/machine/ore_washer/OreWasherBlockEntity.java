package machine.ore_washer;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import fluid.FluidProvider;
import item.OverclockerUpgradeItem;
import machine.OreWasherGuiProvider;
import machine.ore_washer.menu.OreWasherMenu;
import recipe.MachineRecipe;
import recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/**
 * 洗矿机方块实体
 * <p>
 * 默认配置：
 * - 电压等级：LV (32 EU/t)
 * - 内部缓冲：2000 EU
 * - 能量消耗：2 EU/t
 * - 基础加工时间：200 tick
 * - 双输入槽：输入槽1（矿石）、输入槽2（可选辅助材料）
 * - 输出槽：纯净矿石
 * </p>
 */
public class OreWasherBlockEntity extends MachineBlockEntity implements EnergyDisplay, ProcessingDisplay, OreWasherGuiProvider, FluidProvider {

    private static final int INPUT1_SLOT = 0;
    private static final int INPUT2_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int EXTRA_OUTPUT_SLOT = 3;
    private static final long BASE_ENERGY_CONSUMPTION = 2;
    private static final int BASE_PROCESS_TIME = 200;

    /** 最大存储水量（桶数） */
    private static final int MAX_STORED_WATER = 16;

    /** 每次加工消耗的水量（mB） */
    private static final int WATER_PER_OPERATION = 1000;

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final ItemStacksResourceHandler itemHandler;
    private int progress = 0;
    private int maxProgress = BASE_PROCESS_TIME;

    /** 内部存储的水量（mB） */
    private int storedWater = 0;

    @Nullable
    private MachineRecipe cachedRecipe;
    private ItemStack cachedOutput = ItemStack.EMPTY;
    private ItemResource cachedInput1Resource = ItemResource.EMPTY;
    private ItemResource cachedInput2Resource = ItemResource.EMPTY;
    private int cachedOverclockerCount = -1;

    public OreWasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_WASHER.get(), pos, state);
        this.voltage = EUVoltage.LV;
        this.energyStorage = new EUEnergyStorageImpl(2000, voltage);

        // 2 输入槽 + 1 输出槽 + 1 副产槽 + 4 升级槽
        this.itemHandler = new ItemStacksResourceHandler(8) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
                if (index >= 4) cachedOverclockerCount = -1;
            }
        };
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) {
            return;
        }

        // 处理升级效果
        processUpgrades();

        boolean wasWorking = isWorking();

        ItemResource input1Resource = itemHandler.getResource(INPUT1_SLOT);
        int input1Amount = itemHandler.getAmountAsInt(INPUT1_SLOT);

        if (!input1Resource.isEmpty() && input1Amount > 0) {
            ItemResource input2Resource = itemHandler.getResource(INPUT2_SLOT);
            ItemStack input1Stack = input1Resource.toStack(1);
            ItemStack input2Stack = input2Resource.isEmpty() ? ItemStack.EMPTY : input2Resource.toStack(1);

            // 仅在输入变化时重新查找配方
            if (!input1Resource.equals(cachedInput1Resource) || !input2Resource.equals(cachedInput2Resource)) {
                cachedInput1Resource = input1Resource;
                cachedInput2Resource = input2Resource;

                if (input2Resource.isEmpty()) {
                    cachedRecipe = RecipeHelper.findMachineRecipe(level, "ore_washer", input1Stack);
                } else {
                    cachedRecipe = RecipeHelper.findMachineRecipe(level, "ore_washer", input1Stack, input2Stack);
                }
                cachedOutput = cachedRecipe != null ? cachedRecipe.getOutput().copy() : ItemStack.EMPTY;
            }

            if (!cachedOutput.isEmpty() && cachedRecipe != null) {
                // 使用缓存的超频器数量
                if (cachedOverclockerCount < 0) {
                    cachedOverclockerCount = OverclockerUpgradeItem.countOverclockers(this);
                }

                int processTime = OverclockerUpgradeItem.getOverclockedProcessTime(
                    cachedRecipe.getProcessTime(), cachedOverclockerCount);
                long energyPerTick = OverclockerUpgradeItem.getOverclockedEnergyConsumption(
                    cachedRecipe.getEnergyPerTick(), cachedOverclockerCount);
                maxProgress = processTime;

                // 检查水量是否足够
                if (storedWater < WATER_PER_OPERATION && progress == 0) {
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
                        storedWater -= WATER_PER_OPERATION;
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
            cachedInput1Resource = ItemResource.EMPTY;
            cachedInput2Resource = ItemResource.EMPTY;
        }

        if (wasWorking != isWorking()) {
            updateRunningState();
            setChanged();
        }
    }

    private boolean tryProcessOutput() {
        if (cachedOutput.isEmpty() || cachedRecipe == null) return false;

        ItemStack outputStack = cachedOutput;
        ItemResource outputResource = ItemResource.of(outputStack);

        try (Transaction tx = Transaction.openRoot()) {
            // 检查输出是否可接收
            ItemResource currentOutputRes = itemHandler.getResource(OUTPUT_SLOT);
            int currentOutputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);
            if (!currentOutputRes.isEmpty() && !currentOutputRes.equals(outputResource)) return false;
            if (currentOutputAmount + outputStack.getCount() > outputStack.getMaxStackSize()) return false;

            // 消耗输入1
            ItemResource input1Resource = itemHandler.getResource(INPUT1_SLOT);
            int extracted1 = itemHandler.extract(INPUT1_SLOT, input1Resource, cachedRecipe.getInputCount(), tx);
            if (extracted1 < cachedRecipe.getInputCount()) return false;

            // 消耗输入2（如果有）
            if (cachedRecipe.hasDualInput()) {
                ItemResource input2Resource = itemHandler.getResource(INPUT2_SLOT);
                int extracted2 = itemHandler.extract(INPUT2_SLOT, input2Resource, cachedRecipe.getInput2Count(), tx);
                if (extracted2 < cachedRecipe.getInput2Count()) return false;
            }

            // 插入输出
            int inserted = itemHandler.insert(OUTPUT_SLOT, outputResource, outputStack.getCount(), tx);
            if (inserted <= 0) return false;

            // 副产物（概率产出，副产槽满则丢弃）
            if (cachedRecipe.hasExtraResults() && level != null) {
                for (MachineRecipe.ExtraResult extra : cachedRecipe.getExtraResults()) {
                    if (level.getRandom().nextFloat() >= extra.chance()) continue;
                    ItemStack extraStack = extra.create();
                    itemHandler.insert(EXTRA_OUTPUT_SLOT, ItemResource.of(extraStack), extraStack.getCount(), tx);
                }
            }

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

    @Override
    public String getMachineName() { return "洗矿机"; }

    private static final Component DISPLAY_NAME = Component.literal("洗矿机");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OreWasherMenu(containerId, playerInventory, this);
    }

    public EUEnergyStorage getEnergyStorage() { return energyStorage; }

    public EUVoltage getVoltage() { return voltage; }

    public ItemStacksResourceHandler getItemHandler() { return itemHandler; }

    public boolean isWorking() { return progress > 0; }

    public int getProgress() { return progress; }

    public int getMaxProgress() { return maxProgress; }

    // ProcessingDisplay 接口
    @Override
    public boolean isProcessing() { return progress > 0; }

    @Override
    public float getProcessingProgress() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }

    /**
     * 尝试添加一桶水到内部流体槽
     * @return 是否成功添加
     */
    public boolean addWaterBucket() {
        if (storedWater + 1000 > MAX_STORED_WATER * 1000) {
            return false;
        }
        storedWater += 1000;
        setChanged();
        return true;
    }

    /** 获取当前存储的水量（mB） */
    public int getStoredWater() { return storedWater; }

    /** 获取最大存储水量（mB） */
    public int getMaxStoredWater() { return MAX_STORED_WATER * 1000; }

    @Override
    public int getUpgradeSlotCount() { return 4; }

    @Override
    public int getUpgradeSlotStart() { return 4; } // 2 input + 1 output + 1 extra output

    // FluidProvider 接口实现
    @Override
    public int extractFluid(int maxAmount) {
        int extracted = Math.min(maxAmount, storedWater);
        storedWater -= extracted;
        setChanged();
        return extracted;
    }

    @Override
    public int insertFluid(int maxAmount) {
        int canInsert = Math.min(maxAmount, MAX_STORED_WATER * 1000 - storedWater);
        storedWater += canInsert;
        setChanged();
        return canInsert;
    }

    @Override
    public int getStoredFluid() {
        return storedWater;
    }

    @Override
    public int getMaxStoredFluid() {
        return MAX_STORED_WATER * 1000;
    }

    private static final ItemStack WATER_BUCKET_STACK = new ItemStack(Items.WATER_BUCKET);

    @Override
    public ItemStack getFluidItem() {
        return storedWater > 0 ? WATER_BUCKET_STACK : ItemStack.EMPTY;
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        output.putInt("storedWater", storedWater);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        progress = input.getInt("progress").orElse(0);
        maxProgress = input.getInt("maxProgress").orElse(BASE_PROCESS_TIME);
        storedWater = input.getInt("storedWater").orElse(0);
        itemHandler.deserialize(input);
        // 重置缓存
        cachedRecipe = null;
        cachedOutput = ItemStack.EMPTY;
        cachedInput1Resource = ItemResource.EMPTY;
        cachedInput2Resource = ItemResource.EMPTY;
        cachedOverclockerCount = -1;
    }
}
