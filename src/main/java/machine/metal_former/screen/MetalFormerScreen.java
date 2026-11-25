package machine.metal_former.screen;

import client.gui.MachineScreen;
import machine.metal_former.menu.MetalFormerMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The metal former: the stock runs down between the forming rolls; the mold beside them sets whether
 * it is rolled, cut or drawn into wire.
 */
public class MetalFormerScreen extends MachineScreen<MetalFormerMenu> {
    public static final Window WINDOW = new Window(72, 35, 32, 24, Mode.PHASE, 8, false);

    public MetalFormerScreen(MetalFormerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "metal_former", 87);
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, WINDOW, menu.getProgress(0), menu.getMaxProgress(0));
        if (!menu.hasMold()) hit(MetalFormerMenu.MOLD_X - 1, MetalFormerMenu.MOLD_Y - 1, 18, 18,
            List.of(Component.translatable("tooltip.iuce.metal_former.no_mold")));
    }
}
