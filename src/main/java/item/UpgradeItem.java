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
        super(properties.stacksTo(1));
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);

        if (blockEntity instanceof MachineBlockEntity machine) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("BoundX", pos.getX());
            tag.putInt("BoundY", pos.getY());
            tag.putInt("BoundZ", pos.getZ());
            tag.putString("BoundType", machine.getMachineName());
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            player.sendSystemMessage(
                Component.literal("已绑定到 ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(machine.getMachineName()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" @ ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW))
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

                builder.accept(Component.literal("已绑定机器: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(type).withStyle(ChatFormatting.GOLD)));
                builder.accept(Component.literal("位置: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(x + ", " + y + ", " + z).withStyle(ChatFormatting.YELLOW)));
            }
        } else {
            builder.accept(Component.literal("Shift+右键机器以绑定").withStyle(ChatFormatting.DARK_GRAY));
        }
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
