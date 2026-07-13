package item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.function.Supplier;

/**
 * 流体显示槽位（仅用于 GUI 渲染，不可交互）
 * <p>
 * 通过 {@link Supplier} 获取当前流体物品图标，支持基于 Menu 同步数据的判断。
 * </p>
 */
public class FluidSlot extends ResourceHandlerSlot {

    private final Supplier<ItemStack> fluidItemSupplier;

    public FluidSlot(ResourceHandler<ItemResource> handler,
                     IndexModifier<ItemResource> slotModifier,
                     int handlerSlot, int xPosition, int yPosition,
                     Supplier<ItemStack> fluidItemSupplier) {
        super(handler, slotModifier, handlerSlot, xPosition, yPosition);
        this.fluidItemSupplier = fluidItemSupplier;
    }

    @Override
    protected ItemStack getStackCopy() {
        return fluidItemSupplier.get();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
