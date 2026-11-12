package machine.macerator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SingleInputMachineBlockEntity;
import machine.macerator.menu.MaceratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** 打粉机：LV，缓冲 2000 EU，运行 {@code macerator} 配方。 */
public class MaceratorBlockEntity extends SingleInputMachineBlockEntity {

    public MaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACERATOR.get(), pos, state, EUVoltage.LV, 1, 4, 2000, 200);
    }

    @Override protected String recipeType() { return "macerator"; }

    @Override public String getMachineName() { return "打粉机"; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MaceratorMenu(containerId, playerInventory, this);
    }
}
