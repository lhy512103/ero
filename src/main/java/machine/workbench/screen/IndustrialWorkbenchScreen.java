package machine.workbench.screen;

import client.gui.MachineScreen;
import machine.process.ProgressStyle.Mode;
import machine.workbench.menu.IndustrialWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The industrial workbench: the crafting grid on its steel tray, the arrow filling towards the
 * product, the battery wired up to the energy gauge and the two storage rows underneath.
 */
public class IndustrialWorkbenchScreen extends MachineScreen<IndustrialWorkbenchMenu> {
    public static final Window WINDOW = new Window(90, 32, 22, 15, Mode.RIGHT, 0, false);

    public IndustrialWorkbenchScreen(IndustrialWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200, "industrial_workbench", 119);
        // the grid starts on the title's row and the storage rows reach the inventory label's
        titleLabel = false;
        inventoryLabel = false;
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 18, 7, 36, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(), menu.getMaxProgress());
    }
}
