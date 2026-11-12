package item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 装备充电槽位
 * <p>
 * 只允许放置能量装备（具有能量能力的装备物品）的特殊槽位。
 * </p>
 */
public class EquipmentChargeSlot extends ResourceHandlerSlot {

    public EquipmentChargeSlot(ResourceHandler<ItemResource> handler,
                               IndexModifier<ItemResource> slotModifier,
                               int handlerSlot, int xPosition, int yPosition) {
        super(handler, slotModifier, handlerSlot, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isEnergyEquipment(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * 检查物品是否为能量装备（具有能量能力的装备物品）
     */
    public static boolean isEnergyEquipment(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Tools and armor share these charging slots; capability, not equipment type, decides.
        return ChargeSlot.isChargeableItem(stack);
    }

    /**
     * 检查物品是否具有能量存储能力
     */
    private static boolean hasEnergyCapability(ItemStack stack) {
        try {
            return stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack)) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
