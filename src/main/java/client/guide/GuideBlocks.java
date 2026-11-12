package client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Page elements. Each block measures itself for a given width and draws itself; anything
 * interactive is reported through {@link Ctx}. To add a new element: implement {@link Block}
 * here and map a directive name to it in {@link GuideParser}.
 */
final class GuideBlocks {
    private GuideBlocks() {}

    interface Block {
        /** Lays the block out for {@code width} and returns its height. Called every frame; keep it cheap. */
        int layout(Font font, int width);

        void draw(Ctx c, int x, int y, int width);

        default int gap() { return 7; }

        default String searchText() { return ""; }
    }

    // ------------------------------------------------------------------ text

    static final class Heading implements Block {
        private final int level;
        private final RichText text;
        private int height;

        Heading(int level, String text) {
            this.level = level;
            this.text = RichText.bold(text, level == 3 ? GuideStyle.MUTED : GuideStyle.TEXT);
        }

        @Override public int gap() { return level == 1 ? 4 : 11; }

        @Override public int layout(Font font, int width) {
            if (level == 1) height = (int) Math.ceil(text.layout(font, (int) (width / 1.5f)) * 1.5f) + 6;
            else if (level == 2) height = text.layout(font, width - 8) + 4;
            else height = text.layout(font, width);
            return height;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            if (level == 1) {
                c.g.pose().pushMatrix();
                c.g.pose().translate(x, y);
                c.g.pose().scale(1.5f);
                text.draw(c, 0, 0);
                c.g.pose().popMatrix();
                c.g.fill(x, y + height - 3, x + width, y + height - 2, GuideStyle.PANEL_LO);
                c.g.fill(x, y + height - 4, x + 44, y + height - 2, GuideStyle.ACCENT);
            } else if (level == 2) {
                c.g.fill(x, y + 1, x + 3, y + height - 4, GuideStyle.ACCENT);
                text.draw(c, x + 8, y);
                c.g.fill(x, y + height - 1, x + width, y + height, GuideStyle.PANEL_LO);
            } else {
                text.draw(c, x, y);
            }
        }

        @Override public String searchText() { return text.plain(); }
    }

    static final class Paragraph implements Block {
        private final RichText text;

        Paragraph(String markdown) { text = RichText.parse(markdown, GuideStyle.TEXT); }

        @Override public int layout(Font font, int width) { return text.layout(font, width); }

        @Override public void draw(Ctx c, int x, int y, int width) { text.draw(c, x, y); }

        @Override public String searchText() { return text.plain(); }
    }

    static final class ListBlock implements Block {
        private final List<String> markers;
        private final List<RichText> items = new ArrayList<>();
        private final int[] heights;

        ListBlock(List<String> markers, List<String> items) {
            this.markers = markers;
            for (String item : items) this.items.add(RichText.parse(item, GuideStyle.TEXT));
            heights = new int[items.size()];
        }

        @Override public int layout(Font font, int width) {
            int total = 0;
            for (int i = 0; i < items.size(); i++) {
                heights[i] = items.get(i).layout(font, width - 14);
                total += heights[i] + (i > 0 ? 2 : 0);
            }
            return total;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            for (int i = 0; i < items.size(); i++) {
                GuideStyle.text(c.g, c.font, markers.get(i), x + 2, y + 1, GuideStyle.ACCENT);
                items.get(i).draw(c, x + 14, y);
                y += heights[i] + 2;
            }
        }

        @Override public String searchText() {
            StringBuilder out = new StringBuilder();
            for (RichText item : items) out.append(item.plain()).append(' ');
            return out.toString();
        }
    }

    static final class Callout implements Block {
        private final int color;
        private final int soft;
        private final RichText label;
        private final List<RichText> body = new ArrayList<>();
        private final int[] heights;
        private int height;

        Callout(String kind, List<String> lines) {
            switch (kind) {
                case "warn", "warning", "danger" -> { color = GuideStyle.WARN; soft = GuideStyle.WARN_SOFT; kind = "warn"; }
                case "tip" -> { color = GuideStyle.TIP; soft = GuideStyle.TIP_SOFT; }
                default -> { color = GuideStyle.ACCENT; soft = GuideStyle.ACCENT_SOFT; kind = "note"; }
            }
            label = RichText.bold(I18n.get("guide.iuce.callout." + kind), color);
            for (String line : lines) body.add(RichText.parse(line, GuideStyle.TEXT));
            heights = new int[body.size()];
        }

        @Override public int layout(Font font, int width) {
            height = 5 + label.layout(font, width - 14);
            for (int i = 0; i < body.size(); i++) {
                heights[i] = body.get(i).layout(font, width - 14);
                height += heights[i] + 1;
            }
            return height += 4;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            c.g.fill(x, y, x + width, y + height, soft);
            c.g.fill(x, y, x + 3, y + height, color);
            label.draw(c, x + 9, y + 4);
            int ty = y + 5 + RichText.LINE;
            for (int i = 0; i < body.size(); i++) {
                body.get(i).draw(c, x + 9, ty);
                ty += heights[i] + 1;
            }
        }

        @Override public String searchText() {
            StringBuilder out = new StringBuilder();
            for (RichText line : body) out.append(line.plain()).append(' ');
            return out.toString();
        }
    }

    static final class Table implements Block {
        private final List<List<RichText>> rows = new ArrayList<>();
        private final int columns;
        private int[] widths = new int[0];
        private int[] heights = new int[0];

        Table(List<List<String>> source) {
            int max = 0;
            for (List<String> row : source) max = Math.max(max, row.size());
            columns = max;
            for (int r = 0; r < source.size(); r++) {
                List<RichText> cells = new ArrayList<>();
                for (int col = 0; col < columns; col++) {
                    String cell = col < source.get(r).size() ? source.get(r).get(col) : "";
                    cells.add(RichText.parse(r == 0 && !cell.isEmpty() ? "**" + cell + "**" : cell, GuideStyle.TEXT));
                }
                rows.add(cells);
            }
        }

        @Override public int layout(Font font, int width) {
            widths = new int[columns];
            int total = 0;
            for (int col = 0; col < columns; col++) {
                for (List<RichText> row : rows) widths[col] = Math.max(widths[col], row.get(col).naturalWidth(font) + 9);
                total += widths[col];
            }
            if (total > width) {
                // Shrink the widest columns first so short ones keep their natural width.
                int fair = width / Math.max(1, columns);
                int fixed = 0, flexible = 0;
                for (int w : widths) { if (w <= fair) fixed += w; else flexible += w; }
                for (int col = 0; col < columns; col++) {
                    if (widths[col] > fair) widths[col] = Math.max(36, (int) ((long) widths[col] * (width - fixed) / Math.max(1, flexible)));
                }
            } else {
                int extra = width - total;
                for (int col = 0; col < columns; col++) widths[col] += extra / columns;
            }
            heights = new int[rows.size()];
            int height = 1;
            for (int r = 0; r < rows.size(); r++) {
                for (int col = 0; col < columns; col++) {
                    heights[r] = Math.max(heights[r], rows.get(r).get(col).layout(font, widths[col] - 8) + 5);
                }
                height += heights[r];
            }
            return height;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            int tableWidth = 0;
            for (int w : widths) tableWidth += w;
            int ry = y;
            for (int r = 0; r < rows.size(); r++) {
                int fill = r == 0 ? GuideStyle.ACCENT_SOFT : r % 2 == 0 ? GuideStyle.ROW_ALT : GuideStyle.PAPER;
                c.g.fill(x, ry, x + tableWidth, ry + heights[r], fill);
                int cx = x;
                for (int col = 0; col < columns; col++) {
                    rows.get(r).get(col).draw(c, cx + 4, ry + 3);
                    cx += widths[col];
                }
                ry += heights[r];
                c.g.fill(x, ry, x + tableWidth, ry + 1, GuideStyle.PANEL_LO);
            }
            c.g.fill(x, y, x + tableWidth, y + 1, GuideStyle.SLOT_DARK);
            c.g.fill(x, y + heights[0], x + tableWidth, y + heights[0] + 1, GuideStyle.SLOT_DARK);
            c.g.fill(x, ry, x + tableWidth, ry + 1, GuideStyle.SLOT_DARK);
        }

        @Override public String searchText() {
            StringBuilder out = new StringBuilder();
            for (List<RichText> row : rows) for (RichText cell : row) out.append(cell.plain()).append(' ');
            return out.toString();
        }
    }

    static final class Rule implements Block {
        @Override public int layout(Font font, int width) { return 3; }

        @Override public void draw(Ctx c, int x, int y, int width) {
            c.g.fill(x, y + 1, x + width, y + 2, GuideStyle.PANEL_LO);
        }
    }

    // ------------------------------------------------------------------ decoration

    /** {@code {hero: Title | Subtitle}} - banner with the mod logo. */
    static final class Hero implements Block {
        private static final Identifier LOGO = Identifier.parse("iuce:textures/gui/logo/logo.png");
        private final String title;
        private final RichText subtitle;

        Hero(String arg) {
            int bar = arg.indexOf('|');
            title = (bar < 0 ? arg : arg.substring(0, bar)).trim();
            subtitle = RichText.parse(bar < 0 ? "" : arg.substring(bar + 1).trim(), 0xFFCFE2F0);
        }

        @Override public int gap() { return 9; }

        @Override public int layout(Font font, int width) {
            subtitle.layout(font, width - 66);
            return 58;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            c.g.fill(x, y, x + width, y + 58, GuideStyle.BORDER);
            c.g.fillGradient(x + 1, y + 1, x + width - 1, y + 57, 0xFF3F5E7C, 0xFF24384C);
            c.g.fill(x + 1, y + 54, x + width - 1, y + 57, GuideStyle.ACCENT);
            c.g.fill(x + 1, y + 54, x + 60, y + 57, 0xFFFFD040);
            c.g.blit(RenderPipelines.GUI_TEXTURED, LOGO, x + 8, y + 6, 0, 0, 44, 44, 512, 512, 512, 512);
            c.g.pose().pushMatrix();
            c.g.pose().translate(x + 60, y + 9);
            c.g.pose().scale(2f);
            c.g.text(c.font, Component.literal(title), 0, 0, 0xFFFFFFFF, true);
            c.g.pose().popMatrix();
            subtitle.draw(c, x + 60, y + 30);
        }

        @Override public String searchText() { return title + ' ' + subtitle.plain(); }
    }

    /** {@code {image: gui/machine/macerator.png, u, v, w, h | caption}} - texture path is relative to textures/. */
    static final class Image implements Block {
        private final Identifier texture;
        private final RichText caption;
        private final boolean hasCaption;
        private int u, v, regionW, regionH, textureW, textureH;
        private boolean measured;
        private int drawW, drawH, captionH;

        Image(String arg, String caption) {
            String[] parts = arg.split(",");
            String path = parts[0].trim();
            texture = Identifier.parse(path.contains(":") ? path : "iuce:textures/" + path);
            if (parts.length >= 5) {
                try {
                    u = Integer.parseInt(parts[1].trim());
                    v = Integer.parseInt(parts[2].trim());
                    regionW = Integer.parseInt(parts[3].trim());
                    regionH = Integer.parseInt(parts[4].trim());
                } catch (NumberFormatException ignored) {
                    regionW = regionH = 0;
                }
            }
            this.caption = RichText.parse(caption, GuideStyle.MUTED);
            hasCaption = !caption.isEmpty();
        }

        @Override public int layout(Font font, int width) {
            if (!measured) {
                measured = true;
                int[] size = GuideBook.imageSize(texture);
                textureW = size[0];
                textureH = size[1];
                if (regionW <= 0 || regionH <= 0) { regionW = textureW; regionH = textureH; }
            }
            if (textureW <= 0 || regionW <= 0) return RichText.LINE;
            float scale = Math.min(1f, (width - 4) / (float) regionW);
            drawW = Math.round(regionW * scale);
            drawH = Math.round(regionH * scale);
            captionH = hasCaption ? caption.layout(font, width) + 3 : 0;
            return drawH + 4 + captionH;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            if (textureW <= 0 || regionW <= 0) {
                GuideStyle.text(c.g, c.font, "[" + texture + "]", x, y + 1, GuideStyle.WARN);
                return;
            }
            int ix = x + (width - drawW - 4) / 2;
            c.g.fill(ix, y, ix + drawW + 4, y + drawH + 4, GuideStyle.BORDER);
            c.g.fill(ix + 1, y + 1, ix + drawW + 3, y + drawH + 3, GuideStyle.PANEL_HI);
            c.g.blit(RenderPipelines.GUI_TEXTURED, texture, ix + 2, y + 2, u, v, drawW, drawH, regionW, regionH, textureW, textureH);
            if (hasCaption) {
                int cw = Math.min(width, caption.naturalWidth(c.font));
                caption.draw(c, x + (width - cw) / 2, y + drawH + 6);
            }
        }

        @Override public String searchText() { return caption.plain(); }
    }

    // ------------------------------------------------------------------ items

    /** {@code {items: a, b*4, c}} - a wrapped row of item slots. */
    static final class Items implements Block {
        private final List<ItemStack> stacks = new ArrayList<>();
        private int perRow = 1;

        Items(String arg) {
            for (String spec : arg.split(",")) {
                if (!spec.isBlank()) stacks.add(GuideBook.stack(spec));
            }
        }

        @Override public int layout(Font font, int width) {
            perRow = Math.max(1, width / 20);
            return ((stacks.size() + perRow - 1) / perRow) * 20 - 2;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            for (int i = 0; i < stacks.size(); i++) {
                c.slotItem(stacks.get(i), x + (i % perRow) * 20, y + (i / perRow) * 20);
            }
        }
    }

    /**
     * {@code {flow: iuce:tin_ore -> @iuce:macerator -> iuce:crushed_tin_dust*2}} - a processing chain.
     * {@code @id} marks a machine (drawn as a raised tile), {@code a + b} joins inputs.
     */
    static final class Flow implements Block {
        private record Element(int type, ItemStack stack) {}   // 0 item, 1 station, 2 arrow, 3 plus
        private final List<Element> elements = new ArrayList<>();
        private final List<int[]> positions = new ArrayList<>();

        Flow(String arg) {
            String[] steps = arg.split("->");
            for (int s = 0; s < steps.length; s++) {
                if (s > 0) elements.add(new Element(2, ItemStack.EMPTY));
                String[] parts = steps[s].split("\\+");
                for (int p = 0; p < parts.length; p++) {
                    if (p > 0) elements.add(new Element(3, ItemStack.EMPTY));
                    String spec = parts[p].trim();
                    boolean station = spec.startsWith("@");
                    elements.add(new Element(station ? 1 : 0, GuideBook.stack(station ? spec.substring(1) : spec)));
                }
            }
        }

        private static int widthOf(Element element) {
            return switch (element.type) { case 1 -> 24; case 2 -> 22; case 3 -> 12; default -> 18; };
        }

        @Override public int layout(Font font, int width) {
            positions.clear();
            int x = 0, row = 0;
            for (Element element : elements) {
                int w = widthOf(element);
                if (x + w > width && x > 0) { x = 12; row++; }
                positions.add(new int[]{x, row * 28});
                x += w;
            }
            return row * 28 + 24;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                int ex = x + positions.get(i)[0];
                int ey = y + positions.get(i)[1];
                switch (element.type) {
                    case 1 -> {
                        GuideStyle.panel(c.g, ex + 1, ey, 22, 24);
                        c.plainItem(element.stack, ex + 4, ey + 4);
                    }
                    case 2 -> GuideStyle.arrow(c.g, ex + 4, ey + 12, 14, GuideStyle.SLOT_DARK);
                    case 3 -> GuideStyle.text(c.g, c.font, "+", ex + 3, ey + 8, GuideStyle.MUTED);
                    default -> c.slotItem(element.stack, ex, ey + 3);
                }
            }
        }
    }

    /** {@code {cards: page/a, page/b}} - a grid of links to other pages. */
    static final class Cards implements Block {
        private final List<String> ids = new ArrayList<>();
        private int columns = 2;

        Cards(String arg) {
            for (String id : arg.split(",")) if (!id.isBlank()) ids.add(id.trim());
        }

        @Override public int layout(Font font, int width) {
            columns = width >= 330 ? 3 : width >= 200 ? 2 : 1;
            return ((ids.size() + columns - 1) / columns) * 26 - 4;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            int cardW = (width - (columns - 1) * 4) / columns;
            for (int i = 0; i < ids.size(); i++) {
                GuideBook.Page page = GuideBook.get().page(ids.get(i));
                int cx = x + (i % columns) * (cardW + 4);
                int cy = y + (i / columns) * 26;
                boolean hover = c.hover(cx, cy, cardW, 22);
                c.g.fill(cx, cy, cx + cardW, cy + 22, hover ? GuideStyle.ACCENT : GuideStyle.SLOT_DARK);
                c.g.fill(cx + 1, cy + 1, cx + cardW - 1, cy + 21, hover ? GuideStyle.PANEL_HI : GuideStyle.PANEL);
                c.g.fill(cx + 1, cy + 20, cx + cardW - 1, cy + 21, GuideStyle.PANEL_LO);
                if (page == null) {
                    GuideStyle.text(c.g, c.font, ids.get(i), cx + 5, cy + 7, GuideStyle.WARN);
                    continue;
                }
                if (!page.icon.isEmpty()) c.g.item(page.icon, cx + 3, cy + 3);
                String title = c.font.plainSubstrByWidth(page.title, cardW - 26);
                GuideStyle.text(c.g, c.font, title, cx + 23, cy + 7, hover ? GuideStyle.ACCENT : GuideStyle.TEXT);
                c.linkZone(cx, cy, cardW, 22, page.id);
            }
        }
    }

    // ------------------------------------------------------------------ recipes

    /**
     * {@code {recipe: item, item}} recipes producing the items, {@code {uses: item}} recipes consuming it,
     * {@code {recipes: machine_type}} every recipe of a machine. Data comes from the live recipe manager.
     */
    static final class Recipes implements Block {
        enum Mode { PRODUCES, USES, MACHINE }

        private final String arg;
        private final Mode mode;
        private List<GuideRecipes.View> views = List.of();
        private int version = -1;
        private int index;
        private int bodyHeight;

        Recipes(String arg, Mode mode) {
            this.arg = arg;
            this.mode = mode;
        }

        private void resolve() {
            int current = GuideRecipes.version();
            if (current == version) return;
            version = current;
            List<GuideRecipes.View> found = new ArrayList<>();
            for (String part : arg.split(",")) {
                String key = part.trim();
                if (key.isEmpty()) continue;
                if (mode == Mode.MACHINE) {
                    found.addAll(GuideRecipes.ofMachine(key.toLowerCase(Locale.ROOT)));
                    continue;
                }
                ItemStack stack = GuideBook.stack(key);
                if (stack.isEmpty()) continue;
                if (mode == Mode.USES) {
                    found.addAll(GuideRecipes.using(stack.getItem()));
                } else {
                    for (GuideRecipes.View view : GuideRecipes.producing(stack.getItem())) {
                        if (view.output().getItem() == stack.getItem()) found.add(view);
                    }
                }
            }
            views = found;
            index = Math.min(index, Math.max(0, views.size() - 1));
            bodyHeight = 18;
            for (GuideRecipes.View view : views) {
                int h = view.kind() == GuideRecipes.Kind.CRAFT ? 54 : view.extras().isEmpty() ? 18 : 28;
                bodyHeight = Math.max(bodyHeight, h);
            }
        }

        @Override public int layout(Font font, int width) {
            resolve();
            return views.isEmpty() ? 20 : 21 + bodyHeight + 17;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            int w = Math.min(width, 236);
            if (views.isEmpty()) {
                c.g.fill(x, y, x + w, y + 20, GuideStyle.SLOT_DARK);
                c.g.fill(x + 1, y + 1, x + w - 1, y + 19, GuideStyle.ROW_ALT);
                GuideStyle.text(c.g, c.font, I18n.get("guide.iuce.recipe.none"), x + 6, y + 6, GuideStyle.MUTED);
                return;
            }
            int h = 21 + bodyHeight + 17;
            GuideRecipes.View view = views.get(index);
            c.g.fill(x, y, x + w, y + h, GuideStyle.SLOT_DARK);
            c.g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GuideStyle.PANEL);
            c.g.fill(x + 1, y + 1, x + w - 1, y + 19, GuideStyle.ACCENT_SOFT);
            c.g.fill(x + 1, y + 19, x + w - 1, y + 20, GuideStyle.SLOT_DARK);

            // header: station + pager
            c.plainItem(view.station(), x + 3, y + 2);
            int pagerW = views.size() > 1 ? 58 : 0;
            String name = c.font.plainSubstrByWidth(view.stationName().getString(), w - 28 - pagerW);
            GuideStyle.text(c.g, c.font, name, x + 23, y + 6, GuideStyle.TEXT);
            if (views.size() > 1) {
                int px = x + w - 57;
                boolean left = GuideStyle.button(c.g, px, y + 4, 12, 12, c.mouseX, c.mouseY, true);
                GuideStyle.triangle(c.g, px + 4, y + 7, 1, left ? GuideStyle.ACCENT : GuideStyle.TEXT);
                c.actionZone(px, y + 4, 12, 12, () -> index = (index + views.size() - 1) % views.size(), null);
                String count = (index + 1) + "/" + views.size();
                GuideStyle.text(c.g, c.font, count, px + 14 + (28 - c.font.width(count)) / 2, y + 6, GuideStyle.MUTED);
                boolean right = GuideStyle.button(c.g, px + 43, y + 4, 12, 12, c.mouseX, c.mouseY, true);
                GuideStyle.triangle(c.g, px + 47, y + 7, 0, right ? GuideStyle.ACCENT : GuideStyle.TEXT);
                c.actionZone(px + 43, y + 4, 12, 12, () -> index = (index + 1) % views.size(), null);
            }

            // body
            int by = y + 25;
            int cy = by + bodyHeight / 2;
            int ox;
            if (view.kind() == GuideRecipes.Kind.CRAFT) {
                int top = by + (bodyHeight - 54) / 2;
                boolean grid = view.gridW() == 3;
                for (int i = 0; i < 9; i++) {
                    int sx = x + 8 + (i % 3) * 18, sy = top + (i / 3) * 18;
                    List<ItemStack> options = i < view.inputs().size() ? view.inputs().get(i) : List.of();
                    if (options.isEmpty()) GuideStyle.slot(c.g, sx, sy);
                    else c.slotItem(GuideStyle.cycle(options), sx, sy);
                }
                GuideStyle.arrow(c.g, x + 70, cy, 18, GuideStyle.SLOT_DARK);
                ox = x + 96;
                c.slotItem(view.output(), ox, cy - 9);
                String note = view.workbenchOnly() ? I18n.get("guide.iuce.recipe.workbench_only")
                    : grid ? "" : I18n.get("guide.iuce.recipe.shapeless");
                if (!grid && view.workbenchOnly()) note += " · " + I18n.get("guide.iuce.recipe.shapeless");
                GuideStyle.text(c.g, c.font, note, x + 8, y + h - 13, view.workbenchOnly() ? GuideStyle.WARN : GuideStyle.MUTED);
            } else {
                int sx = x + 8;
                int sy = by + (bodyHeight - (view.extras().isEmpty() ? 18 : 28)) / 2;
                for (int i = 0; i < view.inputs().size(); i++) {
                    if (i > 0) {
                        GuideStyle.text(c.g, c.font, "+", sx + 3, sy + 5, GuideStyle.MUTED);
                        sx += 12;
                    }
                    c.slotItem(GuideStyle.cycle(view.inputs().get(i)), sx, sy);
                    sx += 18;
                }
                GuideStyle.arrow(c.g, sx + 5, sy + 9, 18, GuideStyle.SLOT_DARK);
                ox = sx + 30;
                c.slotItem(view.output(), ox, sy);
                int ex = ox + 24;
                for (GuideRecipes.Extra extra : view.extras()) {
                    c.slotItem(extra.stack(), ex, sy);
                    String chance = Math.round(extra.chance() * 100) + "%";
                    GuideStyle.text(c.g, c.font, chance, ex + (18 - c.font.width(chance)) / 2, sy + 20, GuideStyle.MUTED);
                    ex += 22;
                }
                String info;
                if (view.kind() == GuideRecipes.Kind.MACHINE) {
                    long perTick = Math.max(1, (view.energy() + view.time() - 1) / Math.max(1, view.time()));
                    info = I18n.get("guide.iuce.recipe.cost", view.energy(), String.format(Locale.ROOT, "%.1f", view.time() / 20f), perTick);
                } else {
                    info = I18n.get("guide.iuce.recipe.time", String.format(Locale.ROOT, "%.1f", view.time() / 20f));
                }
                GuideStyle.text(c.g, c.font, info, x + 8, y + h - 13, GuideStyle.MUTED);
            }
        }
    }

    // ------------------------------------------------------------------ structures

    /**
     * {@code {structure: name | caption}} - a block layout drawn as a stacked isometric view with a
     * layer selector and a materials list. The layout comes from {@link GuideStructure}.
     */
    static final class Structure implements Block {
        private static final float DX = 7.07f, DZ = 3.54f, DY = 8.66f;
        private final String name;
        private final RichText caption;
        private final boolean hasCaption;
        private int shownLayers = -1;
        private float scale = 1;
        private int viewH, materialsH, captionH;
        private float minX, minY;

        Structure(String name, String caption) {
            this.name = name.trim();
            this.caption = RichText.parse(caption, GuideStyle.MUTED);
            hasCaption = !caption.isEmpty();
        }

        @Override public int layout(Font font, int width) {
            GuideStructure structure = GuideStructure.get(name);
            if (structure == null) return RichText.LINE;
            if (shownLayers < 0) shownLayers = structure.sizeY;
            minX = -(structure.sizeZ - 1) * DX;
            float maxX = (structure.sizeX - 1) * DX + 16;
            minY = -(structure.sizeY - 1) * DY;
            float maxY = (structure.sizeX + structure.sizeZ - 2) * DZ + 16;
            float w = maxX - minX, h = maxY - minY;
            scale = (w * 2 <= width - 16 && h * 2 <= 150) ? 2 : 1;
            viewH = (int) Math.ceil(h * scale) + 16;
            int perRow = Math.max(1, (width - 8) / 20);
            int kinds = structure.materials(shownLayers).size();
            materialsH = ((kinds + perRow - 1) / perRow) * 20;
            captionH = hasCaption ? caption.layout(font, width - 8) + 2 : 0;
            return viewH + 18 + materialsH + captionH + 6;
        }

        @Override public void draw(Ctx c, int x, int y, int width) {
            GuideStructure structure = GuideStructure.get(name);
            if (structure == null) {
                GuideStyle.text(c.g, c.font, "[structure: " + name + "]", x, y + 1, GuideStyle.WARN);
                return;
            }
            int total = viewH + 18 + materialsH + captionH + 6;
            c.g.fill(x, y, x + width, y + total, GuideStyle.SLOT_DARK);
            c.g.fill(x + 1, y + 1, x + width - 1, y + total - 1, GuideStyle.PANEL);
            c.g.fillGradient(x + 1, y + 1, x + width - 1, y + viewH, 0xFFF4FAFA, 0xFFC9D8D8);

            float boundsW = ((structure.sizeX - 1) * DX + 16 - minX) * scale;
            float originX = x + (width - boundsW) / 2f - minX * scale;
            float originY = y + 8 - minY * scale;
            List<GuideStructure.Cell> cells = new ArrayList<>(structure.cells);
            cells.sort((a, b) -> {
                int depth = Integer.compare(a.pos().getX() + a.pos().getZ(), b.pos().getX() + b.pos().getZ());
                return depth != 0 ? depth : Integer.compare(a.pos().getY(), b.pos().getY());
            });
            for (GuideStructure.Cell cell : cells) {
                if (cell.pos().getY() >= shownLayers) continue;
                float px = (cell.pos().getX() - cell.pos().getZ()) * DX;
                float py = (cell.pos().getX() + cell.pos().getZ()) * DZ - cell.pos().getY() * DY;
                c.g.pose().pushMatrix();
                c.g.pose().translate(originX + px * scale, originY + py * scale);
                c.g.pose().scale(scale);
                c.g.item(cell.stack(), 0, 0);
                c.g.pose().popMatrix();
            }

            // layer selector
            int ly = y + viewH + 3;
            c.g.fill(x + 1, y + viewH, x + width - 1, y + viewH + 1, GuideStyle.SLOT_DARK);
            boolean down = GuideStyle.button(c.g, x + 5, ly, 12, 12, c.mouseX, c.mouseY, shownLayers > 1);
            GuideStyle.triangle(c.g, x + 9, ly + 3, 1, down ? GuideStyle.ACCENT : shownLayers > 1 ? GuideStyle.TEXT : GuideStyle.PANEL_LO);
            c.actionZone(x + 5, ly, 12, 12, () -> shownLayers = Math.max(1, shownLayers - 1), null);
            String label = I18n.get("guide.iuce.structure.layers", shownLayers, structure.sizeY);
            int labelW = Math.max(44, c.font.width(label) + 6);
            GuideStyle.text(c.g, c.font, label, x + 19 + (labelW - c.font.width(label)) / 2, ly + 2, GuideStyle.TEXT);
            boolean up = GuideStyle.button(c.g, x + 19 + labelW, ly, 12, 12, c.mouseX, c.mouseY, shownLayers < structure.sizeY);
            GuideStyle.triangle(c.g, x + 23 + labelW, ly + 3, 0, up ? GuideStyle.ACCENT : shownLayers < structure.sizeY ? GuideStyle.TEXT : GuideStyle.PANEL_LO);
            c.actionZone(x + 19 + labelW, ly, 12, 12, () -> shownLayers = Math.min(structure.sizeY, shownLayers + 1), null);
            String size = structure.sizeX + " × " + structure.sizeY + " × " + structure.sizeZ;
            GuideStyle.text(c.g, c.font, size, x + width - 6 - c.font.width(size), ly + 2, GuideStyle.MUTED);

            // materials
            int my = y + viewH + 18;
            int perRow = Math.max(1, (width - 8) / 20);
            List<ItemStack> materials = structure.materials(shownLayers);
            for (int i = 0; i < materials.size(); i++) {
                c.slotItem(materials.get(i), x + 5 + (i % perRow) * 20, my + (i / perRow) * 20);
            }
            if (hasCaption) caption.draw(c, x + 5, my + materialsH + 2);
        }

        @Override public String searchText() { return caption.plain(); }
    }
}
