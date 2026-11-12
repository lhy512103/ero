package item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

public class ElectricWrenchItem extends Item {
    private static final long MAX_ENERGY = 10_000L;
    public ElectricWrenchItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return WrenchActions.use(context, true);
    }
    @Override public InteractionResult useOn(UseOnContext context) { return WrenchActions.use(context, true); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) {
        return (int) Math.clamp(13 * RechargeableBatteryItem.getEnergy(stack) / MAX_ENERGY, 0, 13);
    }
    @Override public int getBarColor(ItemStack stack) { return 0x00FF00; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.literal(RechargeableBatteryItem.formatEnergy(RechargeableBatteryItem.getEnergy(stack))
            + " / " + RechargeableBatteryItem.formatEnergy(MAX_ENERGY) + " EU").withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable("tooltip.iuce.electric_wrench.1"));
        builder.accept(Component.translatable("tooltip.iuce.electric_wrench.2"));
    }
}
