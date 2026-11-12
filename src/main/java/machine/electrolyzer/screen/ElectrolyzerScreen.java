package machine.electrolyzer.screen;

import machine.electrolyzer.menu.ElectrolyzerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {

    private static final Identifier TEXTURE = Identifier.parse("iuce:textures/gui/machine/electrolyzer.png");
    private static final Identifier ARROW_ON = Identifier.parse("iuce:textures/gui/com/arrow_on_1.png");
    private static final Identifier POWER_ON = Identifier.parse("iuce:textures/gui/com/power_on.png");
    private static final Identifier FLUID_ON = Identifier.parse("iuce:textures/gui/com/energy_progress_on_4.png");

    private static final int FLUID_X = 19;
    private static final int FLUID_Y = 56;
    private static final int FLUID_W = 4;
    private static final int FLUID_H = 31;

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
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
        drawFluidProgress(graphics);
    }

    /**
     * 绘制进度箭头（从上往下动画帧）
     */
    private void drawProgressArrow(GuiGraphicsExtractor graphics) {
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (maxProgress <= 0 || progress <= 0) return;
        int filled = progress * 17 / maxProgress;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ARROW_ON, this.leftPos + 80, this.topPos + 38, 0, 0, 52, filled, 52, 17);
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
     * 绘制液体进度条（从上往下动画帧）
     * 纹理尺寸：4×31，屏幕位置：(20, 57)
     */
    private void drawFluidProgress(GuiGraphicsExtractor graphics) {
        int fluid = menu.getFluid();
        int maxFluid = menu.getMaxFluid();
        if (maxFluid <= 0 || fluid <= 0) return;
        int filled = fluid * FLUID_H / maxFluid;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            FLUID_ON, this.leftPos + FLUID_X, this.topPos + FLUID_Y, 0, 0, FLUID_W, filled, FLUID_W, FLUID_H);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 流体输入槽（slot 0）：跳过默认提示，显示流体名+数量
        boolean onFluidInput = this.hoveredSlot != null && this.hoveredSlot.index == 0;
        if (!onFluidInput) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }

        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;

        // 液体条提示
        if (relX >= FLUID_X && relX < FLUID_X + FLUID_W && relY >= FLUID_Y && relY < FLUID_Y + FLUID_H) {
            int fluid = menu.getFluid();
            int maxFluid = menu.getMaxFluid();
            graphics.setComponentTooltipForNextFrame(this.font,
                List.of(Component.literal(fluid + " / " + maxFluid + " mB")),
                mouseX, mouseY);
        }

        // 液体输入槽提示（显示流体名和数量）
        if (onFluidInput && menu.getFluid() > 0) {
            int fluid = menu.getFluid();
            int maxFluid = menu.getMaxFluid();
            graphics.setComponentTooltipForNextFrame(this.font,
                List.of(net.minecraft.world.level.material.Fluids.WATER.getFluidType().getDescription(),
                    Component.literal(fluid + " / " + maxFluid + " mB")),
                mouseX, mouseY);
        }
    }

}
