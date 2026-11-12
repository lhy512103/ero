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
 * 抽取升级物品
 * <p>
 * Shift+右键绑定目标机器，安装到机器后自动从目标机器的输入槽抽取物品。
 * 复用弹出升级的配置常量和工具方法。
 * </p>
 */
public class PullingUpgradeItem extends UpgradeItem {

    public PullingUpgradeItem(Properties properties) {
        super(properties);
    }

    /**
     * 执行抽取逻辑
     */
    public static boolean tryPull(ItemStack upgradeStack, MachineBlockEntity currentMachine) {
        if (upgradeStack.isEmpty() || !(upgradeStack.getItem() instanceof PullingUpgradeItem)) {
            return false;
        }

        BlockPos targetPos = getBoundPos(upgradeStack);
        if (targetPos == null) return false;

        Level level = currentMachine.getLevel();
        if (level == null || level.isClientSide()) return false;

        BlockEntity targetBe = level.getBlockEntity(targetPos);
        if (!(targetBe instanceof MachineBlockEntity targetMachine)) return false;

        if (!EjectorUpgradeItem.isWithinRange(currentMachine.getBlockPos(), targetPos, EjectorUpgradeItem.MAX_RANGE)) {
            return false;
        }

        return pullFromInput(targetMachine, currentMachine);
    }

    /**
     * 从目标机器的输入槽抽取物品到当前机器的输入槽
     * <p>
     * 机器槽位结构：输入槽(0~n-1) | 输出槽(n~2n-1) | 升级槽(2n~2n+m-1)
     * </p>
     */
    private static boolean pullFromInput(MachineBlockEntity source, MachineBlockEntity target) {
        ItemStacksResourceHandler sourceHandler = source.getItemHandler();
        ItemStacksResourceHandler targetHandler = target.getItemHandler();

        if (sourceHandler == null || targetHandler == null) return false;

        for (int srcSlot : source.getInputSlots()) {
            ItemResource srcResource = sourceHandler.getResource(srcSlot);
            int srcAmount = sourceHandler.getAmountAsInt(srcSlot);

            if (srcResource.isEmpty() || srcAmount <= 0) continue;

            for (int tgtSlot : target.getInputSlots()) {
                try (Transaction tx = Transaction.openRoot()) {
                    int extracted = sourceHandler.extract(srcSlot, srcResource, EjectorUpgradeItem.MAX_TRANSFER_COUNT, tx);
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
     * 检查是否应该执行抽取（基于间隔）
     */
    public static boolean shouldPull(ItemStack upgradeStack, long currentTick) {
        return currentTick % EjectorUpgradeItem.TRANSFER_INTERVAL == 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        BlockPos boundPos = getBoundPos(stack);
        if (boundPos != null) {
            builder.accept(Component.translatable("tooltip.iuce.pulling_upgrade.1").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable("tooltip.iuce.pulling_upgrade.2").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable("tooltip.iuce.upgrade.range", EjectorUpgradeItem.MAX_RANGE).withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable("tooltip.iuce.upgrade.interval").withStyle(ChatFormatting.GRAY));
        }
    }
}
