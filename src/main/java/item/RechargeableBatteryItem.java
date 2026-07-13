package item;

import energy.EUVoltage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.Consumer;

/**
 * 可充电电池物品基类
 * <p>
 * 支持EU能量的存储、接收和提取。
 * 能量数据通过物品的CUSTOM_DATA组件持久化存储。
 * </p>
 */
public class RechargeableBatteryItem extends Item {

    /** 能量存储容量 (EU) */
    private final long capacity;

    /** 最大传输速率 (EU/t) */
    private final long maxTransfer;

    /** 电压等级 */
    private final EUVoltage voltage;

    /**
     * 创建可充电电池物品
     *
     * @param properties 物品属性
     * @param capacity   能量容量 (EU)
     * @param voltage    电压等级
     */
    public RechargeableBatteryItem(Properties properties, long capacity, EUVoltage voltage) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.maxTransfer = voltage.getMaxVoltage();
        this.voltage = voltage;
    }

    /**
     * 获取能量存储容量
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * 获取最大传输速率
     */
    public long getMaxTransfer() {
        return maxTransfer;
    }

    /**
     * 获取电压等级
     */
    public EUVoltage getVoltage() {
        return voltage;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long energy = getEnergy(stack);
        if (energy <= 0 || capacity <= 0) return 0;
        return (int) (13 * energy / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00; // 绿色
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);

        long energy = getEnergy(stack);
        builder.accept(Component.literal(formatEnergy(energy) + " / " + formatEnergy(capacity) + " EU")
            .withStyle(ChatFormatting.GREEN));
        builder.accept(Component.literal(voltage.getName() + " (" + maxTransfer + " EU/t)")
            .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 格式化能量值显示
     */
    public static String formatEnergy(long energy) {
        if (energy >= 1_000_000_000L) {
            return String.format("%.1fG", energy / 1_000_000_000.0);
        } else if (energy >= 1_000_000L) {
            return String.format("%.1fM", energy / 1_000_000.0);
        } else if (energy >= 1_000L) {
            return String.format("%.1fK", energy / 1_000.0);
        }
        return String.valueOf(energy);
    }

    /**
     * 获取物品栈中存储的能量值
     */
    public static long getEnergy(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getLongOr("Energy", 0L);
        }
        return 0L;
    }

    /**
     * 设置物品栈中的能量值
     */
    public static void setEnergy(ItemStack stack, long energy) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putLong("Energy", energy);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 为指定物品栈创建EnergyHandler能力
     */
    public EnergyHandler createEnergyHandler(ItemStack stack) {
        return new BatteryEnergyHandler(stack, capacity, maxTransfer);
    }

    /**
     * 电池能量处理器实现
     */
    public static class BatteryEnergyHandler implements EnergyHandler {
        private final ItemStack stack;
        private final long capacity;
        private final long maxTransfer;

        public BatteryEnergyHandler(ItemStack stack, long capacity, long maxTransfer) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxTransfer = maxTransfer;
        }

        @Override
        public long getCapacityAsLong() {
            return capacity;
        }

        @Override
        public long getAmountAsLong() {
            return getEnergy(stack);
        }

        @Override
        public int insert(int maxAmount, TransactionContext transaction) {
            long current = getEnergy(stack);
            long space = capacity - current;
            long toInsert = Math.min(maxAmount, Math.min(maxTransfer, space));

            if (toInsert <= 0) return 0;

            // 直接修改能量值
            setEnergy(stack, current + toInsert);
            return (int) toInsert;
        }

        @Override
        public int extract(int maxAmount, TransactionContext transaction) {
            long current = getEnergy(stack);
            long toExtract = Math.min(maxAmount, Math.min(maxTransfer, current));

            if (toExtract <= 0) return 0;

            // 直接修改能量值
            setEnergy(stack, current - toExtract);
            return (int) toExtract;
        }
    }
}
