package machine.planter.screen;

import machine.planter.menu.PlanterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class PlanterScreen extends AbstractContainerScreen<PlanterMenu> {

    private static final Identifier TEXTURE = Identifier.parse("iuce:textures/gui/machine/crop_planter.png");
    private static final Identifier ENERGY_PROGRESS = Identifier.parse("iuce:textures/gui/com/energy_progress_on_5.png");
    private static final Identifier POWER_ON = Identifier.parse("iuce:textures/gui/com/power_on.png");
    private static final Identifier SOLAR_ON = Identifier.parse("iuce:textures/gui/com/solar_on.png");

    public PlanterScreen(PlanterMenu menu, Inventory inventory, Component title) {
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

        drawEnergyProgress(graphics);
        drawPowerOn(graphics);
        drawSolarOn(graphics);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        if (relX >= 63 && relX < 63 + 13 && relY >= 35 && relY < 35 + 20) {
            int progress = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            graphics.setComponentTooltipForNextFrame(this.font,
                List.of(Component.literal("培育进度: " + progress + " / " + maxProgress)),
                mouseX, mouseY);
        }
        // solar_on 悬停显示游戏日时刻和阶段信息
        if (relX >= 19 && relX < 19 + 10 && relY >= 67 && relY < 67 + 19) {
            int ticks = menu.getClockTime();
            int hours = (ticks / 1000 + 6) % 24;
            int minutes = (ticks % 1000) * 60 / 1000;
            String phase = menu.getPhaseName();
            float speed = menu.getSpeedMultiplier();
            graphics.setComponentTooltipForNextFrame(this.font,
                List.of(
                    Component.literal(String.format("%02d:%02d", hours, minutes)),
                    Component.literal(phase + " · " + speed + "x速度")),
                mouseX, mouseY);
        }
    }

    private void drawEnergyProgress(GuiGraphicsExtractor graphics) {
        long energy = menu.getEnergy();
        long maxEnergy = menu.getMaxEnergy();
        if (maxEnergy <= 0 || energy <= 0) return;
        int filled = (int) (energy * 20 / maxEnergy);
        // 从上往下绘制
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ENERGY_PROGRESS, this.leftPos + 63, this.topPos + 35, 0, 0, 13, filled, 13, 20);
    }

    private void drawPowerOn(GuiGraphicsExtractor graphics) {
        if (menu.getEnergy() > 0) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                POWER_ON, this.leftPos + 7, this.topPos + 71, 0, 0,
                12, 15, 12, 15);
        }
    }

    private void drawSolarOn(GuiGraphicsExtractor graphics) {
        if (menu.isDaytime()) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                SOLAR_ON, this.leftPos + 19, this.topPos + 67, 0, 0,
                10, 19, 10, 19);
        }
    }
}
