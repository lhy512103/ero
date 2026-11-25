package machine.electric_furnace.screen;

import client.gui.MachineScreen;
import machine.electric_furnace.menu.ElectricFurnaceMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The electric furnace: the charge sits on the hearth between glowing heating coils and turns from raw to done. */
public class ElectricFurnaceScreen extends MachineScreen<ElectricFurnaceMenu> {
    public static final Window WINDOW = new Window(72, 35, 32, 24, Mode.UP, 4, true);

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "electric_furnace", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(), menu.getMaxProgress());
    }
}
