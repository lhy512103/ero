package machine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;

/**
 * 打粉机 GUI 提供者接口
 * <p>
 * 所有打粉机方块实体实现此接口以支持右键打开 GUI。
 * </p>
 */
public interface MaceratorGuiProvider extends MenuProvider {

    /**
     * 向客户端写入额外数据（方块位置等）
     */
    default void writeClientSideData(RegistryFriendlyByteBuf buf) {
        // 默认不写入额外数据，子类可覆盖
    }
}
