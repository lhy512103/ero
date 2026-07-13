package machine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;

/**
 * 提取机 GUI 提供者接口
 */
public interface ExtractorGuiProvider extends MenuProvider {
    default void writeClientSideData(RegistryFriendlyByteBuf buf) {}
}
