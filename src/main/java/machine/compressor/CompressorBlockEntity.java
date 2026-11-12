package machine.compressor;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SingleInputMachineBlockEntity;
import machine.compressor.menu.CompressorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** 压缩机：LV，缓冲 2000 EU，运行 {@code compressor} 配方。 */
public class CompressorBlockEntity extends SingleInputMachineBlockEntity {

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR.get(), pos, state, EUVoltage.LV, 1, 4, 2000, 200);
    }

    @Override protected String recipeType() { return "compressor"; }

    @Override public String getMachineName() { return "压缩机"; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressorMenu(containerId, playerInventory, this);
    }
}
