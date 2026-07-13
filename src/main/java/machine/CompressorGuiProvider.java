package machine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;

/**
 * 压缩机 GUI 提供者接口
 */
public interface CompressorGuiProvider extends MenuProvider {
    default void writeClientSideData(RegistryFriendlyByteBuf buf) {}
}
