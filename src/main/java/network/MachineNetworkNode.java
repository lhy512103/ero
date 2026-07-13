package network;

import energy.EUCapability;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 机器网络节点
 * <p>
 * 代表网络中的一个机器，包含位置、类型和能量信息。
 * 缓存EUCapability引用，避免每tick执行level.getCapability()查找。
 * </p>
 */
public class MachineNetworkNode {

    private final BlockPos pos;
    private final String machineName;
    private final EUVoltage voltage;
    private final boolean isProducer;
    private final boolean isConsumer;
    private final boolean isStorage;

    /** 缓存的EUCapability引用，避免每tick执行level.getCapability()查找 */
    private EUCapability cachedCapability;

    /** 缓存对应的Level，用于判断缓存是否失效 */
    private Level cachedLevel;

    public MachineNetworkNode(BlockPos pos, String machineName,
                              EUVoltage voltage, boolean isProducer, boolean isConsumer, boolean isStorage) {
        this.pos = pos;
        this.machineName = machineName;
        this.voltage = voltage;
        this.isProducer = isProducer;
        this.isConsumer = isConsumer;
        this.isStorage = isStorage;
    }

    public BlockPos getPos() { return pos; }
    public String getMachineName() { return machineName; }
    public EUVoltage getVoltage() { return voltage; }
    public boolean isProducer() { return isProducer; }
    public boolean isConsumer() { return isConsumer; }
    public boolean isStorage() { return isStorage; }

    /**
     * 获取该节点在指定世界中的EUCapability
     * <p>
     * 使用缓存避免每tick执行level.getCapability()查找。
     * 缓存在Level实例变化时自动失效（如世界重载）。
     * </p>
     */
    public EUCapability getCapability(Level level) {
        if (level == null) return null;
        if (cachedCapability != null && cachedLevel == level) return cachedCapability;
        cachedCapability = level.getCapability(EUCapability.BLOCK, pos, null);
        cachedLevel = level;
        return cachedCapability;
    }

    /**
     * 使缓存失效（在机器移除或网络重建时调用）
     */
    public void invalidateCache() {
        cachedCapability = null;
        cachedLevel = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineNetworkNode other)) return false;
        return pos.equals(other.pos);
    }

    @Override
    public int hashCode() { return pos.hashCode(); }

    @Override
    public String toString() {
        return machineName + "@" + pos.toShortString();
    }
}
