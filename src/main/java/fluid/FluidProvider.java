package fluid;

import net.minecraft.world.item.ItemStack;

/**
 * 流体提供者接口
 * <p>
 * 实现此接口的方块实体可以向相邻方块提供流体，也可以接收流体。
 * </p>
 */
public interface FluidProvider {

    /**
     * 尝试从提供者中提取指定数量的流体
     *
     * @param maxAmount 最大提取量（mB）
     * @return 实际提取的流体量（mB）
     */
    int extractFluid(int maxAmount);

    /**
     * 尝试向提供者中输入指定数量的流体
     *
     * @param maxAmount 最大输入量（mB）
     * @return 实际输入的流体量（mB）
     */
    int insertFluid(int maxAmount);

    /**
     * 获取当前存储的流体量（mB）
     */
    int getStoredFluid();

    /**
     * 获取最大存储流体量（mB）
     */
    int getMaxStoredFluid();

    /**
     * 获取当前存储流体对应的物品图标（用于 GUI 显示）
     * <p>
     * 返回 ItemStack.EMPTY 表示无流体或无对应物品
     * </p>
     */
    ItemStack getFluidItem();
}