package machine.workbench.screen;

import machine.workbench.menu.IndustrialWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class IndustrialWorkbenchScreen extends AbstractContainerScreen<IndustrialWorkbenchMenu> {

    private static final Identifier TEXTURE = Identifier.parse("ero:textures/gui/machine/industrial_workbench.png");
    private static final Identifier ARROW_D_ON = Identifier.parse("ero:textures/gui/com/arrow_d_on.png");
    private static final Identifier POWER_ON = Identifier.parse("ero:textures/gui/com/power_on.png");

    public IndustrialWorkbenchScreen(IndustrialWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200);
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

        drawArrow(graphics);
        drawPowerOn(graphics);
    }

    private void drawArrow(GuiGraphicsExtractor graphics) {
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (maxProgress <= 0 || progress <= 0) return;
        // 从左往右动画绘制 (90, 32) 源UV(0,1) 22x15
        int filled = progress * 22 / maxProgress;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ARROW_D_ON, this.leftPos + 90, this.topPos + 32, 0, 1,
            filled, 15, 22, 18);
    }

    private void drawPowerOn(GuiGraphicsExtractor graphics) {
        if (menu.getEnergy() > 0) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                POWER_ON, this.leftPos + 10, this.topPos + 43, 0, 0,
                12, 15, 12, 15);
        }
    }
}
