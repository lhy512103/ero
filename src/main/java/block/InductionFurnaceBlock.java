package block;

import blockentity.ModBlockEntities;
import machine.induction_furnace.InductionFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 感应炉方块
 * <p>
 * 为相邻电炉提供效率加成和能量共享，右键打开合金炉GUI。
 * </p>
 */
public class InductionFurnaceBlock extends MachineBlock {

    public InductionFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties, InductionFurnaceBlockEntity::new, ModBlockEntities.INDUCTION_FURNACE);
    }
}
