package machine.compressor.menu;

import init.ModMenus;
import machine.SingleInputMachineMenu;
import machine.compressor.CompressorBlockEntity;
import net.minecraft.world.entity.player.Inventory;

public class CompressorMenu extends SingleInputMachineMenu<CompressorBlockEntity> {
    public CompressorMenu(int containerId, Inventory playerInv, CompressorBlockEntity blockEntity) {
        super(ModMenus.COMPRESSOR.get(), containerId, playerInv, blockEntity);
    }
}
