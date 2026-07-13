package network.performance;

/**
 * 性能状态枚举
 * <p>
 * 定义服务器在不同负载水平下的性能状态，用于驱动自动化性能管理策略。
 * </p>
 */
public enum PerformanceState {

    /** 正常状态 - TPS >= 18，无需节流 */
    NORMAL(20.0, 18.0, 1.0, "正常"),

    /** 警告状态 - TPS 15~18，轻度节流 */
    WARNING(18.0, 15.0, 0.7, "警告"),

    /** 紧张状态 - TPS 10~15，中度节流 */
    STRESSED(15.0, 10.0, 0.4, "紧张"),

    /** 危急状态 - TPS < 10，重度节流 */
    CRITICAL(10.0, 0.0, 0.2, "危急");

    /** 该状态的TPS上限 */
    public final double tpsUpperBound;

    /** 该状态的TPS下限 */
    public final double tpsLowerBound;

    /** 该状态下的tick预算比例 (0.0~1.0) */
    public final double tickBudgetRatio;

    /** 显示名称 */
    public final String displayName;

    PerformanceState(double tpsUpperBound, double tpsLowerBound, double tickBudgetRatio, String displayName) {
        this.tpsUpperBound = tpsUpperBound;
        this.tpsLowerBound = tpsLowerBound;
        this.tickBudgetRatio = tickBudgetRatio;
        this.displayName = displayName;
    }

    /**
     * 根据当前TPS值确定性能状态
     */
    public static PerformanceState fromTps(double tps) {
        if (tps >= NORMAL.tpsUpperBound) return NORMAL;
        if (tps >= WARNING.tpsLowerBound) return WARNING;
        if (tps >= STRESSED.tpsLowerBound) return STRESSED;
        return CRITICAL;
    }

    /**
     * 判断是否需要节流
     */
    public boolean shouldThrottle() {
        return this != NORMAL;
    }
}
