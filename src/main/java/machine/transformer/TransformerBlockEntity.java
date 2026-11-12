package machine.transformer;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import energy.EUCapability;
import energy.EUEnergyCapabilityAdapter;
import energy.EUEnergyStorage;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * 变压器：接收高于输出等级的电压，按选定的输出等级向下游供电（1:1，无损耗）。
 * <p>
 * 每 tick 吞吐量为输出电压的 4 倍（一路高压拆成四路低压）。带电时，周围
 * {@link #SHOCK_RADIUS} 格内处于水中或淋雨的生物会被电击，伤害与停滞时间随电压等级提高。
 * </p>
 */
public class TransformerBlockEntity extends MachineBlockEntity implements EnergyDisplay {

    /** 可选的输出电压等级，右键方块循环切换 */
    public static final EUVoltage[] OUTPUT_TIERS = {EUVoltage.LV, EUVoltage.MV, EUVoltage.HV, EUVoltage.EV};
    public static final int SHOCK_RADIUS = 10;
    /** 两次电击之间的间隔 (tick) */
    public static final int SHOCK_INTERVAL = 40;
    private static final int ACTIVE_TICKS = 40;
    private static final int INPUT_MEMORY_TICKS = 100;

    public static final ResourceKey<DamageType> ELECTRIC_DAMAGE =
        ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("iuce", "electric"));

    private int tierIndex = 0;
    private long energy = 0;
    /** 最近收到的最高输入电压（决定电击强度），一段时间没有输入后清空 */
    @Nullable
    private EUVoltage inputVoltage;
    private int inputMemory = 0;
    private int activeTicks = 0;

    private final EUEnergyStorage storage = new EUEnergyStorage() {
        @Override public long receiveEnergy(long maxReceive, boolean simulate) {
            long accepted = Math.max(0, Math.min(maxReceive, getMaxEnergyStored() - energy));
            if (!simulate && accepted > 0) { energy += accepted; setChanged(); }
            return accepted;
        }
        @Override public long extractEnergy(long maxExtract, boolean simulate) {
            long removed = Math.max(0, Math.min(maxExtract, energy));
            if (!simulate && removed > 0) { energy -= removed; setChanged(); }
            return removed;
        }
        @Override public long getEnergyStored() { return energy; }
        @Override public long getMaxEnergyStored() { return getRate() * 20; }
        @Override public EUVoltage getVoltage() { return getOutputVoltage(); }
        @Override public long getMaxTransferRate() { return getRate(); }
    };

    private final EUCapability capability = new EUEnergyCapabilityAdapter(storage, EUVoltage.LV) {
        @Override public EUVoltage getVoltage() { return getOutputVoltage(); }
    };

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRANSFORMER.get(), pos, state);
    }

    public EUVoltage getOutputVoltage() { return OUTPUT_TIERS[tierIndex]; }

    /** 每 tick 可接收 / 输出的电量上限 */
    public long getRate() { return getOutputVoltage().getMaxVoltage() * 4; }

    /** 切换到下一个输出电压等级 */
    public EUVoltage cycleOutput() {
        tierIndex = (tierIndex + 1) % OUTPUT_TIERS.length;
        setChanged();
        return getOutputVoltage();
    }

    public void setOutputVoltage(EUVoltage voltage) {
        for (int i = 0; i < OUTPUT_TIERS.length; i++) {
            if (OUTPUT_TIERS[i] == voltage) { tierIndex = i; setChanged(); return; }
        }
        throw new IllegalArgumentException("Unsupported transformer output: " + voltage);
    }

    /** 由能量传输管理器调用：记录输入侧电压 */
    public void noteInput(EUVoltage voltage) {
        if (inputVoltage == null || voltage.isHigherThan(inputVoltage)) inputVoltage = voltage;
        inputMemory = INPUT_MEMORY_TICKS;
        activeTicks = ACTIVE_TICKS;
    }

    /** 由能量传输管理器调用：标记正在输出 */
    public void markActive() { activeTicks = ACTIVE_TICKS; }

    /** 电击强度取输入侧与输出侧中较高的电压 */
    public EUVoltage getHazardVoltage() {
        EUVoltage output = getOutputVoltage();
        return inputVoltage != null && inputVoltage.isHigherThan(output) ? inputVoltage : output;
    }

    public static float shockDamage(EUVoltage voltage) { return Math.max(1, voltage.getIndex()); }

    public static int stunTicks(EUVoltage voltage) { return 20 + 10 * Math.max(1, voltage.getIndex()); }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.tickRateManager().runsNormally()) return;

        if (inputMemory > 0 && --inputMemory == 0) inputVoltage = null;
        if (activeTicks > 0) activeTicks--;

        BlockState state = getBlockState();
        boolean running = activeTicks > 0;
        if (state.hasProperty(MachineBlock.RUNNING) && state.getValue(MachineBlock.RUNNING) != running) {
            serverLevel.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, running));
        }

        if (energy > 0 && tickCount % SHOCK_INTERVAL == 0) shockWetEntities(serverLevel);
    }

    /** 电击范围内所有处于水中或淋雨的生物，返回被电到的数量 */
    public int shockWetEntities(ServerLevel serverLevel) {
        EUVoltage voltage = getHazardVoltage();
        float damage = shockDamage(voltage);
        int stun = stunTicks(voltage);
        DamageSource source = serverLevel.damageSources().source(ELECTRIC_DAMAGE);
        int hit = 0;
        AABB area = new AABB(worldPosition).inflate(SHOCK_RADIUS);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e.isInWaterOrRain())) {
            if (entity instanceof Player player && player.isCreative()) continue;
            if (!entity.hurtServer(serverLevel, source, damage)) continue;
            // 停滞：极高等级的缓慢，持续时间内几乎无法移动
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stun, 9, false, true));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                10, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.08);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.BEE_STING, SoundSource.BLOCKS, 0.9f, 1.7f);
            hit++;
        }
        return hit;
    }

    @Override public String getMachineName() { return "变压器"; }

    @Override public EUEnergyStorage getEnergyStorage() { return storage; }

    @Nullable
    @Override
    public EUCapability getEUCapability(@Nullable Direction direction) { return capability; }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return true; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy);
        output.putInt("tier", tierIndex);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = Math.max(0, input.getLongOr("energy", 0));
        int tier = input.getIntOr("tier", 0);
        tierIndex = tier >= 0 && tier < OUTPUT_TIERS.length ? tier : 0;
    }
}
