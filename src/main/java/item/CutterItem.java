package item;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 剪刀 - 用于将板切割成导线。
 * <p>
 * 拥有128点耐久度，每次合成消耗1点耐久，合成后工具返回（带损伤）。
 * </p>
 */
public class CutterItem extends Item {

    public static final int MAX_DAMAGE = 128;

    public CutterItem(Properties properties) {
        super(properties.durability(MAX_DAMAGE));
    }

    @Override
    public @Nullable ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        int damage = instance.getOrDefault(DataComponents.DAMAGE, 0) + 1;
        if (damage >= MAX_DAMAGE) {
            return null;
        }
        return new ItemStackTemplate(this, 1,
            DataComponentPatch.builder().set(DataComponents.DAMAGE, damage).build());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
        int remaining = MAX_DAMAGE - damage;
        builder.accept(Component.translatable("item.ero.cutter.durability", remaining, MAX_DAMAGE));
    }
}
