package blockentity;

/**
 * 处理进度显示接口
 * <p>
 * 实现此接口的方块实体可以显示处理进度信息。
 * 用于Jade兼容、UI显示等场景。
 * </p>
 */
public interface ProcessingDisplay {

    /**
     * 获取处理进度比例
     *
     * @return 0.0 到 1.0 之间的进度比例
     */
    float getProcessingProgress();

    /**
     * 检查是否正在处理
     *
     * @return 如果正在处理返回true
     */
    boolean isProcessing();

    /**
     * 获取进度显示标签
     *
     * @return 进度条前缀文字，默认"处理进度"
     */
    default String getProgressLabel() {
        return "处理进度";
    }
}
