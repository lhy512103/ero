package network;

import energy.EUCapability;
import energy.EUEnergyHelper;
import energy.EUVoltage;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * 能量网络
 * <p>
 * 管理一组连接的机器节点，自动在产电机器和耗电机器之间传输能量。
 * 同一网络内的机器共享能量，发电机产出自动分配给耗电设备和储能设备。
 * </p>
 */
public class EnergyNetwork {

    private final String networkUuid;
    /** 使用 Long2ObjectOpenHashMap 替代 LinkedHashMap<BlockPos, ...>，避免 BlockPos 哈希计算和自动装箱 */
    private final Long2ObjectOpenHashMap<MachineNetworkNode> nodes = new Long2ObjectOpenHashMap<>();
    private long totalEnergyProduced = 0;
    private long totalEnergyConsumed = 0;

    /** 复用的临时集合，避免每tick创建新对象 */
    private final Long2ObjectOpenHashMap<EUCapability> capCache = new Long2ObjectOpenHashMap<>();
    private final List<MachineNetworkNode> producers = new ArrayList<>();
    private final List<MachineNetworkNode> consumers = new ArrayList<>();
    private final List<MachineNetworkNode> storages = new ArrayList<>();
    private final LinkedHashMap<Long, Long> producerAvailable = new LinkedHashMap<>();

    /** 按电压等级分组的producer索引，避免O(C*P)嵌套循环 */
    private final EnumMap<EUVoltage, List<MachineNetworkNode>> producersByVoltage = new EnumMap<>(EUVoltage.class);

    /** 预分配的电压分组列表，避免每tick创建新ArrayList */
    private final List<MachineNetworkNode> voltageListULV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListLV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListMV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListHV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListEV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListIV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListLuV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListZPM = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListUV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListUHV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListUEV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListUIV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListUMV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListUXV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListOpV = new ArrayList<>();
    private final List<MachineNetworkNode> voltageListMAX = new ArrayList<>();

    /** 电压到预分配列表的映射 */
    private final EnumMap<EUVoltage, List<MachineNetworkNode>> voltageListMap;

    public EnergyNetwork() {
        this.networkUuid = UUID.randomUUID().toString().substring(0, 8);
        voltageListMap = new EnumMap<>(EUVoltage.class);
        voltageListMap.put(EUVoltage.ULV, voltageListULV);
        voltageListMap.put(EUVoltage.LV, voltageListLV);
        voltageListMap.put(EUVoltage.MV, voltageListMV);
        voltageListMap.put(EUVoltage.HV, voltageListHV);
        voltageListMap.put(EUVoltage.EV, voltageListEV);
        voltageListMap.put(EUVoltage.IV, voltageListIV);
        voltageListMap.put(EUVoltage.LuV, voltageListLuV);
        voltageListMap.put(EUVoltage.ZPM, voltageListZPM);
        voltageListMap.put(EUVoltage.UV, voltageListUV);
        voltageListMap.put(EUVoltage.UHV, voltageListUHV);
        voltageListMap.put(EUVoltage.UEV, voltageListUEV);
        voltageListMap.put(EUVoltage.UIV, voltageListUIV);
        voltageListMap.put(EUVoltage.UMV, voltageListUMV);
        voltageListMap.put(EUVoltage.UXV, voltageListUXV);
        voltageListMap.put(EUVoltage.OpV, voltageListOpV);
        voltageListMap.put(EUVoltage.MAX, voltageListMAX);
    }

    /**
     * 使用指定UUID创建网络（用于反序列化）
     */
    public EnergyNetwork(String networkUuid) {
        this.networkUuid = networkUuid;
        voltageListMap = new EnumMap<>(EUVoltage.class);
        voltageListMap.put(EUVoltage.ULV, voltageListULV);
        voltageListMap.put(EUVoltage.LV, voltageListLV);
        voltageListMap.put(EUVoltage.MV, voltageListMV);
        voltageListMap.put(EUVoltage.HV, voltageListHV);
        voltageListMap.put(EUVoltage.EV, voltageListEV);
        voltageListMap.put(EUVoltage.IV, voltageListIV);
        voltageListMap.put(EUVoltage.LuV, voltageListLuV);
        voltageListMap.put(EUVoltage.ZPM, voltageListZPM);
        voltageListMap.put(EUVoltage.UV, voltageListUV);
        voltageListMap.put(EUVoltage.UHV, voltageListUHV);
        voltageListMap.put(EUVoltage.UEV, voltageListUEV);
        voltageListMap.put(EUVoltage.UIV, voltageListUIV);
        voltageListMap.put(EUVoltage.UMV, voltageListUMV);
        voltageListMap.put(EUVoltage.UXV, voltageListUXV);
        voltageListMap.put(EUVoltage.OpV, voltageListOpV);
        voltageListMap.put(EUVoltage.MAX, voltageListMAX);
    }

    public String getNetworkUuid() { return networkUuid; }

    public void addNode(MachineNetworkNode node) {
        nodes.put(node.getPos().asLong(), node);
    }

    public void removeNode(BlockPos pos) {
        nodes.remove(pos.asLong());
    }

    public Collection<MachineNetworkNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public int getNodeCount() { return nodes.size(); }

    public boolean isEmpty() { return nodes.isEmpty(); }

    /**
     * 每tick执行能量传输
     *
     * @return true=本tick有能量传输，false=无能量传输（用于空闲检测）
     */
    public boolean tick(Level level) {
        if (nodes.isEmpty()) return false;

        // 清空复用集合
        capCache.clear();
        producers.clear();
        consumers.clear();
        storages.clear();
        producerAvailable.clear();

        // 清空电压分组列表（复用而非new）
        for (List<MachineNetworkNode> list : voltageListMap.values()) {
            list.clear();
        }
        producersByVoltage.clear();

        for (MachineNetworkNode node : nodes.values()) {
            EUCapability cap = node.getCapability(level);
            if (cap == null) continue;
            capCache.put(node.getPos().asLong(), cap);

            if (node.isProducer() && cap.canExtract() && cap.getEnergyStored() > 0) {
                producers.add(node);
                // 按电压等级分组，复用预分配列表
                List<MachineNetworkNode> voltageList = voltageListMap.get(cap.getVoltage());
                if (voltageList != null) {
                    voltageList.add(node);
                    producersByVoltage.put(cap.getVoltage(), voltageList);
                }
            }
            if (node.isConsumer() && cap.canReceive() && cap.getEnergyStored() < cap.getMaxEnergyStored()) {
                consumers.add(node);
            }
            if (node.isStorage() && cap.canReceive() && cap.getEnergyStored() < cap.getMaxEnergyStored()) {
                storages.add(node);
            }
        }

        if (producers.isEmpty()) return false;

        totalEnergyProduced = 0;
        totalEnergyConsumed = 0;

        long totalAvailable = 0;
        for (MachineNetworkNode producer : producers) {
            EUCapability cap = capCache.get(producer.getPos().asLong());
            if (cap == null) continue;
            long available = cap.extractEnergy(cap.getMaxTransferRate(), true);
            if (available > 0) {
                producerAvailable.put(producer.getPos().asLong(), available);
                totalAvailable += available;
            }
        }

        if (totalAvailable <= 0) return false;

        long remaining = totalAvailable;

        // 分配给耗电机器（使用电压分组优化，O(C+P) 而非 O(C*P)）
        for (MachineNetworkNode consumer : consumers) {
            if (remaining <= 0) break;
            EUCapability cap = capCache.get(consumer.getPos().asLong());
            if (cap == null) continue;

            if (!hasCompatibleProducer(cap.getVoltage())) continue;

            long needed = cap.getMaxEnergyStored() - cap.getEnergyStored();
            if (needed <= 0) continue;
            long canReceive = Math.min(needed, cap.getMaxTransferRate());
            long toTransfer = Math.min(remaining, canReceive);
            if (toTransfer <= 0) continue;

            long received = cap.receiveEnergy(toTransfer, false);
            if (received > 0) {
                remaining -= received;
                totalEnergyConsumed += received;
            }
        }

        // 剩余能量存入储能设备
        for (MachineNetworkNode storage : storages) {
            if (remaining <= 0) break;
            EUCapability cap = capCache.get(storage.getPos().asLong());
            if (cap == null) continue;

            if (!hasCompatibleProducer(cap.getVoltage())) continue;

            long needed = cap.getMaxEnergyStored() - cap.getEnergyStored();
            if (needed <= 0) continue;
            long canReceive = Math.min(needed, cap.getMaxTransferRate());
            long toTransfer = Math.min(remaining, canReceive);
            if (toTransfer <= 0) continue;

            long received = cap.receiveEnergy(toTransfer, false);
            if (received > 0) {
                remaining -= received;
            }
        }

        // 按比例扣除产电机器能量
        long totalExtracted = totalAvailable - remaining;
        if (totalExtracted > 0) {
            for (Map.Entry<Long, Long> entry : producerAvailable.entrySet()) {
                EUCapability cap = capCache.get(entry.getKey());
                if (cap == null) continue;
                long producerShare = (long) ((double) entry.getValue() / totalAvailable * totalExtracted);
                if (producerShare > 0) {
                    cap.extractEnergy(producerShare, false);
                    totalEnergyProduced += producerShare;
                }
            }
        }

        return totalExtracted > 0;
    }

    /**
     * 检查是否有与指定电压兼容的producer（O(V) 而非 O(P)）
     */
    private boolean hasCompatibleProducer(EUVoltage voltage) {
        for (Map.Entry<EUVoltage, List<MachineNetworkNode>> entry : producersByVoltage.entrySet()) {
            if (!entry.getValue().isEmpty() && EUEnergyHelper.isVoltageCompatible(voltage, entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    public long getTotalEnergyProduced() { return totalEnergyProduced; }

    public long getTotalEnergyConsumed() { return totalEnergyConsumed; }

    public List<MachineNetworkNode> getNodesByVoltage(EUVoltage voltage) {
        List<MachineNetworkNode> result = new ArrayList<>();
        for (MachineNetworkNode node : nodes.values()) {
            if (node.getVoltage() == voltage) {
                result.add(node);
            }
        }
        return result;
    }

    public void merge(EnergyNetwork other) {
        for (MachineNetworkNode node : other.nodes.values()) {
            this.nodes.put(node.getPos().asLong(), node);
        }
    }

    @Override
    public String toString() {
        return "EnergyNetwork[" + networkUuid +
            ", nodes=" + nodes.size() +
            ", produced=" + totalEnergyProduced +
            ", consumed=" + totalEnergyConsumed + "]";
    }
}
