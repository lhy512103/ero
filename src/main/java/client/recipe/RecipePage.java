package client.recipe;

import client.gui.MachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared machine artwork for the guide and JEI: the page is the machine screen's light panel, just large
 * enough for the parts it shows. Slots are tinted and outlined by what they hold (green for what a
 * run takes, orange for what it gives, blue for what stays in the machine), and the energy and time
 * of a run are told by the tooltips of the energy gauge and the progress window.
 * <p>
 * The parts are laid out in panel coordinates, as on the screens; {@link Bounds} collects them and
 * gives the {@link Frame}: the offset of the page in the panel and its size.
 * </p>
 */
public final class RecipePage {
    public static final int PAD = 4;
    /**
     * The narrowest a page may be. JEI puts as many pages side by side as fit its recipe area
     * (198 - 12 pixels wide by default, less the scroll bar), up to two columns; a narrower page is
     * put in the left column, so a category with one recipe sat against the left edge. From this
     * width on only one column fits and JEI centres the page.
     */
    public static final int MIN_WIDTH = 96;

    public enum Role {
        INPUT(0xFF3F9F4F, 0xFF7F9A83),
        OUTPUT(0xFFD5862B, 0xFFA3917B),
        STATION(0xFF3AA3C6, 0xFF7D98A4);

        final int ring, face;

        Role(int ring, int face) {
            this.ring = ring;
            this.face = face;
        }
    }

    /** A slot to draw at item position x, y (page coordinates). */
    public record Mark(int x, int y, Role role) {}

    /** The page's top-left corner in panel coordinates, and its size. */
    public record Frame(int ox, int oy, int w, int h) {}

    private RecipePage() {}

    public static final class Bounds {
        private int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;

        public Bounds add(int x, int y, int w, int h) {
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            x1 = Math.max(x1, x + w);
            y1 = Math.max(y1, y + h);
            return this;
        }

        /** A slot at item position x, y, with its frame and outline. */
        public Bounds slot(int x, int y) { return add(x - 2, y - 2, 20, 20); }

        /** A tank gauge, with its bevel and outline. */
        public Bounds tank(int x, int y, int w, int h) { return add(x - 2, y - 2, w + 4, h + 4); }

        /** An energy gauge, with its bevel. */
        public Bounds gauge(int x, int y, int w, int h) { return add(x - 1, y - 1, w + 2, h + 2); }

        /** The painted part of a picture drawn at x, y, w by h. */
        public Bounds picture(Identifier texture, int x, int y, int w, int h) {
            int[] box = opaque(texture);
            if (box == null) return add(x, y, w, h);
            if (box[2] <= box[0]) return this;
            return add(x + box[0], y + box[1], box[2] - box[0], box[3] - box[1]);
        }

        public Frame frame() {
            if (x0 > x1) return new Frame(0, 0, MIN_WIDTH, 2 * PAD);
            int w = x1 - x0 + 2 * PAD, wide = Math.max(w, MIN_WIDTH);
            // a narrow page is widened on both sides, so its parts stay in the middle
            return new Frame(x0 - PAD - (wide - w) / 2, y0 - PAD, wide, y1 - y0 + 2 * PAD);
        }
    }

    private static final Map<Identifier, int[]> OPAQUE = new HashMap<>();

    /** The box {x0, y0, x1, y1} (x1, y1 exclusive) of a texture's non-transparent pixels, or null. */
    private static int[] opaque(Identifier texture) {
        if (OPAQUE.containsKey(texture)) return OPAQUE.get(texture);
        int[] box = null;
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    int x0 = image.getWidth(), y0 = image.getHeight(), x1 = 0, y1 = 0;
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            if ((image.getRGB(x, y) >>> 24) == 0) continue;
                            x0 = Math.min(x0, x);
                            y0 = Math.min(y0, y);
                            x1 = Math.max(x1, x + 1);
                            y1 = Math.max(y1, y + 1);
                        }
                    }
                    box = x1 > x0 ? new int[]{x0, y0, x1, y1} : new int[]{0, 0, 0, 0};
                }
            } catch (IOException ignored) {
                // drawn in full: the page is only less tight
            }
        }
        OPAQUE.put(texture, box);
        return box;
    }

    /** The machine screen's raised light panel as the page's background. */
    public static void panel(GuiGraphicsExtractor g, int w, int h) {
        MachineScreen.panel(g, 0, 0, w, h);
    }

    /**
     * Slot frames tinted and outlined by role. The outlines are drawn first, so slots side by side
     * share one outline around the group.
     */
    public static void slots(GuiGraphicsExtractor g, List<Mark> marks) {
        for (Mark m : marks) ring(g, m.x() - 2, m.y() - 2, 20, 20, m.role().ring);
        for (Mark m : marks) {
            MachineScreen.slotFrame(g, m.x() - 1, m.y() - 1);
            g.fill(m.x(), m.y(), m.x() + 16, m.y() + 16, m.role().face);
        }
    }

    /** An empty tank gauge outlined as an input or an output. */
    public static void tank(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean input) {
        ring(g, x - 2, y - 2, w + 4, h + 4, (input ? Role.INPUT : Role.OUTPUT).ring);
        MachineScreen.tankFrame(g, x, y, w, h, input);
    }

    private static void ring(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** The progress window's tooltip line: how long one run takes. */
    public static Component time(int ticks) {
        return Component.translatable("jei.iuce.time", seconds(ticks), ticks);
    }

    public static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }
}
