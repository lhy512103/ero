package network.performance;

/**
 * 机器优先级
 * <p>
 * 定义机器在性能节流时的优先级，高优先级机器更晚被节流。
 * </p>
 */
public enum MachinePriority {

    /** 关键 - 储能设备、发电机等核心设备，最后被节流 */
    CRITICAL(0, "关键"),

    /** 高 - 正在工作的加工机器 */
    HIGH(1, "高"),

    /** 中 - 空闲但有能量缓冲的机器 */
    MEDIUM(2, "中"),

    /** 低 - 空闲且无能量缓冲的机器，最先被节流 */
    LOW(3, "低"),

    /** 可卸载 - 待机机器，可完全跳过tick */
    UNLOADABLE(4, "可卸载");

    /** 优先级数值，越小越优先 */
    public final int level;

    /** 显示名称 */
    public final String displayName;

    MachinePriority(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    /**
     * 根据性能状态获取该优先级对应的节流间隔
     *
     * @param state 当前性能状态
     * @return 每N个tick执行一次（1=不节流，2=隔1tick，4=隔3tick...）
     */
    public int getThrottleInterval(PerformanceState state) {
        return switch (this) {
            case CRITICAL -> switch (state) {
                case NORMAL -> 1;
                case WARNING -> 1;
                case STRESSED -> 2;
                case CRITICAL -> 3;
            };
            case HIGH -> switch (state) {
                case NORMAL -> 1;
                case WARNING -> 2;
                case STRESSED -> 4;
                case CRITICAL -> 8;
            };
            case MEDIUM -> switch (state) {
                case NORMAL -> 1;
                case WARNING -> 4;
                case STRESSED -> 8;
                case CRITICAL -> 16;
            };
            case LOW -> switch (state) {
                case NORMAL -> 1;
                case WARNING -> 8;
                case STRESSED -> 16;
                case CRITICAL -> 32;
            };
            case UNLOADABLE -> switch (state) {
                case NORMAL -> 1;
                case WARNING -> 16;
                case STRESSED -> 32;
                case CRITICAL -> 64;
            };
        };
    }
}
