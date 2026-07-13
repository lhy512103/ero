package machine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;

/**
 * 电解机 GUI 提供者接口
 */
public interface ElectrolyzerGuiProvider extends MenuProvider {

    default void writeClientSideData(RegistryFriendlyByteBuf buf) {
    }
}
