package machine.extractor.screen;

import machine.extractor.menu.ExtractorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ExtractorScreen extends AbstractContainerScreen<ExtractorMenu> {

    private static final Identifier TEXTURE = Identifier.parse("iuce:textures/gui/machine/macerator.png");
    private static final Identifier ARROW_ON = Identifier.parse("iuce:textures/gui/com/arrow_on.png");
    private static final Identifier POWER_ON = Identifier.parse("iuce:textures/gui/com/power_on.png");

    public ExtractorScreen(ExtractorMenu menu, Inventory inventory, Component title) {
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

        drawProgressArrow(graphics);
        drawPowerOn(graphics);
    }

    private void drawProgressArrow(GuiGraphicsExtractor graphics) {
        int progress = menu.getProgress(0);
        int maxProgress = menu.getMaxProgress(0);
        if (maxProgress <= 0 || progress <= 0) return;
        int filled = progress * 22 / maxProgress;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ARROW_ON, this.leftPos + 79, this.topPos + 36, 0, 0, 16, filled, 16, 22);
    }

    private void drawPowerOn(GuiGraphicsExtractor graphics) {
        if (menu.getEnergy() > 0) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                POWER_ON, this.leftPos + 7, this.topPos + 71, 0, 0,
                12, 15, 12, 15);
        }
    }
}
