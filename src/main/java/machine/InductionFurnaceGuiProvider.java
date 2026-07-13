package machine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;

/**
 * 合金炉 GUI 提供者接口
 */
public interface InductionFurnaceGuiProvider extends MenuProvider {
    default void writeClientSideData(RegistryFriendlyByteBuf buf) {}
}
