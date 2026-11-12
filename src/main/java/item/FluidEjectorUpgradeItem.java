package item;

import blockentity.MachineBlockEntity;
import fluid.FluidProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Consumer;

/**
 * 流体输出升级物品
 * <p>
 * 安装到机器后，自动将机器中的流体输出到相邻的FluidProvider接口机器。
 * 无需GUI，直接放入升级槽即可生效。
 * </p>
 */
public class FluidEjectorUpgradeItem extends UpgradeItem {

    /** 流体传输间隔（tick） */
    public static final int TRANSFER_INTERVAL = 20; // 每秒传输一次

    /** 每次传输量（mB） */
    public static final int TRANSFER_AMOUNT = 1000; // 每次传输1桶

    public FluidEjectorUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean needsBinding() { return false; }

    /**
     * 尝试将流体从机器输出到相邻机器
     *
     * @param upgradeStack 升级物品栈
     * @param machine 安装了升级的机器
     */
    public static void tryEjectFluid(ItemStack upgradeStack, MachineBlockEntity machine) {
        if (machine.getLevel() == null || machine.getLevel().isClientSide()) return;

        // 检查机器是否实现了FluidProvider接口
        if (!(machine instanceof FluidProvider fluidProvider)) return;
        if (fluidProvider.getStoredFluid() <= 0) return;

        BlockPos machinePos = machine.getBlockPos();

        // 遍历所有方向，寻找相邻的FluidProvider机器
        for (Direction direction : Direction.values()) {
            if (fluidProvider.getStoredFluid() <= 0) break;

            BlockPos adjPos = machinePos.relative(direction);
            BlockEntity adjBE = machine.getLevel().getBlockEntity(adjPos);

            if (adjBE instanceof FluidProvider adjProvider) {
                // 计算传输量
                int maxTransfer = Math.min(TRANSFER_AMOUNT, fluidProvider.getStoredFluid());
                int canReceive = adjProvider.getMaxStoredFluid() - adjProvider.getStoredFluid();
                int transferAmount = Math.min(maxTransfer, canReceive);

                if (transferAmount > 0) {
                    // 从机器提取流体
                    int extracted = fluidProvider.extractFluid(transferAmount);
                    // 向相邻机器输入流体
                    int inserted = adjProvider.insertFluid(extracted);
                    // 如果输入量小于提取量，需要回退差额
                    if (inserted < extracted) {
                        fluidProvider.insertFluid(extracted - inserted);
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        builder.accept(Component.literal("功能: 自动输出流体").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("效果: 将流体输出到相邻机器").withStyle(ChatFormatting.GREEN));
        builder.accept(Component.literal("传输速率: " + TRANSFER_AMOUNT + " mB/次").withStyle(ChatFormatting.YELLOW));
        builder.accept(Component.literal("传输间隔: " + (TRANSFER_INTERVAL / 20.0) + " 秒").withStyle(ChatFormatting.YELLOW));
    }
}
