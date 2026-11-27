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
        if (player != null && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof machine.process.ProcessMachineBlockEntity source
                && source.def().cardSlot() && !player.isShiftKeyDown()) {
            if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            var encoded = source.getItemHandler().getResource(source.def().cardIndex()).toStack(1);
            var card = encoded.get(machine.process.ProcessCards.TYPE.get());
            if (card == null) return InteractionResult.PASS;
            int copies = 0;
            for (var side : net.minecraft.core.Direction.values()) {
                var next = context.getClickedPos().relative(side);
                if (!context.getLevel().hasChunkAt(next) ||
                        !(context.getLevel().getBlockEntity(next) instanceof machine.process.ProcessMachineBlockEntity target)
                        || !target.def().id().equals(source.def().id())
                        || target.getItemHandler().getAmountAsInt(target.def().cardIndex()) > 0) continue;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack blank = player.getInventory().getItem(slot);
                    if (!blank.is(ModItems.PROCESS_CARD.get()) || blank.has(machine.process.ProcessCards.TYPE.get())) continue;
                    target.getItemHandler().set(target.def().cardIndex(), net.neoforged.neoforge.transfer.item.ItemResource.of(machine.process.ProcessCards.write(card)), 1);
                    blank.shrink(1);
                    player.getInventory().setChanged();
                    copies++;
                    break;
                }
            }
            player.sendOverlayMessage(Component.translatable("message.iuce.configurator.cards", copies));
            return InteractionResult.SUCCESS;
        }
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LogisticsPortBlockEntity port)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Mode", port.getMode());
            tag.putInt("Channel", port.getChannel());
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
        port.setChannel(tag.getIntOr("Channel", 0));
        player.sendOverlayMessage(Component.translatable("message.iuce.configurator.pasted", LogisticsPortBlock.describe(port)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.iuce.configurator.1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.iuce.configurator.2").withStyle(ChatFormatting.GRAY));
    }
}
