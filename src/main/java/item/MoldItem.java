package item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A mold for the metal former. The mold in the machine decides what it makes
 * (0 plates, 1 wires, 2 rods); molds are never used up.
 */
public class MoldItem extends Item {
    private final int mode;

    public MoldItem(Properties properties, int mode) {
        super(properties);
        this.mode = mode;
    }

    public int mode() { return mode; }

    /** The mold for a metal former mode, or null for an unknown mode. */
    public static Item forMode(int mode) {
        return switch (mode) {
            case 0 -> ModItems.PLATE_MOLD.get();
            case 1 -> ModItems.WIRE_MOLD.get();
            case 2 -> ModItems.ROD_MOLD.get();
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.iuce.mold",
            Component.translatable("tooltip.iuce.metal_former.mode." + mode)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.iuce.mold.reusable").withStyle(ChatFormatting.DARK_GRAY));
    }
}
