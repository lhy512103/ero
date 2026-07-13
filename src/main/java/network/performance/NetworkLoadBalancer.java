package network.performance;

import java.util.*;

/**
 * 网络负载均衡器
 * <p>
 * 将大量能量网络的tick计算分散到不同tick中执行，
 * 避免所有网络在同一tick集中处理导致TPS下降。
 * 支持根据性能状态动态调整网络tick频率和分批策略。
 * </p>
 */
public class NetworkLoadBalancer {

    private static final NetworkLoadBalancer INSTANCE = new NetworkLoadBalancer();

    /** 网络tick分组：将网络ID映射到其所属的tick批次 */
    private final Map<String, Integer> networkBatches = new HashMap<>();

    /** 当前批次数量（动态调整） */
    private int batchCount = 1;

    /** 当前tick计数 */
    private long tickCount = 0;

    /** 当前性能状态 */
    private PerformanceState currentState = PerformanceState.NORMAL;

    /** 空闲网络：最近N个tick内无能量传输的网络 */
    private final Set<String> idleNetworks = new HashSet<>();

    /** 网络空闲计数器：网络ID -> 连续无能量传输的tick数 */
    private final Map<String, Integer> idleCounters = new HashMap<>();

    /** 空闲判定阈值：连续N个tick无能量传输则判定为空闲 */
    private static final int IDLE_THRESHOLD = 200; // 10秒

    /** 空闲网络的tick间隔 */
    private static final int IDLE_TICK_INTERVAL = 100; // 空闲网络每5秒检查一次

    private NetworkLoadBalancer() {}

    public static NetworkLoadBalancer getInstance() { return INSTANCE; }

    /**
     * 更新性能状态（由PerformanceGovernor调用）
     */
    public void updateState(PerformanceState newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            recalculateBatches();
        }
    }

    /**
     * 注册网络到负载均衡器
     */
    public void registerNetwork(String networkUuid) {
        if (networkBatches.containsKey(networkUuid)) return;
        // 分配到当前最少使用的批次
        int batch = findLeastLoadedBatch();
        networkBatches.put(networkUuid, batch);
    }

    /**
     * 从负载均衡器注销网络
     */
    public void unregisterNetwork(String networkUuid) {
        networkBatches.remove(networkUuid);
        idleNetworks.remove(networkUuid);
        idleCounters.remove(networkUuid);
    }

    /**
     * 通知网络在本tick有能量传输（重置空闲计数）
     */
    public void notifyNetworkActive(String networkUuid) {
        idleCounters.remove(networkUuid);
        idleNetworks.remove(networkUuid);
    }

    /**
     * 每tick调用，更新空闲检测和批次分配
     */
    public void tick() {
        tickCount++;

        // 更新空闲检测
        for (Map.Entry<String, Integer> entry : idleCounters.entrySet()) {
            int newCount = entry.getValue() + 1;
            if (newCount >= IDLE_THRESHOLD) {
                idleNetworks.add(entry.getKey());
            } else {
                entry.setValue(newCount);
            }
        }
    }

    /**
     * 判断指定网络在当前tick是否应该执行能量传输
     *
     * @param networkUuid 网络UUID
     * @return true=应执行tick，false=跳过
     */
    public boolean shouldTickNetwork(String networkUuid) {
        // 空闲网络大幅降低tick频率
        if (idleNetworks.contains(networkUuid)) {
            return tickCount % IDLE_TICK_INTERVAL == Math.abs(networkUuid.hashCode()) % IDLE_TICK_INTERVAL;
        }

        // 性能正常时所有网络每tick执行
        if (batchCount <= 1) return true;

        // 分批执行：当前tick只执行对应批次的网络
        Integer batch = networkBatches.get(networkUuid);
        if (batch == null) return true;

        int currentBatch = (int) (tickCount % batchCount);
        return batch == currentBatch;
    }

    /**
     * 通知网络在本tick无能量传输（增加空闲计数）
     */
    public void notifyNetworkIdle(String networkUuid) {
        idleCounters.merge(networkUuid, 1, Integer::sum);
    }

    /**
     * 获取当前活跃网络数量
     */
    public int getActiveNetworkCount() {
        return networkBatches.size() - idleNetworks.size();
    }

    /**
     * 获取空闲网络数量
     */
    public int getIdleNetworkCount() {
        return idleNetworks.size();
    }

    /**
     * 获取总网络数量
     */
    public int getTotalNetworkCount() {
        return networkBatches.size();
    }

    /**
     * 根据性能状态重新计算批次分配
     */
    private void recalculateBatches() {
        int totalNetworks = networkBatches.size();

        // 根据性能状态和网络数量决定批次数
        batchCount = switch (currentState) {
            case NORMAL -> 1;
            case WARNING -> Math.max(1, totalNetworks / 50);  // 每50个网络一批
            case STRESSED -> Math.max(1, totalNetworks / 20); // 每20个网络一批
            case CRITICAL -> Math.max(1, totalNetworks / 10); // 每10个网络一批
        };

        if (batchCount > 1) {
            // 重新分配批次，使用一致性哈希确保网络稳定分配
            for (Map.Entry<String, Integer> entry : networkBatches.entrySet()) {
                int batch = Math.abs(entry.getKey().hashCode()) % batchCount;
                entry.setValue(batch);
            }
        }
    }

    /**
     * 找到当前最少使用的批次
     */
    private int findLeastLoadedBatch() {
        if (batchCount <= 1) return 0;

        int[] batchCounts = new int[batchCount];
        for (int batch : networkBatches.values()) {
            if (batch < batchCounts.length) {
                batchCounts[batch]++;
            }
        }

        int minBatch = 0;
        int minCount = batchCounts[0];
        for (int i = 1; i < batchCounts.length; i++) {
            if (batchCounts[i] < minCount) {
                minCount = batchCounts[i];
                minBatch = i;
            }
        }
        return minBatch;
    }
}
