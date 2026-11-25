package machine.compressor.screen;

import client.gui.MachineScreen;
import machine.compressor.menu.CompressorMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The compressor: the ram comes down in the die between the columns and presses the charge into a block. */
public class CompressorScreen extends MachineScreen<CompressorMenu> {
    public static final Window WINDOW = new Window(72, 35, 32, 24, Mode.PHASE, 8, false);

    public CompressorScreen(CompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "compressor", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(0), menu.getMaxProgress(0));
    }
}
