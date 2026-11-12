package client.gui.component;

import machine.compressor.screen.CompressorScreen;
import machine.electric_furnace.screen.ElectricFurnaceScreen;
import machine.electrolyzer.screen.ElectrolyzerScreen;
import machine.extractor.screen.ExtractorScreen;
import machine.generator.screen.GeneratorScreen;
import machine.induction_furnace.screen.InductionFurnaceScreen;
import machine.macerator.screen.MaceratorScreen;
import machine.ore_washer.screen.OreWasherScreen;
import machine.planter.screen.PlanterScreen;
import machine.storage.screen.StorageScreen;
import machine.workbench.screen.IndustrialWorkbenchScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import java.util.Map;

/**
 * 机器 GUI 左上角 "i" 信息按钮 + 机器介绍面板。
 * <p>
 * 各机器背景贴图左上角 (3,3) 处已绘制 13×13 的 "i" 图标。
 * 通过全局 Screen 事件为所有机器界面统一提供：悬浮高亮提示、
 * 点击展开/收起的机器介绍面板，无需修改各 Screen 类。
 * </p>
 */
public final class MachineInfoPanel {

    private MachineInfoPanel() {}

    private static final int BTN_X = 3;
    private static final int BTN_Y = 3;
    private static final int BTN_SIZE = 13;

    private static final int PANEL_BG = 0xF0141414;
    private static final int PANEL_BORDER = 0xFF6A8CAF;
    private static final int TITLE_COLOR = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int LINE_HEIGHT = 10;

    /** Title and text come from the language file: info.iuce.<id>.title and info.iuce.<id>.1, .2, ... */
    private record Info(String id) {
        String title() { return I18n.get("info.iuce." + id + ".title"); }

        List<String> lines() {
            List<String> lines = new java.util.ArrayList<>();
            for (int n = 1; I18n.exists("info.iuce." + id + "." + n); n++) lines.add(I18n.get("info.iuce." + id + "." + n));
            return lines;
        }
    }

    private static final Map<Class<? extends Screen>, Info> INFOS = Map.ofEntries(
        Map.entry(MaceratorScreen.class, new Info("macerator")),
        Map.entry(CompressorScreen.class, new Info("compressor")),
        Map.entry(machine.metal_former.screen.MetalFormerScreen.class, new Info("metal_former")),
        Map.entry(ExtractorScreen.class, new Info("extractor")),
        Map.entry(ElectricFurnaceScreen.class, new Info("electric_furnace")),
        Map.entry(OreWasherScreen.class, new Info("ore_washer")),
        Map.entry(ElectrolyzerScreen.class, new Info("electrolyzer")),
        Map.entry(InductionFurnaceScreen.class, new Info("induction_furnace")),
        Map.entry(PlanterScreen.class, new Info("planter")),
        Map.entry(GeneratorScreen.class, new Info("generator")),
        Map.entry(StorageScreen.class, new Info("storage")),
        Map.entry(IndustrialWorkbenchScreen.class, new Info("industrial_workbench"))
    );

    /** 当前展开介绍面板的界面（切换界面时自动复位） */
    private static Screen expandedScreen = null;

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        expandedScreen = null;
    }

    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!INFOS.containsKey(screen.getClass())) return;

        double relX = event.getMouseX() - screen.getLeftPos();
        double relY = event.getMouseY() - screen.getTopPos();
        boolean onButton = relX >= BTN_X && relX < BTN_X + BTN_SIZE
            && relY >= BTN_Y && relY < BTN_Y + BTN_SIZE;

        if (onButton) {
            expandedScreen = (expandedScreen == screen) ? null : screen;
            event.setCanceled(true);
        } else if (expandedScreen == screen) {
            expandedScreen = null;
            event.setCanceled(true);
        }
    }

    /**
     * Render.Foreground 事件在 pose 已平移到 (leftPos, topPos) 的状态下触发，
     * 因此这里全部使用相对于 GUI 左上角的坐标绘制。
     */
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        Info info = INFOS.get(screen.getClass());
        if (info == null) return;

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int relMouseX = event.getMouseX() - screen.getLeftPos();
        int relMouseY = event.getMouseY() - screen.getTopPos();

        if (expandedScreen == screen) {
            renderPanel(graphics, font, info, screen.getImageWidth());
        } else if (relMouseX >= BTN_X && relMouseX < BTN_X + BTN_SIZE
            && relMouseY >= BTN_Y && relMouseY < BTN_Y + BTN_SIZE) {
            // 悬浮提示
            Component tip = Component.translatable("info.iuce.button");
            int tipW = font.width(tip);
            int tx = BTN_X + BTN_SIZE + 4;
            int ty = BTN_Y + 3;
            graphics.fill(tx - 2, ty - 2, tx + tipW + 2, ty + 9 + 1, PANEL_BG);
            graphics.text(font, tip, tx, ty, TEXT_COLOR);
        }
    }

    private static void renderPanel(GuiGraphicsExtractor graphics, Font font, Info info, int imageWidth) {
        int x0 = 4;
        int y0 = 4;
        int width = imageWidth - 8;
        int textWidth = width - 10;

        int totalLines = 0;
        for (String line : info.lines()) {
            totalLines += Math.max(1, font.split(Component.literal(line), textWidth).size());
        }
        int height = 5 + LINE_HEIGHT + 3 + totalLines * LINE_HEIGHT + 4;

        graphics.fill(x0, y0, x0 + width, y0 + height, PANEL_BG);
        graphics.fill(x0, y0, x0 + width, y0 + 1, PANEL_BORDER);
        graphics.fill(x0, y0 + height - 1, x0 + width, y0 + height, PANEL_BORDER);
        graphics.fill(x0, y0, x0 + 1, y0 + height, PANEL_BORDER);
        graphics.fill(x0 + width - 1, y0, x0 + width, y0 + height, PANEL_BORDER);

        int textX = x0 + 5;
        int textY = y0 + 5;
        graphics.text(font, Component.literal(info.title()), textX, textY, TITLE_COLOR);
        textY += LINE_HEIGHT + 3;
        for (String line : info.lines()) {
            for (var seq : font.split(Component.literal(line), textWidth)) {
                graphics.text(font, seq, textX, textY, TEXT_COLOR);
                textY += LINE_HEIGHT;
            }
        }
    }
}
