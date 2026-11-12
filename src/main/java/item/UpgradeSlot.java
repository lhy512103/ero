package item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * 升级槽位
 * <p>
 * 只允许放置升级物品的特殊槽位。
 * </p>
 */
public class UpgradeSlot extends ResourceHandlerSlot {

    public UpgradeSlot(ResourceHandler<ItemResource> handler,
                       IndexModifier<ItemResource> slotModifier,
                       int handlerSlot, int xPosition, int yPosition) {
        super(handler, slotModifier, handlerSlot, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 只允许放置升级物品
        return stack.getItem() instanceof UpgradeItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1; // 每个升级槽只放 1 个
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1; // 升级可在背包里堆叠，但放进槽里始终只取 1 个
    }
}
