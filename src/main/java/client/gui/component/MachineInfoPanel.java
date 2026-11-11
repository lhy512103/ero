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

    private record Info(String title, List<String> lines) {}

    private static final Map<Class<? extends Screen>, Info> INFOS = Map.ofEntries(
        Map.entry(MaceratorScreen.class, new Info("打粉机", List.of(
            "将矿石粉碎为 2 份粗粉（深层矿 3 份），是矿物增产线的第一步。",
            "粗粉可直接熔炼，但送入洗矿机提纯后再炼才有额外金属粒。",
            "也可把煤、钻石、铜锭磨成粉，供合金与能量材料使用。",
            "电压等级：LV (32 EU/t)。可安装超频、弹出等升级。"))),
        Map.entry(CompressorScreen.class, new Info("压缩机", List.of(
            "只负责「致密化」：9 板 → 致密板，8 煤粉 → 煤球，",
            "8 煤球 → 压缩煤球 → 钻石，9 能量粉 → 能量水晶。",
            "锭压板 / 拉棍 / 切线请使用金属成型机。",
            "电压等级：LV (32 EU/t)。可安装超频、弹出等升级。"))),
        Map.entry(machine.metal_former.screen.MetalFormerScreen.class, new Info("金属成型机", List.of(
            "三种模式（点击箭头右侧按钮切换）：",
            "辊压：锭 → 板 1:1（锻造锤需 2 锭）；",
            "切割：板 → 4 根导线（剪线钳只有 2 根）；",
            "挤压：锭 → 2 根金属棍（锻造锤只有 1 根）。",
            "电压等级：LV (32 EU/t)。可安装超频、弹出等升级。"))),
        Map.entry(ExtractorScreen.class, new Info("提取机", List.of(
            "从生胶乳提取 3 倍橡胶（熔炉只能炼 1 份），",
            "也可加工橡胶原木、橡胶树叶获取胶乳。",
            "电压等级：LV (32 EU/t)。可安装超频、弹出等升级。"))),
        Map.entry(ElectricFurnaceScreen.class, new Info("电炉", List.of(
            "用电力驱动的熔炉，支持全部原版熔炼配方。",
            "熔炼纯净粉额外产出 3 个金属粒；粗粉与矿石没有加成。",
            "毗邻感应炉时：每台 +50% 速度、+1 并行数，并可借用其能量。",
            "电压等级：LV (32 EU/t)。"))),
        Map.entry(OreWasherScreen.class, new Info("洗矿机", List.of(
            "消耗 1000 mB 水将粗粉洗成纯净粉，50% 概率附带伴生金属粒",
            "（锡→锌、铅→银、铀→铅等，矿链互补）。",
            "水可用水桶手动加注，或用泵+流体抽入升级自动供给。",
            "电压等级：LV (32 EU/t)。"))),
        Map.entry(ElectrolyzerScreen.class, new Info("电解机", List.of(
            "MV 电化学设备，每次加工消耗 1000 mB 水：",
            "机器外壳 → 电镀外壳，钻石粉 → 能量粉（唯一来源），",
            "粘土 → 铝粉，荧石粉 → 红石 + 金粒。",
            "可从相邻的泵自动抽水。电压等级：MV (128 EU/t)。"))),
        Map.entry(InductionFurnaceScreen.class, new Info("感应炉", List.of(
            "MV 合金熔炉：青铜、黄铜、钢、精炼钢、磁铁的唯一来源。",
            "锭路线 3+1 → 3，粉路线（打粉+洗矿供料）3+1 → 4。",
            "炼钢需要 4 铁锭 + 1 煤球（压缩机压制）。",
            "毗邻电炉可为其加速。电压等级：MV (128 EU/t)。"))),
        Map.entry(PlanterScreen.class, new Info("种植机", List.of(
            "自动化温室：培养槽放种子/树苗，土壤槽放对应基质",
            "（泥土/沙子/灵魂沙），持续产出作物。",
            "橡胶树苗+泥土 → 橡胶原木，配合提取机全自动产橡胶。",
            "电压等级：LV (32 EU/t)。"))),
        Map.entry(GeneratorScreen.class, new Info("火力发电机", List.of(
            "燃烧煤炭等燃料发电，是电气化的起点。",
            "产出的 EU 通过线缆输往机器，或为电池槽中的电池充电。",
            "电压等级：LV (32 EU/t)。"))),
        Map.entry(StorageScreen.class, new Info("储能设备", List.of(
            "储存电能并对外供电：Batbox (LV) → CESU (MV) →",
            "MFE (HV) → MFSU (EV)，容量与电压逐级提升。",
            "上格给电池充电，下格从电池取电。"))),
        Map.entry(IndustrialWorkbenchScreen.class, new Info("工业工作台", List.of(
            "通电的高级合成台：高级电路、高级机器外壳、储能设备、",
            "外壳升级、太阳能阶梯等关键配方只能在此合成。",
            "电路在此合成产出 ×2（同样材料，手搓只有 1 个）。",
            "需要供电才能工作。")))
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
            Component tip = Component.literal("机器介绍");
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
