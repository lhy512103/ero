package machine.macerator.screen;

import client.gui.MachineScreen;
import machine.macerator.menu.MaceratorMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The macerator: the ore drops from the hopper between two toothed rolls and the dust piles up below them. */
public class MaceratorScreen extends MachineScreen<MaceratorMenu> {
    public static final Window WINDOW = new Window(72, 35, 32, 24, Mode.UP, 4, true);

    public MaceratorScreen(MaceratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "macerator", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(0), menu.getMaxProgress(0));
    }
}
