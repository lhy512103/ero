package item;

import blockentity.MachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

/**
 * 可绑定机器的升级物品基类
 * <p>
 * Shift+右键机器方块可绑定到目标机器位置，
 * 悬停时显示已绑定的机器信息。
 * </p>
 */
public class UpgradeItem extends Item {

    public UpgradeItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    /** Whether this upgrade must be bound to a target machine (sneak + right-click) before it does anything. */
    protected boolean needsBinding() { return true; }

    /**
     * Runs before the block is activated, so right-clicking a machine installs the upgrade
     * instead of opening the machine's GUI. Sneaking is left to {@link #useOn} for binding.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MachineBlockEntity machine)) return InteractionResult.PASS;
        var inventory = machine.getItemHandler();
        if (inventory == null || machine.getUpgradeSlotCount() <= 0) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (needsBinding()) {
            BlockPos bound = getBoundPos(stack);
            if (bound == null) {
                player.sendOverlayMessage(Component.translatable("message.iuce.upgrade.bind_first").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.FAIL;
            }
            if (bound.equals(context.getClickedPos())) {
                player.sendOverlayMessage(Component.translatable("message.iuce.upgrade.self_target").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.FAIL;
            }
        }
        var resource = net.neoforged.neoforge.transfer.item.ItemResource.of(stack);
        int start = machine.getUpgradeSlotStart();
        for (int slot = start; slot < start + machine.getUpgradeSlotCount(); slot++) {
            if (inventory.getAmountAsInt(slot) > 0) continue;
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                if (inventory.insert(slot, resource, 1, transaction) != 1) continue;
                transaction.commit();
            }
            Component name = stack.getHoverName();
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
            machine.setChanged();
            player.sendOverlayMessage(Component.translatable("message.iuce.upgrade.installed", name).withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }
        player.sendOverlayMessage(Component.translatable("message.iuce.upgrade.no_slot").withStyle(ChatFormatting.RED));
        return InteractionResult.FAIL;
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || !needsBinding()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);

        if (blockEntity instanceof MachineBlockEntity machine) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("BoundX", pos.getX());
            tag.putInt("BoundY", pos.getY());
            tag.putInt("BoundZ", pos.getZ());
            tag.putString("BoundType", machine.getBlockState().getBlock().getDescriptionId());
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            player.sendSystemMessage(
                Component.translatable("message.iuce.upgrade.bound",
                    machine.getBlockState().getBlock().getName().withStyle(ChatFormatting.GOLD),
                    Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN)
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("BoundX")) {
                String type = tag.getStringOr("BoundType", "");
                int x = tag.getIntOr("BoundX", 0);
                int y = tag.getIntOr("BoundY", 0);
                int z = tag.getIntOr("BoundZ", 0);

                builder.accept(Component.translatable("tooltip.iuce.upgrade.bound_machine",
                    Component.translatable(type).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
                builder.accept(Component.translatable("tooltip.iuce.upgrade.bound_pos",
                    Component.literal(x + ", " + y + ", " + z).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
            }
        } else if (needsBinding()) {
            builder.accept(Component.translatable("tooltip.iuce.upgrade.bind_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        builder.accept(Component.translatable("tooltip.iuce.upgrade.install_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 获取物品绑定的机器位置
     *
     * @param stack 物品栈
     * @return 绑定的位置，未绑定返回null
     */
    public static BlockPos getBoundPos(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("BoundX")) {
                return new BlockPos(tag.getIntOr("BoundX", 0), tag.getIntOr("BoundY", 0), tag.getIntOr("BoundZ", 0));
            }
        }
        return null;
    }
}
