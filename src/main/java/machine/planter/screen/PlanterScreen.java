package machine.planter.screen;

import client.gui.MachineScreen;
import machine.planter.menu.PlanterMenu;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The planter: the seed above, the soil below and the plant growing in the glass chamber between them
 * under the grow lamps; the harvest leaves on the right. The lamp on the left shows sun or moon.
 */
public class PlanterScreen extends MachineScreen<PlanterMenu> {
    public static final Window WINDOW = new Window(60, 31, 22, 29, Mode.PHASE, 8, false);
    public static final Window SKY = new Window(23, 52, 10, 10, Mode.PHASE, 2, false);

    public PlanterScreen(PlanterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, "planter", 87);
        // the seed slot sits on the title's row
        titleLabel = false;
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        energy(g, 12, 22, 7, 48, menu.getEnergy(), menu.getMaxEnergy());
        run(g, "planter_progress", WINDOW, menu.getProgress(), menu.getMaxProgress(), menu.getProgress() > 0, false);
        hit(WINDOW.x(), WINDOW.y(), WINDOW.w(), WINDOW.h(),
            List.of(Component.translatable("tooltip.iuce.planter.progress", menu.getProgress(), menu.getMaxProgress())));
        run(g, "planter_sky", SKY, menu.isDaytime() ? 1 : 0, 1, false, false);
        int ticks = menu.getClockTime();
        hit(SKY.x(), SKY.y(), SKY.w(), SKY.h(), List.of(
            Component.literal(String.format("%02d:%02d", (ticks / 1000 + 6) % 24, (ticks % 1000) * 60 / 1000)),
            Component.translatable("tooltip.iuce.planter.speed", Component.translatable(menu.getPhaseName()), menu.getSpeedMultiplier())));
    }
}
