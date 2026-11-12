package client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Colours and drawing helpers shared by the guide. The palette is sampled from the machine GUIs. */
final class GuideStyle {
    private GuideStyle() {}

    static final int PANEL = 0xFFD3E1DF;
    static final int PANEL_HI = 0xFFEDF6F6;
    static final int PANEL_LO = 0xFFA2B0B4;
    static final int BORDER = 0xFF5F6D78;
    static final int SIDEBAR = 0xFFC3D1D1;
    static final int PAPER = 0xFFE7F1F0;
    static final int ROW_ALT = 0xFFDCE8E6;
    static final int SLOT = 0xFF9BABAE;
    static final int SLOT_DARK = 0xFF76838D;
    static final int TEXT = 0xFF2F3842;
    static final int MUTED = 0xFF6A7882;
    static final int ACCENT = 0xFF2A6FB5;
    static final int ACCENT_SOFT = 0xFFBFD8EC;
    static final int WARN = 0xFFC2601A;
    static final int WARN_SOFT = 0xFFF1DCC6;
    static final int TIP = 0xFF2F8554;
    static final int TIP_SOFT = 0xFFCDE6D5;
    static final int CODE = 0x1F6F78;
    static final int LINK = 0x2A6FB5;
    static final int HOVER = 0x40FFFFFF;

    /** Raised panel: dark outline, light top-left bevel, shaded bottom-right bevel. */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, PANEL_HI);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, PANEL_HI);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, PANEL_LO);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, PANEL_LO);
    }

    /** Sunken area filled with the given colour. */
    static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, PANEL_HI);
        g.fill(x, y, x + w - 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** 18x18 item slot in the machine GUI style. */
    static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, PANEL_HI);
        g.fill(x, y, x + 17, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    /** Small button face; returns whether the mouse is over it. */
    static boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY, boolean enabled) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? PANEL_HI : PANEL);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, PANEL_LO);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, PANEL_LO);
        return hover;
    }

    /** Right-pointing arrow, {@code length} wide and 7 tall, vertically centred on {@code cy}. */
    static void arrow(GuiGraphicsExtractor g, int x, int cy, int length, int color) {
        g.fill(x, cy - 1, x + length - 4, cy + 1, color);
        for (int i = 0; i < 4; i++) {
            g.fill(x + length - 4 + i, cy - 4 + i, x + length - 3 + i, cy + 4 - i, color);
        }
    }

    /** Small triangle used for navigation buttons and collapsible sections. */
    static void triangle(GuiGraphicsExtractor g, int x, int y, int dir, int color) {
        switch (dir) {
            case 0 -> { for (int i = 0; i < 4; i++) g.fill(x + i, y + i, x + i + 1, y + 7 - i, color); }          // right
            case 1 -> { for (int i = 0; i < 4; i++) g.fill(x + 3 - i, y + i, x + 4 - i, y + 7 - i, color); }      // left
            default -> { for (int i = 0; i < 4; i++) g.fill(x + i, y + i, x + 7 - i, y + i + 1, color); }         // down
        }
    }

    /** Picks the stack to show from a list of alternatives, cycling once per second. */
    static ItemStack cycle(List<ItemStack> options) {
        if (options.isEmpty()) return ItemStack.EMPTY;
        return options.get((int) ((System.currentTimeMillis() / 1000L) % options.size()));
    }

    static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        g.text(font, text, x, y, color, false);
    }
}
