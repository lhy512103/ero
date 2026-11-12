package machine.geothermal_generator;

import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import fluid.FluidProvider;
import fluid.MachineTank;
import net.minecraft.world.level.material.Fluids;
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
public class GeothermalGeneratorBlockEntity extends MachineBlockEntity implements EnergyDisplay, FluidProvider {

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
    /** 最近一次检查时是否紧邻熔岩 */
    private boolean nearLava = false;

    /** 内部熔岩储罐（mB） */
    private final MachineTank tank = MachineTank.of(Fluids.LAVA, MAX_STORED_LAVA * 1000, this::setChanged);

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
        if (hasFuel() && !energyStorage.isFull()) {
            active = true;
            energyStorage.addEnergy(generationRate);

            if (lavaTickRemaining <= 0 && tank.drain(1000) == 1000) lavaTickRemaining = TICKS_PER_BUCKET;
            lavaTickRemaining--;
            setChanged();
        } else if (!hasFuel()) {
            // 内部流体槽为空时，检测相邻岩浆源
            // 每秒检查一次周围是否有熔岩；烧完储罐后不会靠上一次的状态继续空转
            if (tickCount % 20 == 0) nearLava = isAdjacentToLava();
            active = nearLava;
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

    /** 是否还有可烧的熔岩：正在烧的那一桶，或储罐里至少一整桶 */
    private boolean hasFuel() { return lavaTickRemaining > 0 || tank.getAmount() >= 1000; }

    @Override public MachineTank getTank() { return tank; }
    @Override public net.minecraft.world.level.material.Fluid getTankFluid() { return Fluids.LAVA; }

    /** 尝试添加一桶熔岩，返回是否成功 */
    public boolean addLavaBucket() { return addBucket(); }

    /** 储罐里的熔岩桶数，正在烧的那一桶也算在内 */
    public int getStoredLava() { return tank.getAmount() / 1000 + (lavaTickRemaining > 0 ? 1 : 0); }

    public int getMaxStoredLava() { return MAX_STORED_LAVA; }

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
        tank.save(output, "tank");
        output.putInt("lavaTickRemaining", lavaTickRemaining);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        active = input.getBooleanOr("active", false);
        lavaTickRemaining = input.getIntOr("lavaTickRemaining", 0);
        // 旧存档按「桶数」记录，且正在烧的那一桶仍计在内
        boolean legacy = input.child("tank").isEmpty();
        tank.load(input, "tank", "storedLava", Fluids.LAVA, 1000);
        if (legacy && lavaTickRemaining > 0) tank.drain(1000);
    }

    @Override protected boolean canReceiveEU() { return false; }
    @Override protected boolean canExtractEU() { return true; }
}
