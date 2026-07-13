package client.gui.sync;

import net.minecraft.world.inventory.SimpleContainerData;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Long 值容器同步工具（参考 Mekanism SyncableLong 设计）
 * <p>
 * SimpleContainerData 只支持 int 值，因此 long 需要拆分为 2 个 int 传输。
 * 此工具类封装了拆分/重组逻辑，避免在每个 Menu 类中重复编写位操作代码。
 * <p>
 * 使用方式：
 * <pre>
 *   // 1. 计算所需大小
 *   int size = SyncableLongHelper.size(2); // 2 个 long 值需要 4 个 int 槽位
 *
 *   // 2. 创建 ContainerData
 *   SimpleContainerData data = new SimpleContainerData(size);
 *
 *   // 3. 服务端写入（在 tick 或构造时）
 *   SyncableLongHelper.setLong(data, 0, blockEntity.getEnergyStored());
 *   SyncableLongHelper.setLong(data, 2, blockEntity.getMaxEnergyStored());
 *
 *   // 4. 客户端读取
 *   long energy = SyncableLongHelper.getLong(data, 0);
 *   long maxEnergy = SyncableLongHelper.getLong(data, 2);
 * </pre>
 */
public final class SyncableLongHelper {

    private SyncableLongHelper() {}

    /**
     * 计算 N 个 long 值需要的 int 槽位数
     */
    public static int size(int longCount) {
        return longCount * 2;
    }

    /**
     * 从 SimpleContainerData 读取 long 值
     *
     * @param data  容器数据
     * @param index 起始索引（占用 index 和 index+1）
     * @return 重组后的 long 值
     */
    public static long getLong(SimpleContainerData data, int index) {
        int low = data.get(index);
        int high = data.get(index + 1);
        return ((long) high << 32) | ((long) low & 0xFFFFFFFFL);
    }

    /**
     * 向 SimpleContainerData 写入 long 值
     *
     * @param data  容器数据
     * @param index 起始索引（占用 index 和 index+1）
     * @param value 要写入的 long 值
     */
    public static void setLong(SimpleContainerData data, int index, long value) {
        data.set(index, (int) (value & 0xFFFFFFFFL));
        data.set(index + 1, (int) (value >>> 32));
    }

    /**
     * 创建一个自动同步 long 值的 Runnable（服务端用）
     * <p>
     * 返回的 Runnable 在每次调用时将 getter 的值写入 data。
     * 适合在 Menu 的 tick 或构造函数中调用。
     *
     * @param data   容器数据
     * @param index  起始索引
     * @param getter 值提供者
     * @return 同步 Runnable
     */
    public static Runnable createSyncTask(SimpleContainerData data, int index, LongSupplier getter) {
        return () -> setLong(data, index, getter.getAsLong());
    }

    /**
     * 创建一个带有双向同步的 SyncableLong 实例
     * <p>
     * 服务端：getter → data
     * 客户端：data → setter
     */
    public static SyncableLongTracker createTracker(SimpleContainerData data, int index,
                                                     LongSupplier getter, LongConsumer setter) {
        return new SyncableLongTracker(data, index, getter, setter);
    }

    /**
     * 可追踪的 Long 同步器
     */
    public static class SyncableLongTracker {
        private final SimpleContainerData data;
        private final int index;
        private final LongSupplier getter;
        private final LongConsumer setter;
        private long lastValue;

        SyncableLongTracker(SimpleContainerData data, int index,
                            LongSupplier getter, LongConsumer setter) {
            this.data = data;
            this.index = index;
            this.getter = getter;
            this.setter = setter;
        }

        /**
         * 服务端调用：将当前值写入 data（仅在值变化时写入）
         */
        public void syncFromServer() {
            long current = getter.getAsLong();
            if (current != lastValue) {
                lastValue = current;
                setLong(data, index, current);
            }
        }

        /**
         * 客户端调用：从 data 读取值并应用到 setter
         */
        public void syncToClient() {
            long current = getLong(data, index);
            setter.accept(current);
        }

        /**
         * 获取当前值（客户端用）
         */
        public long get() {
            return getLong(data, index);
        }
    }
}
