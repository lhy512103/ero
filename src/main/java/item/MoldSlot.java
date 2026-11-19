package item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** Holds exactly one mold. */
public class MoldSlot extends ResourceHandlerSlot {
    public MoldSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> modifier, int handlerSlot, int x, int y) {
        super(handler, modifier, handlerSlot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof MoldItem; }

    @Override
    public int getMaxStackSize() { return 1; }

    @Override
    public int getMaxStackSize(ItemStack stack) { return 1; }
}
