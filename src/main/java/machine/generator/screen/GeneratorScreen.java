package machine.generator.screen;

import machine.generator.menu.GeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {

    private static final Identifier TEXTURE = Identifier.parse("ero:textures/gui/generator/generator.png");
    private static final Identifier ARROW_D_ON = Identifier.parse("ero:textures/gui/com/arrow_d_on.png");
    private static final Identifier ENERGY_PROGRESS_ON = Identifier.parse("ero:textures/gui/com/energy_progress_on.png");

    private static final int ENERGY_X = 136;
    private static final int ENERGY_Y = 26;
    private static final int ENERGY_W = 7;
    private static final int ENERGY_H = 34;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
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

        drawBurnProgress(graphics);
        drawEnergyProgress(graphics);
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

    private void drawBurnProgress(GuiGraphicsExtractor graphics) {
        int burnTime = menu.getBurnTime();
        int totalBurnTime = menu.getTotalBurnTime();
        if (totalBurnTime <= 0 || burnTime <= 0) return;
        int barWidth = 22;
        int filled = (totalBurnTime - burnTime) * barWidth / totalBurnTime;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ARROW_D_ON, this.leftPos + 70, this.topPos + 36, 0, 0, filled, 16, 22, 16);
    }

    private void drawEnergyProgress(GuiGraphicsExtractor graphics) {
        long energy = menu.getEnergy();
        long maxEnergy = menu.getMaxEnergy();
        if (maxEnergy <= 0 || energy <= 0) return;
        int barHeight = 34;
        int filled = (int)(energy * barHeight / maxEnergy);
        int y = this.topPos + 26 + (barHeight - filled);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ENERGY_PROGRESS_ON, this.leftPos + 136, y, 0, barHeight - filled, 7, filled, 7, 34);
    }
}
