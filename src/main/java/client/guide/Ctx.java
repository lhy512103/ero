package client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Per-frame drawing context. Blocks draw themselves and register {@link Zone}s for anything
 * interactive; the screen resolves hover tooltips and clicks from the zones of the last frame,
 * so blocks need no input handling of their own.
 */
final class Ctx {
    final GuiGraphicsExtractor g;
    final Font font;
    final int mouseX;
    final int mouseY;
    private final List<Zone> zones;
    /** Visible area; zones outside it are discarded so scrolled-away content cannot be clicked. */
    int clipX0 = Integer.MIN_VALUE, clipY0 = Integer.MIN_VALUE, clipX1 = Integer.MAX_VALUE, clipY1 = Integer.MAX_VALUE;

    /** An interactive rectangle. Exactly one of stack / link / action is normally set. */
    record Zone(int x, int y, int w, int h, ItemStack stack, @Nullable String link,
                @Nullable Runnable action, @Nullable Component tip) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    Ctx(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, List<Zone> zones) {
        this.g = g;
        this.font = font;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.zones = zones;
    }

    void clip(int x0, int y0, int x1, int y1) {
        clipX0 = x0; clipY0 = y0; clipX1 = x1; clipY1 = y1;
    }

    void noClip() {
        clip(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private void add(int x, int y, int w, int h, ItemStack stack, @Nullable String link,
                     @Nullable Runnable action, @Nullable Component tip) {
        int x0 = Math.max(x, clipX0), y0 = Math.max(y, clipY0);
        int x1 = Math.min(x + w, clipX1), y1 = Math.min(y + h, clipY1);
        if (x1 > x0 && y1 > y0) zones.add(new Zone(x0, y0, x1 - x0, y1 - y0, stack, link, action, tip));
    }

    void linkZone(int x, int y, int w, int h, String target) { add(x, y, w, h, ItemStack.EMPTY, target, null, null); }

    void actionZone(int x, int y, int w, int h, Runnable action, @Nullable Component tip) {
        add(x, y, w, h, ItemStack.EMPTY, null, action, tip);
    }

    void itemZone(int x, int y, int w, int h, ItemStack stack) {
        if (!stack.isEmpty()) add(x, y, w, h, stack, null, null, null);
    }

    void tipZone(int x, int y, int w, int h, Component tip) { add(x, y, w, h, ItemStack.EMPTY, null, null, tip); }

    boolean hover(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h
            && mouseX >= clipX0 && mouseX < clipX1 && mouseY >= clipY0 && mouseY < clipY1;
    }

    /** Item inside a slot frame, with count, hover highlight and an interactive zone. */
    void slotItem(ItemStack stack, int x, int y) {
        GuideStyle.slot(g, x, y);
        plainItem(stack, x + 1, y + 1);
    }

    /** Item without a slot frame. */
    void plainItem(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            GuideStyle.text(g, font, "?", x + 5, y + 4, GuideStyle.WARN);
            return;
        }
        g.item(stack, x, y);
        g.itemDecorations(font, stack, x, y);
        if (hover(x, y, 16, 16)) g.fill(x, y, x + 16, y + 16, GuideStyle.HOVER);
        itemZone(x, y, 16, 16, stack);
    }
}
