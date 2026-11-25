package machine.storage.screen;

import client.gui.MachineScreen;
import machine.storage.menu.StorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The energy stores (battery box and up): a cabinet of cells with its charge gauge on the front, the
 * two charge slots wired into it on the left and the four equipment slots along the bottom.
 */
public class StorageScreen extends MachineScreen<StorageMenu> {
    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "storage", 87);
        // the equipment slots sit on the inventory label's row
        inventoryLabel = false;
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energyBar(g, 66, 34, 84, 14, menu.getEnergy(), menu.getMaxEnergy());
    }
}
