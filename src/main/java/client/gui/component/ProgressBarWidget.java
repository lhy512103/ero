package client.gui.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.function.IntSupplier;

/**
 * 通用进度条组件（参考 Mekanism GuiProgress 设计）
 * <p>
 * 支持三种填充方向：TOP_DOWN（上→下）、BOTTOM_UP（下→上）、LEFT_RIGHT（左→右）
 * 通过 IntSupplier 获取当前进度和最大进度，实现数据驱动的绘制。
 */
public class ProgressBarWidget {

    public enum Direction {
        TOP_DOWN, BOTTOM_UP, LEFT_RIGHT
    }

    private final Identifier texture;
    private final int screenX;
    private final int screenY;
    private final int barWidth;
    private final int barHeight;
    private final Direction direction;
    private final IntSupplier progressSupplier;
    private final IntSupplier maxProgressSupplier;
    private final int srcU;
    private final int srcV;

    /**
     * 创建进度条
     *
     * @param texture           纹理标识符
     * @param screenX           屏幕 X 偏移（相对于 leftPos）
     * @param screenY           屏幕 Y 偏移（相对于 topPos）
     * @param barWidth          绘制宽度
     * @param barHeight         绘制高度
     * @param direction         填充方向
     * @param progressSupplier  当前进度
     * @param maxProgressSupplier 最大进度
     */
    public ProgressBarWidget(Identifier texture, int screenX, int screenY,
                              int barWidth, int barHeight, Direction direction,
                              IntSupplier progressSupplier, IntSupplier maxProgressSupplier) {
        this(texture, screenX, screenY, barWidth, barHeight, direction,
             progressSupplier, maxProgressSupplier, 0, 0);
    }

    /**
     * 创建进度条（带自定义 UV 偏移）
     */
    public ProgressBarWidget(Identifier texture, int screenX, int screenY,
                              int barWidth, int barHeight, Direction direction,
                              IntSupplier progressSupplier, IntSupplier maxProgressSupplier,
                              int srcU, int srcV) {
        this.texture = texture;
        this.screenX = screenX;
        this.screenY = screenY;
        this.barWidth = barWidth;
        this.barHeight = barHeight;
        this.direction = direction;
        this.progressSupplier = progressSupplier;
        this.maxProgressSupplier = maxProgressSupplier;
        this.srcU = srcU;
        this.srcV = srcV;
    }

    /**
     * 绘制进度条
     *
     * @param graphics GUI 图形上下文
     * @param leftPos  GUI 左边界
     * @param topPos   GUI 上边界
     */
    public void draw(GuiGraphicsExtractor graphics, int leftPos, int topPos) {
        int progress = progressSupplier.getAsInt();
        int maxProgress = maxProgressSupplier.getAsInt();
        if (maxProgress <= 0 || progress <= 0) return;

        switch (direction) {
            case TOP_DOWN -> drawTopDown(graphics, leftPos, topPos, progress, maxProgress);
            case BOTTOM_UP -> drawBottomUp(graphics, leftPos, topPos, progress, maxProgress);
            case LEFT_RIGHT -> drawLeftRight(graphics, leftPos, topPos, progress, maxProgress);
        }
    }

    /**
     * 上→下填充
     */
    private void drawTopDown(GuiGraphicsExtractor graphics, int leftPos, int topPos,
                              int progress, int maxProgress) {
        int filled = progress * barHeight / maxProgress;
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                texture,
                leftPos + screenX, topPos + screenY,
                srcU, srcV,
                barWidth, filled,
                barWidth, barHeight);
    }

    /**
     * 下→上填充
     */
    private void drawBottomUp(GuiGraphicsExtractor graphics, int leftPos, int topPos,
                               int progress, int maxProgress) {
        int filled = progress * barHeight / maxProgress;
        int y = topPos + screenY + (barHeight - filled);
        int v = barHeight - filled;
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                texture,
                leftPos + screenX, y,
                srcU, v,
                barWidth, filled,
                barWidth, barHeight);
    }

    /**
     * 左→右填充
     */
    private void drawLeftRight(GuiGraphicsExtractor graphics, int leftPos, int topPos,
                                int progress, int maxProgress) {
        int filled = progress * barWidth / maxProgress;
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                texture,
                leftPos + screenX, topPos + screenY,
                srcU, srcV,
                filled, barHeight,
                barWidth, barHeight);
    }

    // === 工厂方法：预定义常用进度条 ===

    /**
     * 创建纵向小箭头进度条（16×22, 上→下）
     * 用于：Macerator, Compressor, Extractor, ElectricFurnace, InductionFurnace
     */
    public static ProgressBarWidget verticalArrow(int x, int y, IntSupplier progress, IntSupplier maxProgress) {
        return new ProgressBarWidget(MachineTextures.ARROW_VERTICAL, x, y, 16, 22,
                Direction.TOP_DOWN, progress, maxProgress);
    }

    /**
     * 创建纵向大箭头进度条（52×17, 上→下）
     * 用于：Electrolyzer
     */
    public static ProgressBarWidget verticalArrowWide(int x, int y, IntSupplier progress, IntSupplier maxProgress) {
        return new ProgressBarWidget(MachineTextures.ARROW_VERTICAL_WIDE, x, y, 52, 17,
                Direction.TOP_DOWN, progress, maxProgress);
    }

    /**
     * 创建横向箭头进度条（22×16, 左→右）
     * 用于：Generator, IndustrialWorkbench
     */
    public static ProgressBarWidget horizontalArrow(int x, int y, IntSupplier progress, IntSupplier maxProgress) {
        return new ProgressBarWidget(MachineTextures.ARROW_HORIZONTAL, x, y, 22, 16,
                Direction.LEFT_RIGHT, progress, maxProgress);
    }

    /**
     * 创建横向洗矿进度条（16×17, 左→右）
     * 用于：OreWasher
     */
    public static ProgressBarWidget oreWashProgress(int x, int y, IntSupplier progress, IntSupplier maxProgress) {
        return new ProgressBarWidget(MachineTextures.ORE_WASH_PROGRESS, x, y, 16, 17,
                Direction.LEFT_RIGHT, progress, maxProgress);
    }

    /**
     * 创建工业工作台横向箭头（22×15, 左→右, srcV=1）
     */
    public static ProgressBarWidget workbenchArrow(int x, int y, IntSupplier progress, IntSupplier maxProgress) {
        return new ProgressBarWidget(MachineTextures.ARROW_HORIZONTAL, x, y, 22, 15,
                Direction.LEFT_RIGHT, progress, maxProgress, 0, 1);
    }

    /**
     * 创建能量条（7×34, 下→上）
     * 用于：Generator
     */
    public static ProgressBarWidget energyBar(int x, int y, IntSupplier energy, IntSupplier maxEnergy) {
        return new ProgressBarWidget(MachineTextures.ENERGY_BAR, x, y, 7, 34,
                Direction.BOTTOM_UP, energy, maxEnergy);
    }

    /**
     * 创建中型能量条（13×20, 上→下）
     * 用于：Planter
     */
    public static ProgressBarWidget energyBarMedium(int x, int y, IntSupplier energy, IntSupplier maxEnergy) {
        return new ProgressBarWidget(MachineTextures.ENERGY_BAR_MEDIUM, x, y, 13, 20,
                Direction.TOP_DOWN, energy, maxEnergy);
    }

    /**
     * 创建窄液体条（4×31, 上→下）
     * 用于：Electrolyzer
     */
    public static ProgressBarWidget fluidBarNarrow(int x, int y, IntSupplier fluid, IntSupplier maxFluid) {
        return new ProgressBarWidget(MachineTextures.FLUID_BAR_NARROW, x, y, 4, 31,
                Direction.TOP_DOWN, fluid, maxFluid);
    }

    /**
     * 创建宽液体条（18×72, 下→上）
     * 用于：OreWasher
     */
    public static ProgressBarWidget fluidBarWide(int x, int y, IntSupplier fluid, IntSupplier maxFluid) {
        return new ProgressBarWidget(MachineTextures.FLUID_BAR_WIDE, x, y, 18, 72,
                Direction.BOTTOM_UP, fluid, maxFluid);
    }
}
