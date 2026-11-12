package item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 充电槽位
 * <p>
 * 只允许放置充电电池等可充能物品的特殊槽位。
 * </p>
 */
public class ChargeSlot extends ResourceHandlerSlot {

    public ChargeSlot(ResourceHandler<ItemResource> handler,
                      IndexModifier<ItemResource> slotModifier,
                      int handlerSlot, int xPosition, int yPosition) {
        super(handler, slotModifier, handlerSlot, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isChargeableItem(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * 检查物品是否为可充能的电池类物品
     */
    public static boolean isChargeableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack)
            .getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM) != null;
    }
}
