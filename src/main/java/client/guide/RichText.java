package client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inline Markdown: {@code **bold**}, {@code *italic*}, {@code `code`}, {@code [label](target)} and
 * {@code {item:namespace:path}}.
 * <p>
 * Wrapping is done here rather than with the vanilla splitter, which only breaks at spaces and
 * therefore produces ragged lines for Chinese text. Lines may break between any two CJK characters,
 * never inside a Latin word or number, and closing punctuation is kept on the previous line.
 * </p>
 */
final class RichText {
    static final int LINE = 11;
    private static final String NO_LINE_START = "，。、；：！？）》」』”’％%,.;:!?)]}";

    private record Glyph(int codePoint, Style style, float width) {}
    private record Run(String text, Style style, int x, int width) {}

    private static final Map<Integer, Float> WIDTHS = new HashMap<>();
    private static final Map<Integer, Float> BOLD_WIDTHS = new HashMap<>();

    final Component component;
    private final int color;
    private List<Glyph> glyphs;
    private List<Component> lines = List.of();
    private List<List<Run>> runs = List.of();
    private int laidOutWidth = -1;

    private RichText(Component component, int color) {
        this.component = component;
        this.color = color;
    }

    static RichText parse(String markdown, int color) {
        MutableComponent out = Component.empty();
        inline(markdown, Style.EMPTY, out);
        return new RichText(out, color);
    }

    static RichText bold(String text, int color) {
        return new RichText(Component.literal(text).withStyle(Style.EMPTY.withBold(true)), color);
    }

    static void clearWidthCache() {
        WIDTHS.clear();
        BOLD_WIDTHS.clear();
    }

    String plain() { return component.getString(); }

    int naturalWidth(Font font) { return font.width(component); }

    private static float width(Font font, int codePoint, Style style) {
        Map<Integer, Float> cache = style.isBold() ? BOLD_WIDTHS : WIDTHS;
        Float cached = cache.get(codePoint);
        if (cached != null) return cached;
        Style measure = style.isBold() ? Style.EMPTY.withBold(true) : Style.EMPTY;
        float w = font.getSplitter().stringWidth(FormattedText.of(Character.toString(codePoint), measure));
        cache.put(codePoint, w);
        return w;
    }

    private static boolean wordChar(int cp) {
        return cp < 0x2E80 && !Character.isWhitespace(cp);
    }

    /** Wraps to {@code width} and returns the resulting height. */
    int layout(Font font, int width) {
        width = Math.max(24, width);
        if (width == laidOutWidth) return lines.size() * LINE;
        laidOutWidth = width;
        if (glyphs == null) {
            List<Glyph> list = new ArrayList<>();
            component.visit((style, text) -> {
                text.codePoints().forEach(cp -> list.add(new Glyph(cp, style, width(font, cp, style))));
                return Optional.empty();
            }, Style.EMPTY);
            glyphs = list;
        }
        List<Component> outLines = new ArrayList<>();
        List<List<Run>> outRuns = new ArrayList<>();
        int start = 0;
        int n = glyphs.size();
        while (start < n) {
            float used = 0;
            int end = start;
            int lastBreak = -1;
            while (end < n) {
                Glyph glyph = glyphs.get(end);
                if (glyph.codePoint == '\n') break;
                if (used + glyph.width > width && end > start) break;
                used += glyph.width;
                end++;
                // A break is allowed after this glyph unless it would split a Latin word.
                if (end < n) {
                    int next = glyphs.get(end).codePoint;
                    if (!(wordChar(glyph.codePoint) && wordChar(next))) lastBreak = end;
                }
            }
            if (end < n && glyphs.get(end).codePoint != '\n') {
                if (lastBreak > start) end = lastBreak;
                // Hanging punctuation: keep closing marks with the text before them.
                if (end < n && NO_LINE_START.indexOf(glyphs.get(end).codePoint) >= 0) end++;
            }
            emit(start, end, outLines, outRuns);
            start = end;
            if (start < n && glyphs.get(start).codePoint == '\n') start++;
            else while (start < n && glyphs.get(start).codePoint == ' ') start++;
        }
        if (outLines.isEmpty()) emit(0, 0, outLines, outRuns);
        lines = outLines;
        runs = outRuns;
        return lines.size() * LINE;
    }

    private void emit(int from, int to, List<Component> outLines, List<List<Run>> outRuns) {
        MutableComponent line = Component.empty();
        List<Run> lineRuns = new ArrayList<>();
        float x = 0;
        int i = from;
        while (i < to) {
            Style style = glyphs.get(i).style;
            StringBuilder text = new StringBuilder();
            float w = 0;
            while (i < to && glyphs.get(i).style.equals(style)) {
                text.appendCodePoint(glyphs.get(i).codePoint);
                w += glyphs.get(i).width;
                i++;
            }
            line.append(Component.literal(text.toString()).withStyle(style));
            lineRuns.add(new Run(text.toString(), style, (int) x, (int) Math.ceil(w)));
            x += w;
        }
        outLines.add(line);
        outRuns.add(lineRuns);
    }

    int lineCount() { return lines.size(); }

    void draw(Ctx c, int x, int y) {
        for (int i = 0; i < lines.size(); i++) {
            int ly = y + i * LINE;
            c.g.text(c.font, lines.get(i), x, ly + 1, color, false);
            for (Run run : runs.get(i)) {
                String target = run.style.getInsertion();
                if (target != null) c.linkZone(x + run.x, ly, run.width, LINE, target);
            }
        }
    }

    private static Style link(Style base, String target) {
        return base.withColor(GuideStyle.LINK).withUnderlined(true).withInsertion(target);
    }

    private static void inline(String s, Style style, MutableComponent out) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                buf.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (s.startsWith("**", i)) {
                int end = s.indexOf("**", i + 2);
                if (end > i + 2) {
                    flush(buf, style, out);
                    inline(s.substring(i + 2, end), style.withBold(true), out);
                    i = end + 2;
                    continue;
                }
            }
            if (ch == '*' && i + 1 < s.length() && s.charAt(i + 1) != ' ' && s.charAt(i + 1) != '*') {
                int end = s.indexOf('*', i + 1);
                if (end > i + 1) {
                    flush(buf, style, out);
                    inline(s.substring(i + 1, end), style.withItalic(true), out);
                    i = end + 1;
                    continue;
                }
            }
            if (ch == '`') {
                int end = s.indexOf('`', i + 1);
                if (end > i) {
                    flush(buf, style, out);
                    out.append(Component.literal(s.substring(i + 1, end)).withStyle(style.withColor(GuideStyle.CODE)));
                    i = end + 1;
                    continue;
                }
            }
            if (ch == '[') {
                int mid = s.indexOf("](", i + 1);
                int end = mid > 0 ? s.indexOf(')', mid + 2) : -1;
                if (end > 0) {
                    flush(buf, style, out);
                    inline(s.substring(i + 1, mid), link(style, s.substring(mid + 2, end).trim()), out);
                    i = end + 1;
                    continue;
                }
            }
            if (s.startsWith("{item:", i)) {
                int end = s.indexOf('}', i);
                if (end > 0) {
                    flush(buf, style, out);
                    String id = s.substring(i + 6, end).trim();
                    ItemStack stack = GuideBook.stack(id);
                    String name = stack.isEmpty() ? id : stack.getHoverName().getString();
                    out.append(Component.literal(name).withStyle(link(style, "item:" + id)));
                    i = end + 1;
                    continue;
                }
            }
            buf.append(ch);
            i++;
        }
        flush(buf, style, out);
    }

    private static void flush(StringBuilder buf, Style style, MutableComponent out) {
        if (buf.isEmpty()) return;
        out.append(Component.literal(buf.toString()).withStyle(style));
        buf.setLength(0);
    }
}
