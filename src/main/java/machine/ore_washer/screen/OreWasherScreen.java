package machine.ore_washer.screen;

import machine.ore_washer.menu.OreWasherMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class OreWasherScreen extends AbstractContainerScreen<OreWasherMenu> {

    private static final Identifier TEXTURE = Identifier.parse("ero:textures/gui/machine/ore_washing_plant.png");
    private static final Identifier XIKUANG_ON = Identifier.parse("ero:textures/gui/com/xikuang_progress_on.png");
    private static final Identifier POWER_ON = Identifier.parse("ero:textures/gui/com/power_on.png");
    private static final Identifier FLUID_ON = Identifier.parse("ero:textures/gui/com/energy_progress_on_3.png");

    private static final int FLUID_X = 123;
    private static final int FLUID_Y = 11;
    private static final int FLUID_W = 18;
    private static final int FLUID_H = 72;

    public OreWasherScreen(OreWasherMenu menu, Inventory inventory, Component title) {
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

        drawProgressAnimation(graphics);
        drawPowerOn(graphics);
        drawFluidProgress(graphics);
    }

    /**
     * 绘制洗矿进度（从左往右动画帧）
     */
    private void drawProgressAnimation(GuiGraphicsExtractor graphics) {
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (maxProgress <= 0 || progress <= 0) return;
        int filled = progress * 16 / maxProgress;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            XIKUANG_ON, this.leftPos + 74, this.topPos + 39, 0, 0, filled, 17, 16, 17);
    }

    /**
     * 绘制能量指示灯（复用电炉GUI能量指示灯UV坐标）
     */
    private void drawPowerOn(GuiGraphicsExtractor graphics) {
        if (menu.getEnergy() > 0) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                POWER_ON, this.leftPos + 7, this.topPos + 71, 0, 0,
                12, 15, 12, 15);
        }
    }

    /**
     * 绘制液体进度条（从下往上动画帧）
     * 获取水后才开始绘制动画
     * 纹理尺寸：18×72，屏幕位置：(123, 11)
     */
    private void drawFluidProgress(GuiGraphicsExtractor graphics) {
        int water = menu.getWater();
        int maxWater = menu.getMaxWater();
        if (maxWater <= 0 || water <= 0) return;
        int filled = water * FLUID_H / maxWater;
        int y = this.topPos + FLUID_Y + (FLUID_H - filled);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            FLUID_ON, this.leftPos + FLUID_X, y, 0, FLUID_H - filled, FLUID_W, filled, FLUID_W, FLUID_H);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        if (relX >= FLUID_X && relX < FLUID_X + FLUID_W && relY >= FLUID_Y && relY < FLUID_Y + FLUID_H) {
            int water = menu.getWater();
            int maxWater = menu.getMaxWater();
            String fluidKey = menu.getFluidTranslationKey();
            graphics.setComponentTooltipForNextFrame(this.font,
                List.of(Component.translatable(fluidKey), Component.literal(water + " / " + maxWater + " mB")),
                mouseX, mouseY);
        }
    }
}