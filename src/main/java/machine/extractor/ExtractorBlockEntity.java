package machine.extractor;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SingleInputMachineBlockEntity;
import machine.extractor.menu.ExtractorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** 提取机：LV，缓冲 1500 EU，运行 {@code extractor} 配方。 */
public class ExtractorBlockEntity extends SingleInputMachineBlockEntity {

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTOR.get(), pos, state, EUVoltage.LV, 1, 4, 1500, 200);
    }

    @Override protected String recipeType() { return "extractor"; }

    @Override public String getMachineName() { return "提取机"; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExtractorMenu(containerId, playerInventory, this);
    }
}
