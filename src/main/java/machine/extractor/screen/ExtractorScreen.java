package machine.extractor.screen;

import client.gui.MachineScreen;
import machine.extractor.menu.ExtractorMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The extractor: the press squeezes the charge over a sieve and the sap drips into the trough below. */
public class ExtractorScreen extends MachineScreen<ExtractorMenu> {
    public static final Window WINDOW = new Window(72, 35, 32, 24, Mode.UP, 4, true);

    public ExtractorScreen(ExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "extractor", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(0), menu.getMaxProgress(0));
    }
}
