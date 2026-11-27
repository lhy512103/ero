package machine.electrolyzer.screen;

import client.gui.MachineScreen;
import machine.electrolyzer.menu.ElectrolyzerMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

/**
 * The electrolyzer: the water comes in through the filler on top into the cell, the current runs
 * through it and the gas bubbles up at the electrodes over the three product slots below.
 */
public class ElectrolyzerScreen extends MachineScreen<ElectrolyzerMenu> {
    public static final Window WINDOW = new Window(44, 40, 88, 13, Mode.UP, 4, false);

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "electrolyzer", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        tank(g, 25, 22, 10, 48, Fluids.WATER, menu.getFluid(), menu.getMaxFluid(), true);
        run(g, WINDOW, menu.getProgress(), menu.getMaxProgress());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // the filler slot shows a water bucket while there is water: name the water, not the bucket
        if (hoveredSlot != null && hoveredSlot.index == 0 && hoveredSlot.hasItem()) {
            g.setComponentTooltipForNextFrame(font, List.of(Fluids.WATER.getFluidType().getDescription(),
                Component.literal(menu.getFluid() + " / " + menu.getMaxFluid() + " mB")), mouseX, mouseY);
            return;
        }
        super.extractTooltip(g, mouseX, mouseY);
    }
}
