package network.performance;

/**
 * Tick节流器
 * <p>
 * 根据性能状态和机器优先级，控制机器的tick执行频率。
 * 集成 {@link DynamicThrottleController} 实现基于 TPS 偏差的连续动态节流，
 * 替代固定间隔的离散策略，实现平滑的性能回收与恢复。
 * </p>
 */
public class TickThrottler {

    private static final TickThrottler INSTANCE = new TickThrottler();

    /** 当前性能状态 */
    private PerformanceState currentState = PerformanceState.NORMAL;

    /** 动态节流控制器 */
    private final DynamicThrottleController dynamicController = DynamicThrottleController.getInstance();

    private TickThrottler() {}

    public static TickThrottler getInstance() { return INSTANCE; }

    /**
     * 更新性能状态（由PerformanceGovernor调用）
     */
    public void updateState(PerformanceState newState) {
        this.currentState = newState;
    }

    /**
     * 判断指定优先级的机器在当前tick是否应该执行
     * <p>
     * 使用动态节流控制器计算间隔，实现连续平滑的节流。
     * </p>
     *
     * @param priority  机器优先级
     * @param tickCount 当前tick计数
     * @return true=应执行tick，false=跳过此tick
     */
    public boolean shouldTick(MachinePriority priority, long tickCount) {
        if (currentState == PerformanceState.NORMAL) return true;

        int interval = dynamicController.getThrottleInterval(priority);
        if (interval <= 1) return true;

        // 使用tickCount取模实现均匀分布的tick节流
        // 不同优先级使用不同的偏移量，避免同优先级机器在同一tick集中执行
        return tickCount % interval == priority.level % interval;
    }

    /**
     * 获取指定优先级机器的有效tick率（每秒实际tick次数）
     *
     * @param priority 机器优先级
     * @return 有效tick率
     */
    public double getEffectiveTickRate(MachinePriority priority) {
        if (currentState == PerformanceState.NORMAL) return 20.0;
        return dynamicController.getEffectiveTickRate(priority);
    }

    /**
     * 获取当前性能状态
     */
    public PerformanceState getCurrentState() {
        return currentState;
    }

    /**
     * 获取动态节流控制器
     */
    public DynamicThrottleController getDynamicController() {
        return dynamicController;
    }
}
