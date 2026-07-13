package network.performance;

/**
 * 动态节流控制器
 * <p>
 * 基于 TPS 偏差的连续节流调节。替代固定间隔的离散节流策略，
 * 实现平滑的性能回收与恢复。
 * </p>
 *
 * <h3>核心机制：</h3>
 * <ul>
 *   <li>TPS 趋势追踪：指数移动平均（EMA）平滑瞬时波动</li>
 *   <li>偏差计算：targetTps - smoothedTps，正数表示需要节流</li>
 *   <li>分级敏感度：优先级越低，节流越激进</li>
 *   <li>节流因子：1.0 = 正常，20.0 = 最大节流</li>
 *   <li>恢复斜率：限制每 tick 最大恢复幅度，防止 TPS 抖动</li>
 * </ul>
 *
 * <h3>节流因子映射（TPS 偏差 → 节流因子）：</h3>
 * <pre>
 * 偏差=0  → 全部 1.0（正常）
 * 偏差=2  → CRITICAL 1.0, HIGH 1.2, MEDIUM 1.6, LOW 4.0, UNLOADABLE 8.0
 * 偏差=5  → CRITICAL 1.1, HIGH 1.6, MEDIUM 3.0, LOW 8.0, UNLOADABLE 16.0
 * 偏差=10 → CRITICAL 1.4, HIGH 2.4, MEDIUM 5.2, LOW 12.0, UNLOADABLE 20.0
 * 偏差=15 → CRITICAL 1.6, HIGH 3.2, MEDIUM 8.0, LOW 16.0, UNLOADABLE 20.0
 * </pre>
 */
public class DynamicThrottleController {

    private static final DynamicThrottleController INSTANCE = new DynamicThrottleController();

    /** 目标 TPS */
    private static final double TARGET_TPS = 20.0;

    /** TPS EMA 平滑系数（越小越平滑，响应越慢） */
    private static final double EMA_ALPHA = 0.15;

    /** 节流因子下限（无节流） */
    private static final double FACTOR_MIN = 1.0;

    /** 节流因子上限（最大节流） */
    private static final double FACTOR_MAX = 20.0;

    /** 每 tick 最大恢复幅度（防止 TPS 抖动时频繁切换） */
    private static final double MAX_RECOVERY_PER_TICK = 0.5;

    /** 每 tick 最大加节流幅度 */
    private static final double MAX_THROTTLE_PER_TICK = 2.0;

    /** 各优先级的节流敏感度系数 */
    private static final double[] SENSITIVITY = {
        0.1,  // CRITICAL - 几乎不节流
        0.3,  // HIGH     - 轻度敏感
        0.6,  // MEDIUM   - 中度敏感
        1.0,  // LOW      - 标准敏感
        1.4   // UNLOADABLE - 高度敏感
    };

    /** 当前平滑后的 TPS */
    private double smoothedTps = TARGET_TPS;

    /** 当前节流因子（1.0 = 正常） */
    private double throttleFactor = FACTOR_MIN;

    private DynamicThrottleController() {}

    public static DynamicThrottleController getInstance() { return INSTANCE; }

    /**
     * 每 tick 调用，更新平滑 TPS 和节流因子
     *
     * @param currentTps 当前实时 TPS
     */
    public void update(double currentTps) {
        // EMA 平滑 TPS
        smoothedTps = smoothedTps + EMA_ALPHA * (currentTps - smoothedTps);

        // 计算 TPS 偏差（正数 = 需要节流）
        double deviation = TARGET_TPS - smoothedTps;

        // 偏差为负或极小时，目标因子为 1.0（无节流）
        double targetFactor;
        if (deviation <= 0.1) {
            targetFactor = FACTOR_MIN;
        } else {
            // 线性映射：deviation 0~20 → factor 1.0~20.0
            targetFactor = FACTOR_MIN + (FACTOR_MAX - FACTOR_MIN) * Math.min(deviation / TARGET_TPS, 1.0);
        }

        // 平滑过渡：限制每 tick 变化幅度
        double delta = targetFactor - throttleFactor;
        if (delta > 0) {
            // 加节流：快速响应
            throttleFactor += Math.min(delta, MAX_THROTTLE_PER_TICK);
        } else {
            // 恢复：缓慢释放
            throttleFactor += Math.max(delta, -MAX_RECOVERY_PER_TICK);
        }

        // 钳位
        throttleFactor = Math.max(FACTOR_MIN, Math.min(FACTOR_MAX, throttleFactor));
    }

    /**
     * 获取指定优先级的动态节流间隔
     *
     * @param priority 机器优先级
     * @return 节流间隔（1 = 不节流，2 = 隔1tick执行...）
     */
    public int getThrottleInterval(MachinePriority priority) {
        if (throttleFactor <= FACTOR_MIN) return 1;

        double sensitivity = SENSITIVITY[priority.level];
        double interval = 1.0 + (throttleFactor - 1.0) * sensitivity;

        return Math.max(1, (int) Math.round(interval));
    }

    /**
     * 获取指定优先级的有效 tick 率
     */
    public double getEffectiveTickRate(MachinePriority priority) {
        int interval = getThrottleInterval(priority);
        return TARGET_TPS / interval;
    }

    /**
     * 获取当前节流因子
     */
    public double getThrottleFactor() {
        return throttleFactor;
    }

    /**
     * 获取平滑后的 TPS
     */
    public double getSmoothedTps() {
        return smoothedTps;
    }

    /**
     * 获取当前 TPS 偏差
     */
    public double getDeviation() {
        return TARGET_TPS - smoothedTps;
    }

    /**
     * 重置状态（世界切换时调用）
     */
    public void reset() {
        smoothedTps = TARGET_TPS;
        throttleFactor = FACTOR_MIN;
    }
}
