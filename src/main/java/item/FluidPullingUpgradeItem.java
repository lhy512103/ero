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
 * 流体输入升级物品
 * <p>
 * 安装到机器后，自动从相邻的FluidProvider接口机器中输入流体。
 * 无需GUI，直接放入升级槽即可生效。
 * </p>
 */
public class FluidPullingUpgradeItem extends UpgradeItem {

    /** 流体传输间隔（tick） */
    public static final int TRANSFER_INTERVAL = 20; // 每秒传输一次

    /** 每次传输量（mB） */
    public static final int TRANSFER_AMOUNT = 1000; // 每次传输1桶

    public FluidPullingUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        // Shift+右键执行绑定逻辑
        if (player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        // 普通右键：尝试放入升级槽
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = context.getLevel().getBlockEntity(pos);

        if (blockEntity instanceof MachineBlockEntity machine) {
            ItemStacksResourceHandler itemHandler = machine.getItemHandler();
            if (itemHandler != null) {
                int upgradeSlotCount = machine.getUpgradeSlotCount();
                int upgradeSlotStart = machine.getUpgradeSlotStart();

                // 查找空的升级槽
                for (int i = 0; i < upgradeSlotCount; i++) {
                    int slotIndex = upgradeSlotStart + i;
                    ItemResource resource = itemHandler.getResource(slotIndex);
                    int amount = itemHandler.getAmountAsInt(slotIndex);

                    if (resource.isEmpty() || amount <= 0) {
                        // 找到空槽，放入升级物品
                        ItemStack stack = context.getItemInHand();
                        ItemResource upgradeResource = ItemResource.of(stack);
                        try (Transaction tx = Transaction.openRoot()) {
                            int inserted = itemHandler.insert(slotIndex, upgradeResource, 1, tx);
                            if (inserted > 0) {
                                tx.commit();
                                stack.shrink(1);
                                player.sendSystemMessage(
                                    Component.literal("已安装流体输入升级").withStyle(ChatFormatting.GREEN)
                                );
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }

                player.sendSystemMessage(
                    Component.literal("没有空的升级槽").withStyle(ChatFormatting.RED)
                );
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * 尝试从相邻机器中输入流体到本机器
     *
     * @param upgradeStack 升级物品栈
     * @param machine 安装了升级的机器
     */
    public static void tryPull(ItemStack upgradeStack, MachineBlockEntity machine) {
        if (machine.getLevel() == null || machine.getLevel().isClientSide()) return;

        // 检查机器是否实现了FluidProvider接口
        if (!(machine instanceof FluidProvider fluidProvider)) return;
        if (fluidProvider.getStoredFluid() >= fluidProvider.getMaxStoredFluid()) return;

        BlockPos machinePos = machine.getBlockPos();

        // 遍历所有方向，寻找相邻的FluidProvider机器
        for (Direction direction : Direction.values()) {
            if (fluidProvider.getStoredFluid() >= fluidProvider.getMaxStoredFluid()) break;

            BlockPos adjPos = machinePos.relative(direction);
            BlockEntity adjBE = machine.getLevel().getBlockEntity(adjPos);

            if (adjBE instanceof FluidProvider adjProvider) {
                if (adjProvider.getStoredFluid() <= 0) continue;

                // 计算传输量
                int canReceive = fluidProvider.getMaxStoredFluid() - fluidProvider.getStoredFluid();
                int maxTransfer = Math.min(TRANSFER_AMOUNT, canReceive);
                int available = adjProvider.getStoredFluid();
                int transferAmount = Math.min(maxTransfer, available);

                if (transferAmount > 0) {
                    // 从相邻机器提取流体
                    int extracted = adjProvider.extractFluid(transferAmount);
                    // 向本机器输入流体
                    int inserted = fluidProvider.insertFluid(extracted);
                    // 如果输入量小于提取量，需要回退差额
                    if (inserted < extracted) {
                        adjProvider.insertFluid(extracted - inserted);
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        builder.accept(Component.literal("功能: 自动输入流体").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("效果: 从相邻机器输入流体").withStyle(ChatFormatting.GREEN));
        builder.accept(Component.literal("传输速率: " + TRANSFER_AMOUNT + " mB/次").withStyle(ChatFormatting.YELLOW));
        builder.accept(Component.literal("传输间隔: " + (TRANSFER_INTERVAL / 20.0) + " 秒").withStyle(ChatFormatting.YELLOW));
        builder.accept(Component.literal("使用方式: 右键机器安装").withStyle(ChatFormatting.GOLD));
    }
}
