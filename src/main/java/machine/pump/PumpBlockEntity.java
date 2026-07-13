package machine.pump;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import fluid.FluidProvider;
import item.FluidEjectorUpgradeItem;
import machine.ore_washer.OreWasherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * 泵方块实体
 * <p>
 * 从相邻6面水源方块中自动吸收水，支持无限水源（2+相邻水源）。
 * 可通过水桶手动填充。
 * 吸收的水可输出到相邻的洗矿机等机器。
 * </p>
 * <p>
 * 配置：
 * - 电压等级：LV (32 EU/t)
 * - 内部缓冲：1000 EU
 * - 最大水存储：16000 mB (16桶)
 * - 吸收速率：1桶/500 ticks (25秒)
 * - 能量消耗：1 EU/t
 * </p>
 */
public class PumpBlockEntity extends MachineBlockEntity implements EnergyDisplay, FluidProvider {

    /** 最大存储水量（mB） */
    private static final int MAX_STORED_WATER = 16000;

    /** 主动吸收速率（mB/tick） */
    private static final int ACTIVE_ABSORB_RATE = 10;

    /** 升级槽数量 */
    private static final int UPGRADE_SLOTS = 4;

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final ItemStacksResourceHandler itemHandler;

    /** 内部存储的水量（mB） */
    private int storedWater = 0;
    
    /** 流体输入速率（mB/tick） */
    private static final int PASSIVE_ABSORB_RATE = 100;
    
    /** 无限水补充速率（mB/tick） */
    private static final int INFINITE_WATER_RATE = 10;

    /** 当前吸收周期已消耗的tick数 */
    private int absorbTicks = 0;

    /** 是否正在工作 */
    private boolean active = false;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state);
        this.voltage = EUVoltage.LV;
        this.energyStorage = new EUEnergyStorageImpl(1000, voltage);
        
        // 4 个升级槽（无输入/输出槽）
        this.itemHandler = new ItemStacksResourceHandler(UPGRADE_SLOTS) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        // 处理升级效果
        processUpgrades();

        boolean wasActive = active;
        active = false;

        // 水槽已满，不工作
        if (storedWater >= MAX_STORED_WATER) {
            if (wasActive) {
                updateRunningState();
                setChanged();
            }
            return;
        }

        // 检查相邻水源
        int adjacentSources = countAdjacentWaterSources();
        if (adjacentSources <= 0) {
            if (wasActive) {
                updateRunningState();
                setChanged();
            }
            return;
        }

        // 2+相邻水源方块 = 无限水源，不会消耗水源方块
        int adjacentSourceBlocks = countAdjacentWaterSourceBlocks();
        boolean isInfinite = adjacentSourceBlocks >= 2;

        // 消耗能量（1 EU/t）
        long consumed = energyStorage.removeEnergy(1);
        if (consumed <= 0) {
            if (wasActive) {
                updateRunningState();
                setChanged();
            }
            return;
        }

        active = true;
        absorbTicks++;

        // 无限水模式：瞬间吸收满（如果未满）
        if (isInfinite) {
            if (storedWater < MAX_STORED_WATER) {
                storedWater = MAX_STORED_WATER;
            }
        } else {
            // 非无限水模式：每次吸收一桶水（1000 mB）并消耗一个水方块
            storedWater = Math.min(storedWater + 1000, MAX_STORED_WATER);
            removeOneAdjacentWaterSource();
        }

        // 向相邻机器输出水（仅在没有流体输出升级时使用）
        if (!hasFluidEjectorUpgrade()) {
            outputWaterToAdjacent();
        }
        
        // 被动吸收水源到输入1格
        passiveAbsorbWater();
        
        // 无限水处理
        processInfiniteWater();

        if (wasActive != active) {
            updateRunningState();
        }
        setChanged();
    }

    /**
     * 统计相邻6面的水源方块数量
     */
    private int countAdjacentWaterSources() {
        if (level == null) return 0;
        int count = 0;
        for (Direction direction : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(direction);
            FluidState fluidState = level.getFluidState(adjPos);
            if (fluidState.is(Fluids.WATER)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 统计相邻6面的水源方块数量（仅水源）
     */
    private int countAdjacentWaterSourceBlocks() {
        if (level == null) return 0;
        int count = 0;
        for (Direction direction : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(direction);
            FluidState fluidState = level.getFluidState(adjPos);
            if (fluidState.is(Fluids.WATER) && fluidState.isSource()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 移除一个相邻的水方块（非无限水源模式）
     * 优先消耗水源方块，如果没有水源再消耗流动水
     */
    private void removeOneAdjacentWaterSource() {
        if (level == null) return;
        // 优先消耗水源方块
        for (Direction direction : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(direction);
            FluidState fluidState = level.getFluidState(adjPos);
            if (fluidState.is(Fluids.WATER) && fluidState.isSource()) {
                level.setBlock(adjPos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
        }
        // 如果没有水源，消耗流动水
        for (Direction direction : Direction.values()) {
            BlockPos adjPos = worldPosition.relative(direction);
            FluidState fluidState = level.getFluidState(adjPos);
            if (fluidState.is(Fluids.WATER)) {
                level.setBlock(adjPos, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
        }
    }

    /**
     * 向相邻机器输出水（持续输出，遍历所有方向）
     */
    private void outputWaterToAdjacent() {
        if (level == null || storedWater <= 0) return;
        
        for (Direction direction : Direction.values()) {
            if (storedWater <= 0) break;
            
            BlockPos adjPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(adjPos);
            
            // 向洗矿机输出（每次1000 mB）
            if (be instanceof OreWasherBlockEntity washer) {
                if (storedWater >= 1000 && washer.addWaterBucket()) {
                    storedWater -= 1000;
                }
            }
            // 向FluidProvider接口的机器输出
            else if (be instanceof FluidProvider provider) {
                int maxTransfer = Math.min(storedWater, 1000); // 每次最多传输1000 mB
                int transferred = provider.insertFluid(maxTransfer);
                if (transferred > 0) {
                    storedWater -= transferred;
                }
            }
        }
    }
    
    /**
     * 流体输入 - 从相邻的FluidProvider中提取流体
     */
    private void passiveAbsorbWater() {
        if (level == null || level.isClientSide()) return;
        
        // 从相邻的FluidProvider中提取流体
        for (Direction direction : Direction.values()) {
            if (storedWater >= MAX_STORED_WATER) break;
            
            BlockPos adjPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(adjPos);
            if (be instanceof FluidProvider provider) {
                // 计算需要提取的量
                int needed = MAX_STORED_WATER - storedWater;
                int extractAmount = Math.min(needed, PASSIVE_ABSORB_RATE);
                
                // 从提供者中提取流体
                int extracted = provider.extractFluid(extractAmount);
                if (extracted > 0) {
                    storedWater += extracted;
                    setChanged();
                }
            }
        }
    }
    
    /**
     * 无限水处理
     */
    private void processInfiniteWater() {
        if (level == null || level.isClientSide()) return;
        
        // 无限水条件：有2个或更多相邻水源方块
        int adjacentSourceBlocks = countAdjacentWaterSourceBlocks();
        if (adjacentSourceBlocks >= 2) {
            // 无限水源，每tick自动补充
            if (storedWater + INFINITE_WATER_RATE <= MAX_STORED_WATER) {
                storedWater += INFINITE_WATER_RATE;
            }
        }
    }

    private void updateRunningState() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(MachineBlock.RUNNING)) {
                boolean currentRunning = state.getValue(MachineBlock.RUNNING);
                if (currentRunning != active) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, active));
                }
            }
        }
    }

    /**
     * 尝试添加一桶水（水桶交互）
     *
     * @return 是否成功添加
     */
    public boolean addWaterBucket() {
        if (storedWater + 1000 > MAX_STORED_WATER) {
            return false;
        }
        storedWater += 1000;
        setChanged();
        return true;
    }

    /** 获取当前存储的水量（mB） */
    public int getStoredWater() { return storedWater; }

    /** 获取最大存储水量（mB） */
    public int getMaxStoredWater() { return MAX_STORED_WATER; }

    /** 是否正在工作 */
    public boolean isActive() { return active; }

    @Override
    public String getMachineName() { return "泵"; }

    public EUEnergyStorage getEnergyStorage() { return energyStorage; }

    public EUVoltage getVoltage() { return voltage; }

    @Override
    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public int getUpgradeSlotCount() {
        return UPGRADE_SLOTS;
    }

    @Override
    public int getUpgradeSlotStart() {
        return 0; // 泵没有输入/输出槽，升级槽从0开始
    }

    /**
     * 检查是否安装了流体输出升级
     */
    private boolean hasFluidEjectorUpgrade() {
        if (itemHandler == null) return false;
        int upgradeSlotCount = getUpgradeSlotCount();
        int upgradeSlotStart = getUpgradeSlotStart();
        for (int i = 0; i < upgradeSlotCount; i++) {
            int slotIndex = upgradeSlotStart + i;
            net.minecraft.world.item.Item item = itemHandler.getResource(slotIndex).getItem();
            if (item instanceof FluidEjectorUpgradeItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canReceiveEU() { return true; }

    @Override
    protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("storedWater", storedWater);
        output.putInt("absorbTicks", absorbTicks);
        output.putBoolean("active", active);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        storedWater = input.getInt("storedWater").orElse(0);
        absorbTicks = input.getInt("absorbTicks").orElse(0);
        active = input.getBooleanOr("active", false);
        itemHandler.deserialize(input);
    }
    
    /**
     * 实现FluidProvider接口
     * 尝试从提供者中提取指定数量的流体
     * 
     * @param maxAmount 最大提取量（mB）
     * @return 实际提取的流体量（mB）
     */
    @Override
    public int extractFluid(int maxAmount) {
        int extractAmount = Math.min(maxAmount, storedWater);
        if (extractAmount > 0) {
            storedWater -= extractAmount;
            setChanged();
        }
        return extractAmount;
    }
    
    /**
     * 实现FluidProvider接口
     * 尝试向提供者中输入指定数量的流体
     * 
     * @param maxAmount 最大输入量（mB）
     * @return 实际输入的流体量（mB）
     */
    @Override
    public int insertFluid(int maxAmount) {
        int insertAmount = Math.min(maxAmount, MAX_STORED_WATER - storedWater);
        if (insertAmount > 0) {
            storedWater += insertAmount;
            setChanged();
        }
        return insertAmount;
    }
    
    /**
     * 实现FluidProvider接口
     * 获取当前存储的流体量（mB）
     */
    @Override
    public int getStoredFluid() {
        return storedWater;
    }
    
    /**
     * 实现FluidProvider接口
     * 获取最大存储流体量（mB）
     */
    @Override
    public int getMaxStoredFluid() {
        return MAX_STORED_WATER;
    }

    private static final ItemStack WATER_BUCKET_STACK = new ItemStack(Items.WATER_BUCKET);

    @Override
    public ItemStack getFluidItem() {
        return storedWater > 0 ? WATER_BUCKET_STACK : ItemStack.EMPTY;
    }
}
