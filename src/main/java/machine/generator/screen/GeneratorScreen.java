package machine.generator.screen;

import client.gui.MachineScreen;
import machine.generator.menu.GeneratorMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The generator: the fuel burns in the firebox between the two fuel slots, the flame shrinking as it
 * burns down; the shaft turns the dynamo, which charges the item in the slot beside it and the store.
 */
public class GeneratorScreen extends MachineScreen<GeneratorMenu> {
    public static final Window FLAME = new Window(47, 37, 14, 14, Mode.UP, 4, false);
    public static final Window ROTOR = new Window(76, 34, 18, 20, Mode.UP, 4, true);

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "generator", 84);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 132, 22, 9, 46, menu.getEnergy(), menu.getMaxEnergy());
        boolean burning = menu.getBurnTime() > 0;
        run(g, "generator_progress", FLAME, menu.getBurnTime(), menu.getTotalBurnTime(), burning, false);
        run(g, "generator_rotor", ROTOR, 0, 1, burning, false);
        if (menu.getTotalBurnTime() > 0) hit(FLAME.x(), FLAME.y(), FLAME.w(), FLAME.h(),
            List.of(Component.translatable("gui.iuce.generator.burn", menu.getBurnTime() / 20, menu.getTotalBurnTime() / 20)));
    }
}
