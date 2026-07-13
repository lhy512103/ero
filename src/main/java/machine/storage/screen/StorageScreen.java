package machine.storage.screen;

import machine.storage.menu.StorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {

    private static final Identifier TEXTURE = Identifier.parse("ero:textures/gui/machine/battery_box.png");
    private static final Identifier ENERGY_BAR = Identifier.parse("ero:textures/gui/com/energy_progress_on_2.png");

    // 能量条坐标（贴图中的深色区域）
    private static final int ENERGY_X = 64;
    private static final int ENERGY_Y = 29;
    private static final int ENERGY_W = 75;
    private static final int ENERGY_H = 18;

    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            TEXTURE, this.leftPos, this.topPos, 0, 0,
            this.imageWidth, this.imageHeight,
            256, 256);

        super.extractContents(graphics, mouseX, mouseY, a);

        drawEnergyBar(graphics);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        if (relX >= ENERGY_X && relX < ENERGY_X + ENERGY_W && relY >= ENERGY_Y && relY < ENERGY_Y + ENERGY_H) {
            long energy = menu.getEnergy();
            long maxEnergy = menu.getMaxEnergy();
            graphics.setComponentTooltipForNextFrame(this.font,
                List.of(Component.literal(energy + " / " + maxEnergy + " EU")),
                mouseX, mouseY);
        }
    }

    private void drawEnergyBar(GuiGraphicsExtractor graphics) {
        long energy = menu.getEnergy();
        long maxEnergy = menu.getMaxEnergy();
        if (maxEnergy <= 0 || energy <= 0) return;
        int filled = (int)(energy * ENERGY_W / maxEnergy);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ENERGY_BAR, this.leftPos + ENERGY_X, this.topPos + ENERGY_Y, 0, 0,
            filled, ENERGY_H, ENERGY_W, ENERGY_H);
    }
}
