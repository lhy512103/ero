package item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import java.util.function.Consumer;

public class WrenchItem extends Item {
    private static final int MAX_DAMAGE = 128;
    public WrenchItem(Properties properties) { super(properties.durability(MAX_DAMAGE)); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return WrenchActions.use(context, false);
    }
    @Override public InteractionResult useOn(UseOnContext context) { return WrenchActions.use(context, false); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.iuce.wrench.durability", MAX_DAMAGE - stack.getDamageValue(), MAX_DAMAGE));
        builder.accept(Component.translatable("tooltip.iuce.wrench.1"));
        builder.accept(Component.translatable("tooltip.iuce.wrench.2"));
    }
}
