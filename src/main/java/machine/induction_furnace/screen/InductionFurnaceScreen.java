package machine.induction_furnace.screen;

import client.gui.MachineScreen;
import machine.induction_furnace.menu.InductionFurnaceMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The induction furnace: both charges fall into the crucible inside the copper coil and melt together, the alloy pours out below. */
public class InductionFurnaceScreen extends MachineScreen<InductionFurnaceMenu> {
    public static final Window WINDOW = new Window(66, 36, 44, 26, Mode.UP, 4, true);

    public InductionFurnaceScreen(InductionFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "induction_furnace", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(), menu.getMaxProgress());
    }
}
