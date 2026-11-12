package machine.extractor.menu;

import init.ModMenus;
import machine.SingleInputMachineMenu;
import machine.extractor.ExtractorBlockEntity;
import net.minecraft.world.entity.player.Inventory;

public class ExtractorMenu extends SingleInputMachineMenu<ExtractorBlockEntity> {
    public ExtractorMenu(int containerId, Inventory playerInv, ExtractorBlockEntity blockEntity) {
        super(ModMenus.EXTRACTOR.get(), containerId, playerInv, blockEntity);
    }
}
