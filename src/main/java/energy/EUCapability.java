package energy;





import net.minecraft.core.Direction;


import net.minecraft.resources.Identifier;


import net.neoforged.neoforge.capabilities.BlockCapability;


/**


 * EU能量能力接口


 * <p>


 * 这是NeoForge能力系统的EU能量专用接口。


 * 用于在方块实体之间传输EU能量，与FE系统并行存在。


 * </p>


 */


public interface EUCapability {





    /**


     * EU能量能力的唯一标识（方块能力，支持方向上下文）


     * 用于查询方块实体的EU能量存储


     * createSided 方法自动处理 @Nullable Direction 上下文


     */


    public static final BlockCapability<EUCapability, Direction> BLOCK =


        BlockCapability.createSided(


            Identifier.fromNamespaceAndPath("iuce", "eu_energy"),


            EUCapability.class


        );





    /**


     * 向存储中添加能量


     *


     * @param maxReceive 最大接收量 (EU)


     * @param simulate   如果为true，仅模拟不实际改变


     * @return 实际接收的能量值


     */


    long receiveEnergy(long maxReceive, boolean simulate);





    /**


     * 从存储中提取能量


     *


     * @param maxExtract 最大提取量 (EU)


     * @param simulate   如果为true，仅模拟不实际改变


     * @return 实际提取的能量值


     */


    long extractEnergy(long maxExtract, boolean simulate);





    /**


     * 获取当前存储的能量


     *


     * @return 当前能量值 (EU)


     */


    long getEnergyStored();





    /**


     * 获取最大存储容量


     *


     * @return 最大能量值 (EU)


     */


    long getMaxEnergyStored();





    /**


     * 获取运行电压等级


     *


     * @return 电压等级


     */


    EUVoltage getVoltage();





    /**


     * 获取最大传输速率


     *


     * @return 最大传输速率 (EU/t)


     */


    long getMaxTransferRate();





    /**


     * 检查是否可以接收能量


     *


     * @return 如果可以接收返回true


     */


    default boolean canReceive() {


        return true;


    }





    /**


     * 检查是否可以提取能量


     *


     * @return 如果可以提取返回true


     */


    default boolean canExtract() {


        return true;


    }





    /**


     * 检查存储是否已满


     *


     * @return 如果已满返回true


     */


    default boolean isFull() {


        return getEnergyStored() >= getMaxEnergyStored();


    }





    /**


     * 检查存储是否为空


     *


     * @return 如果为空返回true


     */


    default boolean isEmpty() {


        return getEnergyStored() <= 0;


    }





    /**


     * 获取能量填充比例


     *


     * @return 0.0到1.0之间的填充比例


     */


    default double getEnergyPercentage() {


        long max = getMaxEnergyStored();


        if (max <= 0) return 0;


        return (double) getEnergyStored() / max;


    }





    /**


     * 获取能量显示字符串（用于UI显示）


     *


     * @return 格式化的能量信息，如 "512/1000 EU"


     */


    default String getEnergyDisplay() {


        return String.format("%d/%d EU", getEnergyStored(), getMaxEnergyStored());


    }





    /**


     * 获取详细的能量信息（用于调试和高级UI）


     *


     * @return 包含电压等级的详细信息


     */


    default String getDetailedInfo() {


        return String.format("%d/%d EU (%s, %d EU/t)", 


            getEnergyStored(), 


            getMaxEnergyStored(), 


            getVoltage().getName(),


            getMaxTransferRate());


    }


}