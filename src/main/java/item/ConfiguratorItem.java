package item;

import block.LogisticsPortBlock;
import blockentity.LogisticsPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

/** Configurator: sneak-right-click a logistics interface to copy its settings, right-click another to paste them. */
public class ConfiguratorItem extends Item {
    public ConfiguratorItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LogisticsPortBlockEntity port)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Mode", port.getMode());
            if (port.getFilter() != null) tag.putString("Filter", BuiltInRegistries.ITEM.getKey(port.getFilter()).toString());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.sendOverlayMessage(Component.translatable("message.iuce.configurator.copied", LogisticsPortBlock.describe(port)));
            return InteractionResult.SUCCESS;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            player.sendOverlayMessage(Component.translatable("message.iuce.configurator.empty").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        CompoundTag tag = data.copyTag();
        Identifier id = Identifier.tryParse(tag.getStringOr("Filter", ""));
        Item filter = tag.contains("Filter") && id != null ? BuiltInRegistries.ITEM.getValue(id) : null;
        port.configure(tag.getIntOr("Mode", 0), filter);
        player.sendOverlayMessage(Component.translatable("message.iuce.configurator.pasted", LogisticsPortBlock.describe(port)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.iuce.configurator.1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.iuce.configurator.2").withStyle(ChatFormatting.GRAY));
    }
}
