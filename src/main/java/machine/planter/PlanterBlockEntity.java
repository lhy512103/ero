package machine.planter;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import item.OverclockerUpgradeItem;
import machine.planter.menu.PlanterMenu;
import recipe.MachineRecipe;
import recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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
import org.jetbrains.annotations.Nullable;

public class PlanterBlockEntity extends MachineBlockEntity implements EnergyDisplay, ProcessingDisplay, MenuProvider {

    // /** 时间模式（测试模式，暂未启用） */
    // public enum TimeMode {
    //     NORMAL,       // 正常模式（根据游戏时间变化）
    //     ETERNAL_DAY,  // 永昼（固定正午 3x速度, 1x能耗）
    //     ETERNAL_NIGHT // 永夜（固定夜晚 1x速度, 2x能耗）
    // }

    private static final int CULTIVATION_SLOT = 0;
    private static final int SOIL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final long BASE_ENERGY_CONSUMPTION = 2;
    private static final int BASE_PROCESS_TIME = 200;
    // private static final long ETERNAL_DAY_ENERGY_COST = 2_000_000;   // 永昼切换能耗
    // private static final long ETERNAL_NIGHT_ENERGY_COST = 500_000;   // 永夜切换能耗

    // 时间阶段倍率
    private static final float PHASE_DAWN_MULT = 1.5f;      // 凌晨 0-2000: 1.5x速度
    private static final float PHASE_MORNING_MULT = 2.0f;    // 清晨 2000-6000: 2x速度
    private static final float PHASE_NOON_MULT = 3.0f;       // 正午 6000-8000: 3x速度
    private static final float PHASE_AFTERNOON_MULT = 2.0f;  // 下午 8000-12000: 2x速度
    private static final float PHASE_NIGHT_MULT = 1.0f;      // 夜晚 12000-24000: 1x速度(基准)
    private static final long NIGHT_ENERGY_MULT = 2;         // 夜晚能耗倍率

    // private TimeMode timeMode = TimeMode.NORMAL;

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final ItemStacksResourceHandler itemHandler;
    private int progress = 0;
    private int maxProgress = BASE_PROCESS_TIME;

    @Nullable
    private MachineRecipe cachedRecipe;
    private ItemStack cachedOutput = ItemStack.EMPTY;
    private ItemResource cachedInput1Resource = ItemResource.EMPTY;
    private ItemResource cachedInput2Resource = ItemResource.EMPTY;
    private int cachedOverclockerCount = -1;

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLANTER.get(), pos, state);
        this.voltage = EUVoltage.LV;
        this.energyStorage = new EUEnergyStorageImpl(1000, voltage);

        // 1 培育槽 + 1 土壤槽 + 1 输出槽 + 4 升级槽
        this.itemHandler = new ItemStacksResourceHandler(7) {
            @Override
            protected void onContentsChanged(int index, net.minecraft.world.item.ItemStack previousContents) {
                setChanged();
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

        ItemResource input1Resource = itemHandler.getResource(CULTIVATION_SLOT);
        int input1Amount = itemHandler.getAmountAsInt(CULTIVATION_SLOT);

        if (!input1Resource.isEmpty() && input1Amount > 0) {
            ItemResource input2Resource = itemHandler.getResource(SOIL_SLOT);
            ItemStack input1Stack = input1Resource.toStack(1);
            ItemStack input2Stack = input2Resource.isEmpty() ? ItemStack.EMPTY : input2Resource.toStack(1);

            // 仅在输入变化时重新查找配方
            if (!input1Resource.equals(cachedInput1Resource) || !input2Resource.equals(cachedInput2Resource)) {
                cachedInput1Resource = input1Resource;
                cachedInput2Resource = input2Resource;

                if (input2Resource.isEmpty()) {
                    cachedRecipe = RecipeHelper.findMachineRecipe(level, "planter", input1Stack);
                } else {
                    cachedRecipe = RecipeHelper.findMachineRecipe(level, "planter", input1Stack, input2Stack);
                }
                cachedOutput = cachedRecipe != null ? cachedRecipe.getOutput().copy() : ItemStack.EMPTY;
            }

            if (!cachedOutput.isEmpty() && cachedRecipe != null) {
                // 使用缓存的超频器数量
                if (cachedOverclockerCount < 0) {
                    cachedOverclockerCount = OverclockerUpgradeItem.countOverclockers(this);
                }

                int baseProcessTime = OverclockerUpgradeItem.getOverclockedProcessTime(
                    cachedRecipe.getProcessTime(), cachedOverclockerCount);
                long baseEnergyPerTick = OverclockerUpgradeItem.getOverclockedEnergyConsumption(
                    cachedRecipe.getEnergyPerTick(), cachedOverclockerCount);

                // 时间阶段加成
                float speedMult = getTimeSpeedMultiplier();
                long energyMult = getTimeEnergyMultiplier();
                int processTime = Math.max(1, (int) (baseProcessTime / speedMult));
                long energyPerTick = baseEnergyPerTick * energyMult;
                maxProgress = processTime;

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

            // 消耗培育槽输入
            ItemResource input1Resource = itemHandler.getResource(CULTIVATION_SLOT);
            int extracted1 = itemHandler.extract(CULTIVATION_SLOT, input1Resource, cachedRecipe.getInputCount(), tx);
            if (extracted1 < cachedRecipe.getInputCount()) return false;

            // 消耗土壤槽输入（如果有）
            if (cachedRecipe.hasDualInput()) {
                ItemResource input2Resource = itemHandler.getResource(SOIL_SLOT);
                int extracted2 = itemHandler.extract(SOIL_SLOT, input2Resource, cachedRecipe.getInput2Count(), tx);
                if (extracted2 < cachedRecipe.getInput2Count()) return false;
            }

            // 插入输出
            int inserted = itemHandler.insert(OUTPUT_SLOT, outputResource, outputStack.getCount(), tx);
            if (inserted <= 0) return false;

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

    // /** 方块移除时恢复昼夜循环（测试模式） */
    // @Override
    // public void onRemove() {
    //     super.onRemove();
    //     if (timeMode != TimeMode.NORMAL && level != null && !level.isClientSide() && level.getServer() != null) {
    //         var source = level.getServer().createCommandSourceStack()
    //             .withLevel((net.minecraft.server.level.ServerLevel) level);
    //         level.getServer().getCommands().performPrefixedCommand(source, "gamerule doDaylightCycle true");
    //     }
    // }

    @Override
    public String getMachineName() { return "种植机"; }

    private static final Component DISPLAY_NAME = Component.literal("种植机");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PlanterMenu(containerId, playerInventory, this);
    }

    @Override
    public ItemStacksResourceHandler getItemHandler() { return itemHandler; }

    @Override
    public int getUpgradeSlotCount() { return 4; }

    @Override
    public int getUpgradeSlotStart() { return 3; }

    public EUEnergyStorage getEnergyStorage() { return energyStorage; }

    public EUVoltage getVoltage() { return voltage; }

    public boolean isWorking() { return progress > 0; }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }

    /** 获取当前时间阶段名称 */
    public String getPhaseName() {
        int time = getClockTime();
        if (time < 2000) return "凌晨";
        if (time < 6000) return "清晨";
        if (time < 8000) return "正午";
        if (time < 12000) return "下午";
        return "夜晚";
    }

    // /** 切换时间模式：正常 → 永昼 → 永夜 → 正常（测试模式，需要消耗能量） */
    // public void cycleMode() {
    //     TimeMode next = switch (timeMode) {
    //         case NORMAL -> TimeMode.ETERNAL_DAY;
    //         case ETERNAL_DAY -> TimeMode.ETERNAL_NIGHT;
    //         case ETERNAL_NIGHT -> TimeMode.NORMAL;
    //     };
    //     if (next == TimeMode.ETERNAL_DAY) {
    //         if (energyStorage.getEnergyStored() < ETERNAL_DAY_ENERGY_COST) return;
    //         energyStorage.removeEnergy(ETERNAL_DAY_ENERGY_COST);
    //     } else if (next == TimeMode.ETERNAL_NIGHT) {
    //         if (energyStorage.getEnergyStored() < ETERNAL_NIGHT_ENERGY_COST) return;
    //         energyStorage.removeEnergy(ETERNAL_NIGHT_ENERGY_COST);
    //     }
    //     timeMode = next;
    //     applyTimeMode();
    //     setChanged();
    // }

    // /** 将当前时间模式应用到当前维度（测试模式） */
    // private void applyTimeMode() {
    //     if (level == null || level.isClientSide() || level.getServer() == null) return;
    //     var source = level.getServer().createCommandSourceStack().withLevel((net.minecraft.server.level.ServerLevel) level);
    //     var commands = level.getServer().getCommands();
    //     switch (timeMode) {
    //         case ETERNAL_DAY -> {
    //             commands.performPrefixedCommand(source, "gamerule doDaylightCycle false");
    //             commands.performPrefixedCommand(source, "time set 6000");
    //         }
    //         case ETERNAL_NIGHT -> {
    //             commands.performPrefixedCommand(source, "gamerule doDaylightCycle false");
    //             commands.performPrefixedCommand(source, "time set 18000");
    //         }
    //         case NORMAL -> {
    //             commands.performPrefixedCommand(source, "gamerule doDaylightCycle true");
    //         }
    //     }
    // }

    // public TimeMode getTimeMode() { return timeMode; }

    /** 获取当前速度倍率（整数表示，用于同步） */
    public int getSpeedMultiplierPercent() {
        return (int) (getTimeSpeedMultiplier() * 100);
    }

    // ProcessingDisplay 接口
    @Override
    public boolean isProcessing() { return progress > 0; }

    @Override
    public float getProcessingProgress() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }

    @Override
    public String getProgressLabel() {
        return "培育进度";
    }

    /**
     * 检测当前是否为白天（清晨、正午、中午）
     * 使用与太阳能发电机相同的检测逻辑
     */
    public boolean isDaytime() {
        if (level == null) return false;
        return level.getOverworldClockTime() % 24000 < 12000;
    }

    /**
     * 获取当前游戏日时刻（0-23999）
     */
    public int getClockTime() {
        if (level == null) return 0;
        return (int) (level.getOverworldClockTime() % 24000);
    }

    /**
     * 获取当前时间阶段的速度倍率
     * 凌晨 1.5x, 清晨 2x, 正午 3x, 下午 2x, 夜晚 1x
     */
    private float getTimeSpeedMultiplier() {
        int time = getClockTime();
        if (time < 2000) return PHASE_DAWN_MULT;
        if (time < 6000) return PHASE_MORNING_MULT;
        if (time < 8000) return PHASE_NOON_MULT;
        if (time < 12000) return PHASE_AFTERNOON_MULT;
        return PHASE_NIGHT_MULT;
    }

    /**
     * 获取当前时间阶段的能耗倍率
     * 夜晚 2x, 其他时段 1x
     */
    private long getTimeEnergyMultiplier() {
        int time = getClockTime();
        return time >= 12000 ? NIGHT_ENERGY_MULT : 1;
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        // output.putString("timeMode", timeMode.name());
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        progress = input.getInt("progress").orElse(0);
        maxProgress = input.getInt("maxProgress").orElse(BASE_PROCESS_TIME);
        // input.getString("timeMode").ifPresent(s -> {
        //     try { timeMode = TimeMode.valueOf(s); } catch (IllegalArgumentException ignored) {}
        // });
        itemHandler.deserialize(input);
        // 重置缓存
        cachedRecipe = null;
        cachedOutput = ItemStack.EMPTY;
        cachedInput1Resource = ItemResource.EMPTY;
        cachedInput2Resource = ItemResource.EMPTY;
        cachedOverclockerCount = -1;
    }
}
