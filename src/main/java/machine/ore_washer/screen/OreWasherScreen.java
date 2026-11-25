package machine.ore_washer.screen;

import client.gui.MachineScreen;
import machine.ore_washer.menu.OreWasherMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

/**
 * The ore washer: the crushed ore and the water run into the washing drum, which fills and tumbles;
 * the washed ore and the by-product leave on the right, the water comes from the tank beside them.
 */
public class OreWasherScreen extends MachineScreen<OreWasherMenu> {
    public static final Window WINDOW = new Window(73, 28, 20, 40, Mode.UP, 4, false);

    public OreWasherScreen(OreWasherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "ore_washer", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        tank(g, 126, 20, 12, 54, Fluids.WATER, Component.translatable(menu.getFluidTranslationKey()),
            menu.getWater(), menu.getMaxWater(), true);
        run(g, WINDOW, menu.getProgress(), menu.getMaxProgress());
    }
}
