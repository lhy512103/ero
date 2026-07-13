package blockentity;

import energy.EUEnergyStorage;

/**
 * 能量显示接口
 * <p>
 * 实现此接口的方块实体可以显示EU能量信息。
 * 用于Jade兼容、UI显示等场景。
 * </p>
 */
public interface EnergyDisplay {

    /**
     * 获取能量存储实例
     *
     * @return EU能量存储
     */
    EUEnergyStorage getEnergyStorage();
}
