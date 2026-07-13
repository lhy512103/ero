package client.gui.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.function.LongSupplier;

/**
 * 电源指示灯组件
 * <p>
 * 当能量 > 0 时显示亮灯纹理，否则不显示。
 * 几乎所有机器都使用相同的纹理 (12×15) 和默认位置 (7, 71)，
 * 仅 IndustrialWorkbench 使用不同位置 (10, 43)。
 */
public class PowerIndicatorWidget {

    private static final int WIDTH = 12;
    private static final int HEIGHT = 15;

    private final int screenX;
    private final int screenY;
    private final LongSupplier energySupplier;

    /**
     * 创建电源指示灯
     *
     * @param screenX        屏幕 X 偏移（相对于 leftPos）
     * @param screenY        屏幕 Y 偏移（相对于 topPos）
     * @param energySupplier 能量值提供者
     */
    public PowerIndicatorWidget(int screenX, int screenY, LongSupplier energySupplier) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.energySupplier = energySupplier;
    }

    /**
     * 绘制指示灯
     *
     * @param graphics GUI 图形上下文
     * @param leftPos  GUI 左边界
     * @param topPos   GUI 上边界
     */
    public void draw(GuiGraphicsExtractor graphics, int leftPos, int topPos) {
        if (energySupplier.getAsLong() > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED,
                    MachineTextures.POWER_INDICATOR,
                    leftPos + screenX, topPos + screenY,
                    0, 0,
                    WIDTH, HEIGHT,
                    WIDTH, HEIGHT);
        }
    }

    // === 工厂方法 ===

    /**
     * 创建默认位置的电源指示灯 (7, 71)
     * 用于：Macerator, Compressor, Extractor, ElectricFurnace, InductionFurnace,
     *       Electrolyzer, OreWasher, Planter
     */
    public static PowerIndicatorWidget defaultPosition(LongSupplier energySupplier) {
        return new PowerIndicatorWidget(7, 71, energySupplier);
    }

    /**
     * 创建工业工作台位置的电源指示灯 (10, 43)
     */
    public static PowerIndicatorWidget workbenchPosition(LongSupplier energySupplier) {
        return new PowerIndicatorWidget(10, 43, energySupplier);
    }
}
