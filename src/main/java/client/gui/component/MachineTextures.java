package client.gui.component;

import net.minecraft.resources.Identifier;

/**
 * 机器 GUI 公共纹理常量
 */
public final class MachineTextures {

    private MachineTextures() {}

    // === 进度条纹理 ===
    /** 纵向小箭头 (16×22, 上→下) */
    public static final Identifier ARROW_VERTICAL = Identifier.parse("iuce:textures/gui/com/arrow_on.png");
    /** 纵向大箭头 (52×17, 上→下) */
    public static final Identifier ARROW_VERTICAL_WIDE = Identifier.parse("iuce:textures/gui/com/arrow_on_1.png");
    /** 横向箭头 (22×18, 左→右) */
    public static final Identifier ARROW_HORIZONTAL = Identifier.parse("iuce:textures/gui/com/arrow_d_on.png");
    /** 洗矿进度条 (16×17, 左→右) */
    public static final Identifier ORE_WASH_PROGRESS = Identifier.parse("iuce:textures/gui/com/xikuang_progress_on.png");

    // === 能量/液体条纹理 ===
    /** 能量条 (7×34, 下→上) */
    public static final Identifier ENERGY_BAR = Identifier.parse("iuce:textures/gui/com/energy_progress_on.png");
    /** 液体条-宽 (18×72, 下→上) */
    public static final Identifier FLUID_BAR_WIDE = Identifier.parse("iuce:textures/gui/com/energy_progress_on_3.png");
    /** 液体条-窄 (4×31, 上→下) */
    public static final Identifier FLUID_BAR_NARROW = Identifier.parse("iuce:textures/gui/com/energy_progress_on_4.png");
    /** 能量条-中 (13×20, 上→下) */
    public static final Identifier ENERGY_BAR_MEDIUM = Identifier.parse("iuce:textures/gui/com/energy_progress_on_5.png");

    // === 指示灯纹理 ===
    /** 电源指示灯 (12×15) */
    public static final Identifier POWER_INDICATOR = Identifier.parse("iuce:textures/gui/com/power_on.png");
    /** 太阳能指示灯 (10×19) */
    public static final Identifier SOLAR_INDICATOR = Identifier.parse("iuce:textures/gui/com/solar_on.png");
}
