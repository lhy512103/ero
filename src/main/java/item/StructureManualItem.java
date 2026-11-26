package item;

import block.ModBlocks;
import machine.process.ProcessCards;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Structure manual: the product of a large machine's structure page in JEI. Encoding that page in
 * an AE2 pattern encoding terminal gives a processing pattern from all of the machine's blocks to
 * one manual, so the whole set of blocks can be ordered as a single fake craft.
 */
public class StructureManualItem extends Item {
    public StructureManualItem(Properties properties) { super(properties.stacksTo(16)); }

    /** A manual for the large machine {@code structure} (a guide structure name). */
    public static ItemStack of(String structure) {
        ItemStack stack = new ItemStack(ModItems.STRUCTURE_MANUAL.get());
        stack.set(ProcessCards.STRUCTURE.get(), structure);
        return stack;
    }

    /** The controller item a guide structure name stands for. */
    public static ItemStack controller(String structure) {
        return switch (structure) {
            case "ore_plant" -> new ItemStack(ModBlocks.PLANT_CONTROLLER.get());
            case "rolling_mill" -> new ItemStack(ModBlocks.MILL_CONTROLLER.get());
            default -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("iuce", structure)));
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        String structure = stack.get(ProcessCards.STRUCTURE.get());
        if (structure == null) return super.getName(stack);
        return Component.translatable("item.iuce.structure_manual.named", controller(structure).getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.iuce.structure_manual").withStyle(ChatFormatting.GRAY));
    }
}
