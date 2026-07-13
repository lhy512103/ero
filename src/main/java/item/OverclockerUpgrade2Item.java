package item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * 超频升级 MK2 物品
 * <p>
 * 高级超频升级，减少 60% 处理时间，增加 200% 能量消耗。
 * </p>
 */
public class OverclockerUpgrade2Item extends OverclockerUpgradeItem {

    /** 处理时间减少倍数 (减少 60%) */
    public static final double MK2_SPEED_MULTIPLIER = 0.40;

    /** 能量消耗增加倍数 (增加 200%) */
    public static final double MK2_ENERGY_MULTIPLIER = 3.0;

    public OverclockerUpgrade2Item(Properties properties) {
        super(properties);
    }

    /**
     * 计算 MK2 超频后的处理时间
     */
    public static int getOverclockedProcessTime(int baseProcessTime, int overclockerCount) {
        if (overclockerCount <= 0) return baseProcessTime;

        double multiplier = Math.pow(MK2_SPEED_MULTIPLIER, overclockerCount);
        int result = (int) Math.round(baseProcessTime * multiplier);
        return Math.max(1, result);
    }

    /**
     * 计算 MK2 超频后的能量消耗
     */
    public static long getOverclockedEnergyConsumption(long baseEnergyConsumption, int overclockerCount) {
        if (overclockerCount <= 0) return baseEnergyConsumption;

        double multiplier = Math.pow(MK2_ENERGY_MULTIPLIER, overclockerCount);
        return (long) Math.round(baseEnergyConsumption * multiplier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        builder.accept(Component.literal("功能: 加速机器处理 (MK2)").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("效果: 减少 60% 处理时间").withStyle(ChatFormatting.GREEN));
        builder.accept(Component.literal("代价: 增加 200% 能量消耗").withStyle(ChatFormatting.RED));
        builder.accept(Component.literal("最大叠加: " + MAX_STACK + " 个").withStyle(ChatFormatting.GRAY));
    }
}
