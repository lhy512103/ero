package item;

import blockentity.MachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Consumer;

/**
 * 弹出升级物品
 * <p>
 * Shift+右键绑定目标机器，安装到机器后自动将输出槽物品推送到目标机器。
 * 支持双向物流：当前机器输出 → 目标机器输入。
 * </p>
 */
public class EjectorUpgradeItem extends UpgradeItem {

    /** 每次弹出的最大物品数量 */
    public static final int MAX_TRANSFER_COUNT = 1;

    /** 弹出间隔（tick） */
    public static final int TRANSFER_INTERVAL = 20;

    /** 最大连接距离 */
    public static final int MAX_RANGE = 8;

    public EjectorUpgradeItem(Properties properties) {
        super(properties);
    }

    /**
     * 执行弹出逻辑
     */
    public static boolean tryEject(ItemStack upgradeStack, MachineBlockEntity currentMachine) {
        if (upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof EjectorUpgradeItem)) {
            return false;
        }

        BlockPos targetPos = getBoundPos(upgradeStack);
        if (targetPos == null) return false;

        Level level = currentMachine.getLevel();
        if (level == null || level.isClientSide()) return false;

        BlockEntity targetBe = level.getBlockEntity(targetPos);
        if (!(targetBe instanceof MachineBlockEntity targetMachine)) return false;

        if (!isWithinRange(currentMachine.getBlockPos(), targetPos, MAX_RANGE)) {
            return false;
        }

        return pushOutputToInput(currentMachine, targetMachine);
    }

    /**
     * 检查两个位置是否在指定范围内
     */
    public static boolean isWithinRange(BlockPos pos1, BlockPos pos2, int range) {
        return pos1.distSqr(pos2) <= range * range;
    }

    /**
     * 从源机器的输出槽推送到目标机器的输入槽
     * <p>
     * 机器槽位结构：输入槽(0~n-1) | 输出槽(n~2n-1) | 升级槽(2n~2n+m-1)
     * </p>
     */
    public static boolean pushOutputToInput(MachineBlockEntity source, MachineBlockEntity target) {
        ItemStacksResourceHandler sourceHandler = source.getItemHandler();
        ItemStacksResourceHandler targetHandler = target.getItemHandler();

        if (sourceHandler == null || targetHandler == null) return false;

        int sourceUpgradeStart = source.getUpgradeSlotStart();
        int targetUpgradeStart = target.getUpgradeSlotStart();

        // 源机器输出槽范围
        int sourceTotalSlots = sourceUpgradeStart;
        int sourceOutputStart = sourceTotalSlots / 2;
        int sourceOutputEnd = sourceTotalSlots - 1;

        // 目标机器输入槽范围
        int targetTotalSlots = targetUpgradeStart;
        int targetInputEnd = targetTotalSlots / 2 - 1;

        for (int srcSlot = sourceOutputStart; srcSlot <= sourceOutputEnd; srcSlot++) {
            ItemResource srcResource = sourceHandler.getResource(srcSlot);
            int srcAmount = sourceHandler.getAmountAsInt(srcSlot);

            if (srcResource.isEmpty() || srcAmount <= 0) continue;

            for (int tgtSlot = 0; tgtSlot <= targetInputEnd; tgtSlot++) {
                try (Transaction tx = Transaction.openRoot()) {
                    int extracted = sourceHandler.extract(srcSlot, srcResource, MAX_TRANSFER_COUNT, tx);
                    if (extracted > 0) {
                        int inserted = targetHandler.insert(tgtSlot, srcResource, extracted, tx);
                        if (inserted > 0) {
                            tx.commit();
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查是否应该执行弹出（基于间隔）
     */
    public static boolean shouldEject(ItemStack upgradeStack, long currentTick) {
        return currentTick % TRANSFER_INTERVAL == 0;
    }

    /**
     * 获取绑定目标的机器实例
     */
    public static MachineBlockEntity getBoundMachine(ItemStack upgradeStack, Level level) {
        BlockPos targetPos = getBoundPos(upgradeStack);
        if (targetPos == null) return null;

        BlockEntity targetBe = level.getBlockEntity(targetPos);
        if (!(targetBe instanceof MachineBlockEntity targetMachine)) return null;

        return targetMachine;
    }

    /**
     * 检查连接是否有效
     */
    public static boolean isLinkValid(ItemStack upgradeStack, MachineBlockEntity currentMachine) {
        Level level = currentMachine.getLevel();
        if (level == null) return false;

        MachineBlockEntity targetMachine = getBoundMachine(upgradeStack, level);
        if (targetMachine == null) return false;

        return isWithinRange(currentMachine.getBlockPos(), targetMachine.getBlockPos(), MAX_RANGE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        BlockPos boundPos = getBoundPos(stack);
        if (boundPos != null) {
            builder.accept(Component.literal("功能: 自动推送输出槽物品到目标机器").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.literal("方向: 当前机器输出 → 目标机器输入").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.literal("范围: " + MAX_RANGE + "格").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.literal("间隔: 1秒").withStyle(ChatFormatting.GRAY));
        }
    }
}
