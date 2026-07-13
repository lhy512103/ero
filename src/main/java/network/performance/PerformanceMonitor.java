package network.performance;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能监控器
 * <p>
 * 实时监控服务器TPS，通过滑动窗口计算平均TPS，
 * 并在性能状态变化时通知监听器。
 * </p>
 */
public class PerformanceMonitor {

    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

    /** 滑动窗口大小（采样tick数） */
    private static final int WINDOW_SIZE = 100;

    /** TPS采样间隔（每N个tick采样一次） */
    private static final int SAMPLE_INTERVAL = 5;

    /** 状态变化所需的持续tick数，避免瞬时波动误判 */
    private static final int STATE_CHANGE_HOLD_TICKS = 60;

    /** tick时间记录（纳秒） */
    private final long[] tickTimes = new long[WINDOW_SIZE];
    private int tickTimeIndex = 0;
    private int tickTimeCount = 0;

    /** 滑动窗口总和（纳秒），避免每次 updateMetrics 全窗口遍历 */
    private long slidingSum = 0;

    /** 当前性能状态 */
    private PerformanceState currentState = PerformanceState.NORMAL;

    /** 状态变化候选（需要持续一段时间才确认变化） */
    private PerformanceState pendingState = PerformanceState.NORMAL;
    private int pendingStateTicks = 0;

    /** 当前tick计数 */
    private long tickCount = 0;

    /** 上一次采样时间 */
    private long lastSampleTime = 0;

    /** 缓存的TPS值 */
    private double cachedTps = 20.0;

    /** 缓存的平均tick耗时（毫秒） */
    private double cachedAvgTickTime = 0.0;

    /** 状态变化监听器 */
    private final List<PerformanceStateListener> listeners = new ArrayList<>();

    private boolean registered = false;

    private PerformanceMonitor() {}

    public static PerformanceMonitor getInstance() { return INSTANCE; }

    /**
     * 注册服务器tick事件监听
     */
    public void register() {
        if (registered) return;
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        registered = true;
    }

    /**
     * 添加性能状态变化监听器
     */
    public void addListener(PerformanceStateListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除性能状态变化监听器
     */
    public void removeListener(PerformanceStateListener listener) {
        listeners.remove(listener);
    }

    private void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        // 服务器冻结时（无玩家/手动降速）跳过TPS采样，
        // 因为此时 getAverageTickTimeNanos() 返回的是不准确的值，
        // 会导致错误的性能状态转换
        if (!server.tickRateManager().runsNormally()) {
            return;
        }

        tickCount++;

        // 采样tick耗时（滑动窗口：替换旧值时减去旧值，加上新值）
        long currentTickTime = server.getAverageTickTimeNanos();
        if (tickTimeCount >= WINDOW_SIZE) {
            slidingSum -= tickTimes[tickTimeIndex];
        }
        tickTimes[tickTimeIndex] = currentTickTime;
        slidingSum += currentTickTime;
        tickTimeIndex = (tickTimeIndex + 1) % WINDOW_SIZE;
        if (tickTimeCount < WINDOW_SIZE) tickTimeCount++;

        // 每SAMPLE_INTERVAL个tick更新一次TPS计算
        if (tickCount % SAMPLE_INTERVAL == 0) {
            updateMetrics();
            checkStateTransition();
        }
    }

    /**
     * 更新TPS和平均tick耗时指标（使用滑动窗口总和，O(1) 复杂度）
     */
    private void updateMetrics() {
        if (tickTimeCount == 0) return;

        double avgNanos = (double) slidingSum / tickTimeCount;
        cachedAvgTickTime = avgNanos / 1_000_000.0; // 转毫秒

        // TPS = 1000ms / avgTickTimeMs，上限20
        if (cachedAvgTickTime > 0) {
            cachedTps = Math.min(20.0, 1000.0 / cachedAvgTickTime);
        } else {
            cachedTps = 20.0;
        }

        // 更新动态节流控制器（基于 TPS 偏差的连续节流）
        DynamicThrottleController.getInstance().update(cachedTps);
    }

    /**
     * 检查并执行性能状态转换
     * <p>
     * 使用滞后机制：新状态需要持续维持 STATE_CHANGE_HOLD_TICKS 才会确认转换，
     * 避免瞬时波动导致频繁切换。
     * </p>
     */
    private void checkStateTransition() {
        PerformanceState detected = PerformanceState.fromTps(cachedTps);

        if (detected == currentState) {
            // 状态一致，重置候选
            pendingState = currentState;
            pendingStateTicks = 0;
            return;
        }

        if (detected == pendingState) {
            pendingStateTicks++;
            if (pendingStateTicks >= STATE_CHANGE_HOLD_TICKS) {
                // 确认状态变化
                PerformanceState oldState = currentState;
                currentState = pendingState;
                pendingStateTicks = 0;
                notifyStateChange(oldState, currentState);
            }
        } else {
            // 新的候选状态，重置计数
            pendingState = detected;
            pendingStateTicks = 1;
        }
    }

    /**
     * 通知所有监听器状态变化
     */
    private void notifyStateChange(PerformanceState oldState, PerformanceState newState) {
        for (PerformanceStateListener listener : listeners) {
            listener.onPerformanceStateChanged(oldState, newState);
        }
    }

    // ==================== 查询接口 ====================

    public PerformanceState getCurrentState() { return currentState; }

    public double getCurrentTps() { return cachedTps; }

    public double getAverageTickTimeMs() { return cachedAvgTickTime; }

    public long getTickCount() { return tickCount; }

    /**
     * 获取当前状态下的tick预算比例
     */
    public double getTickBudgetRatio() {
        return currentState.tickBudgetRatio;
    }

    /**
     * 判断当前是否需要节流
     */
    public boolean shouldThrottle() {
        return currentState.shouldThrottle();
    }

    /**
     * 获取性能摘要信息
     */
    public String getSummary() {
        return String.format("[%s] TPS: %.1f | 平均耗时: %.1fms | 预算: %.0f%%",
            currentState.displayName, cachedTps, cachedAvgTickTime, currentState.tickBudgetRatio * 100);
    }

    /**
     * 性能状态变化监听器接口
     */
    @FunctionalInterface
    public interface PerformanceStateListener {
        void onPerformanceStateChanged(PerformanceState oldState, PerformanceState newState);
    }
}
