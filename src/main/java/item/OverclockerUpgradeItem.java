package item;

import blockentity.MachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.function.Consumer;

/**
 * 超频升级物品
 * <p>
 * 安装到机器后，减少处理时间但增加能量消耗。
 * 效果可叠加，每个超频升级减少 25% 处理时间，增加 50% 能量消耗。
 * </p>
 */
public class OverclockerUpgradeItem extends UpgradeItem {

    /** 处理时间减少倍数 (每个超频升级减少 25%) */
    public static final double SPEED_MULTIPLIER = 0.75;

    /** 能量消耗增加倍数 (每个超频升级增加 50%) */
    public static final double ENERGY_MULTIPLIER = 1.5;

    /** 最大叠加数量 */
    public static final int MAX_STACK = 4;

    public OverclockerUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean needsBinding() { return false; }

    /**
     * 计算机器中超频升级的数量
     *
     * @param machine 目标机器
     * @return 超频升级数量
     */
    public static int countOverclockers(MachineBlockEntity machine) {
        ItemStacksResourceHandler handler = machine.getItemHandler();
        if (handler == null) return 0;

        int upgradeStart = machine.getUpgradeSlotStart();
        int upgradeCount = machine.getUpgradeSlotCount();
        int count = 0;

        for (int i = 0; i < upgradeCount; i++) {
            int slotIndex = upgradeStart + i;
            ItemResource resource = handler.getResource(slotIndex);
            int amount = handler.getAmountAsInt(slotIndex);

            if (!resource.isEmpty() && amount > 0) {
                // 直接通过 ItemResource 获取 Item 类型，避免创建 ItemStack
                if (resource.getItem() instanceof OverclockerUpgradeItem) {
                    count++;
                }
            }
        }

        return Math.min(count, MAX_STACK);
    }

    /**
     * 计算超频后的处理时间
     *
     * @param baseProcessTime 基础处理时间
     * @param overclockerCount 超频升级数量
     * @return 超频后的处理时间 (最小为 1 tick)
     */
    public static int getOverclockedProcessTime(int baseProcessTime, int overclockerCount) {
        if (overclockerCount <= 0) return baseProcessTime;

        double multiplier = Math.pow(SPEED_MULTIPLIER, overclockerCount);
        int result = (int) Math.round(baseProcessTime * multiplier);
        return Math.max(1, result);
    }

    /**
     * 计算超频后的能量消耗
     *
     * @param baseEnergyConsumption 基础能量消耗
     * @param overclockerCount 超频升级数量
     * @return 超频后的能量消耗
     */
    public static long getOverclockedEnergyConsumption(long baseEnergyConsumption, int overclockerCount) {
        if (overclockerCount <= 0) return baseEnergyConsumption;

        double multiplier = Math.pow(ENERGY_MULTIPLIER, overclockerCount);
        return (long) Math.round(baseEnergyConsumption * multiplier);
    }

    /**
     * 应用超频效果到机器
     * <p>
     * 此方法应在机器的 tick 方法中调用，用于动态调整处理时间和能量消耗。
     * </p>
     *
     * @param machine 目标机器
     * @return 超频升级数量
     */
    public static int applyOverclockerEffect(MachineBlockEntity machine) {
        int overclockerCount = countOverclockers(machine);
        if (overclockerCount <= 0) return 0;

        // 注意：实际的处理时间和能量消耗调整需要在具体机器的 tick 方法中实现
        // 这里只返回超频升级数量供机器使用
        return overclockerCount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        builder.accept(Component.literal("功能: 加速机器处理").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("效果: 减少 25% 处理时间").withStyle(ChatFormatting.GREEN));
        builder.accept(Component.literal("代价: 增加 50% 能量消耗").withStyle(ChatFormatting.RED));
        builder.accept(Component.literal("最大叠加: " + MAX_STACK + " 个").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("当前效果: 减少 " + String.format("%.0f", (1 - Math.pow(SPEED_MULTIPLIER, 1)) * 100) + "% 时间").withStyle(ChatFormatting.YELLOW));
    }
}
