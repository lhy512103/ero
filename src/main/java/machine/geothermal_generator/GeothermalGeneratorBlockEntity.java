package machine.geothermal_generator;

import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import network.performance.MachinePriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 地热发电机方块实体
 * <p>
 * 利用岩浆热量发电的机器。
 * 支持两种岩浆来源：
 * 1. SHIFT+右键使用熔岩桶填充内部流体槽（1-64桶容量）
 * 2. 放置在岩浆源旁边时自动发电
 * <p>
 * 默认配置：
 * - 电压等级：LV (32 EU/t)
 * - 发电速率：20 EU/t
 * - 内部缓冲：2000 EU
 * - 每桶岩浆持续时间：1000 ticks (50秒)
 * </p>
 */
public class GeothermalGeneratorBlockEntity extends MachineBlockEntity implements EnergyDisplay {

    /** 每桶岩浆持续的tick数 */
    private static final int TICKS_PER_BUCKET = 1000;

    /** 最大存储桶数 */
    private static final int MAX_STORED_LAVA = 64;

    /** 能量存储 */
    private final EUEnergyStorageImpl energyStorage;

    /** 发电速率 (EU/t) */
    private final long generationRate;

    /** 发电电压等级 */
    private final EUVoltage voltage;

    /** 是否正在发电 */
    private boolean active = false;

    /** 内部存储的岩浆桶数 (0-64) */
    private int storedLava = 0;

    /** 当前桶岩浆剩余的tick数 */
    private int lavaTickRemaining = 0;

    /**
     * 创建地热发电机方块实体
     *
     * @param pos   方块位置
     * @param state 方块状态
     */
    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, EUVoltage.LV, 20, 2000);
    }

    /**
     * 创建地热发电机方块实体（自定义配置）
     *
     * @param pos            方块位置
     * @param state          方块状态
     * @param voltage        电压等级
     * @param generationRate 发电速率 (EU/t)
     * @param bufferCapacity 内部缓冲容量 (EU)
     */
    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state, EUVoltage voltage,
                                          long generationRate, long bufferCapacity) {
        super(ModBlockEntities.GEOTHERMAL_GENERATOR.get(), pos, state);
        this.voltage = voltage;
        this.generationRate = generationRate;
        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        boolean wasActive = active;

        // 优先从内部流体槽消耗岩浆发电
        if (storedLava > 0 && !energyStorage.isFull()) {
            active = true;
            energyStorage.addEnergy(generationRate);

            lavaTickRemaining--;
            if (lavaTickRemaining <= 0) {
                storedLava--;
                if (storedLava > 0) {
                    lavaTickRemaining = TICKS_PER_BUCKET;
                }
            }
            setChanged();
        } else if (storedLava <= 0) {
            // 内部流体槽为空时，检测相邻岩浆源
            if (tickCount % 20 == 0) {
                active = isAdjacentToLava();
            }
            if (active && !energyStorage.isFull()) {
                energyStorage.addEnergy(generationRate);
                setChanged();
            }
        } else {
            active = false;
        }

        if (wasActive != active) {
            setChanged();
        }
    }

    /**
     * 检查是否紧邻岩浆源
     */
    private boolean isAdjacentToLava() {
        if (level == null) return false;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(direction)).getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试添加一桶岩浆到内部流体槽
     *
     * @return 是否成功添加
     */
    public boolean addLavaBucket() {
        if (storedLava >= MAX_STORED_LAVA) {
            return false;
        }
        storedLava++;
        if (lavaTickRemaining <= 0) {
            lavaTickRemaining = TICKS_PER_BUCKET;
        }
        setChanged();
        return true;
    }

    /**
     * 获取当前存储的岩浆桶数
     */
    public int getStoredLava() {
        return storedLava;
    }

    /**
     * 获取最大存储桶数
     */
    public int getMaxStoredLava() {
        return MAX_STORED_LAVA;
    }

    @Override
    public String getMachineName() {
        return "地热发电机";
    }

    public EUEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public EUVoltage getVoltage() {
        return voltage;
    }

    public long getGenerationRate() {
        return generationRate;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public MachinePriority getMachinePriority() {
        return MachinePriority.CRITICAL;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putBoolean("active", active);
        output.putInt("storedLava", storedLava);
        output.putInt("lavaTickRemaining", lavaTickRemaining);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        active = input.getBooleanOr("active", false);
        storedLava = input.getIntOr("storedLava", 0);
        lavaTickRemaining = input.getIntOr("lavaTickRemaining", 0);
    }

    @Override protected boolean canReceiveEU() { return false; }
    @Override protected boolean canExtractEU() { return true; }
}
