package machine;

import blockentity.EnergyDisplay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * 储电箱 GUI 提供者接口
 */
public interface StorageGuiProvider extends MenuProvider, EnergyDisplay {
    /**
     * 获取物品处理器（10槽位：充电、放电、2升级、充电槽1、充电槽2、4装备充电槽）
     */
    ItemStacksResourceHandler getItemHandler();

    default void writeClientSideData(RegistryFriendlyByteBuf buf) {}
}
