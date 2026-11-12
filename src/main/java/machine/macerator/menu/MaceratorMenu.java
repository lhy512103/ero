package machine.macerator.menu;

import init.ModMenus;
import machine.SingleInputMachineMenu;
import machine.macerator.MaceratorBlockEntity;
import net.minecraft.world.entity.player.Inventory;

public class MaceratorMenu extends SingleInputMachineMenu<MaceratorBlockEntity> {
    public MaceratorMenu(int containerId, Inventory playerInv, MaceratorBlockEntity blockEntity) {
        super(ModMenus.MACERATOR.get(), containerId, playerInv, blockEntity);
    }
}
