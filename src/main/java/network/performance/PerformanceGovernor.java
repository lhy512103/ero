package network.performance;

/**
 * 性能调节器（策略引擎）
 * <p>
 * 核心调度组件，监听性能状态变化，协调 TickThrottler 和 NetworkLoadBalancer
 * 执行相应的节流和负载均衡策略。确保在工作时快速调用性能，待机时及时释放。
 * </p>
 *
 * <h3>策略说明：</h3>
 * <ul>
 *   <li>NORMAL: 全速运行，所有机器和网络每tick执行</li>
 *   <li>WARNING: 轻度节流，低优先级机器降频，网络开始分批</li>
 *   <li>STRESSED: 中度节流，仅关键和高优先级机器正常运行，网络深度分批</li>
 *   <li>CRITICAL: 重度节流，仅关键机器运行，网络最大分批，空闲网络暂停</li>
 * </ul>
 */
public class PerformanceGovernor implements PerformanceMonitor.PerformanceStateListener {

    private static final PerformanceGovernor INSTANCE = new PerformanceGovernor();

    private PerformanceGovernor() {}

    public static PerformanceGovernor getInstance() { return INSTANCE; }

    /**
     * 初始化性能调节器，注册为PerformanceMonitor的监听器
     */
    public void initialize() {
        PerformanceMonitor.getInstance().addListener(this);
    }

    @Override
    public void onPerformanceStateChanged(PerformanceState oldState, PerformanceState newState) {
        // 更新TickThrottler的节流策略
        TickThrottler.getInstance().updateState(newState);

        // 更新NetworkLoadBalancer的分批策略
        NetworkLoadBalancer.getInstance().updateState(newState);
    }

    /**
     * 查询当前性能状态
     */
    public PerformanceState getCurrentState() {
        return PerformanceMonitor.getInstance().getCurrentState();
    }

    /**
     * 查询当前TPS
     */
    public double getCurrentTps() {
        return PerformanceMonitor.getInstance().getCurrentTps();
    }

    /**
     * 获取性能摘要
     */
    public String getSummary() {
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        TickThrottler throttler = TickThrottler.getInstance();
        DynamicThrottleController dtc = throttler.getDynamicController();
        NetworkLoadBalancer balancer = NetworkLoadBalancer.getInstance();

        return String.format(
            "%s | 动态节流: 因子%.1f 偏差%.1f | 网络: %d活跃/%d空闲/%d总计",
            monitor.getSummary(),
            dtc.getThrottleFactor(),
            dtc.getDeviation(),
            balancer.getActiveNetworkCount(),
            balancer.getIdleNetworkCount(),
            balancer.getTotalNetworkCount()
        );
    }
}
