package client.guide;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns a page file (front matter + Markdown subset + {directives}) into blocks. See docs/GUIDE.md. */
final class GuideParser {
    private GuideParser() {}

    static GuideBook.Page parse(String id, List<String> lines) {
        GuideBook.Page page = new GuideBook.Page(id);
        int i = 0;
        if (!lines.isEmpty() && lines.get(0).trim().equals("---")) {
            i = 1;
            while (i < lines.size() && !lines.get(i).trim().equals("---")) {
                String line = lines.get(i++);
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                switch (key) {
                    case "title" -> page.title = value;
                    case "category" -> page.category = value;
                    case "icon" -> page.icon = GuideBook.stack(value);
                    case "order" -> {
                        try { page.order = Integer.parseInt(value); } catch (NumberFormatException ignored) { }
                    }
                    case "items" -> {
                        for (String spec : value.split(",")) {
                            ItemStack stack = GuideBook.stack(spec);
                            if (!stack.isEmpty()) page.items.add(stack.getItem());
                        }
                    }
                    default -> { }
                }
            }
            i++;
        }
        if (page.title.isEmpty()) page.title = id;
        if (page.icon.isEmpty() && !page.items.isEmpty()) page.icon = new ItemStack(page.items.get(0));

        List<GuideBlocks.Block> blocks = page.blocks;
        StringBuilder paragraph = new StringBuilder();
        while (i < lines.size()) {
            String raw = lines.get(i);
            String line = raw.trim();
            if (line.isEmpty()) {
                flush(paragraph, blocks);
                i++;
            } else if (line.startsWith("#")) {
                flush(paragraph, blocks);
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') level++;
                blocks.add(new GuideBlocks.Heading(Math.min(3, level), line.substring(level).trim()));
                i++;
            } else if (line.equals("---") || line.equals("***")) {
                flush(paragraph, blocks);
                blocks.add(new GuideBlocks.Rule());
                i++;
            } else if (line.startsWith("{") && line.endsWith("}")) {
                flush(paragraph, blocks);
                blocks.add(directive(line.substring(1, line.length() - 1)));
                i++;
            } else if (line.startsWith(">")) {
                flush(paragraph, blocks);
                List<String> body = new ArrayList<>();
                String kind = "note";
                while (i < lines.size() && lines.get(i).trim().startsWith(">")) {
                    String text = lines.get(i).trim().substring(1).trim();
                    if (text.startsWith("[!")) {
                        int end = text.indexOf(']');
                        if (end > 0) {
                            kind = text.substring(2, end).toLowerCase(Locale.ROOT);
                            text = text.substring(end + 1).trim();
                        }
                    }
                    if (!text.isEmpty()) body.add(text);
                    i++;
                }
                blocks.add(new GuideBlocks.Callout(kind, body));
            } else if (line.startsWith("|")) {
                flush(paragraph, blocks);
                List<List<String>> rows = new ArrayList<>();
                while (i < lines.size() && lines.get(i).trim().startsWith("|")) {
                    String row = lines.get(i).trim();
                    i++;
                    if (row.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty()) continue;
                    if (row.endsWith("|")) row = row.substring(0, row.length() - 1);
                    List<String> cells = new ArrayList<>();
                    for (String cell : row.substring(1).split("\\|", -1)) cells.add(cell.trim());
                    rows.add(cells);
                }
                if (!rows.isEmpty()) blocks.add(new GuideBlocks.Table(rows));
            } else if (isListItem(line)) {
                flush(paragraph, blocks);
                List<String> markers = new ArrayList<>();
                List<String> items = new ArrayList<>();
                int number = 1;
                while (i < lines.size() && isListItem(lines.get(i).trim())) {
                    String item = lines.get(i).trim();
                    if (item.startsWith("- ") || item.startsWith("* ")) {
                        markers.add("•");
                        items.add(item.substring(2).trim());
                    } else {
                        markers.add(number++ + ".");
                        items.add(item.substring(item.indexOf(". ") + 2).trim());
                    }
                    i++;
                }
                blocks.add(new GuideBlocks.ListBlock(markers, items));
            } else {
                if (!paragraph.isEmpty()) {
                    char last = paragraph.charAt(paragraph.length() - 1);
                    if (last < 0x2E80 && line.charAt(0) < 0x2E80) paragraph.append(' ');
                }
                paragraph.append(line);
                i++;
            }
        }
        flush(paragraph, blocks);

        StringBuilder search = new StringBuilder();
        for (GuideBlocks.Block block : blocks) search.append(block.searchText()).append(' ');
        page.searchText = search.toString().toLowerCase(Locale.ROOT);
        return page;
    }

    private static boolean isListItem(String line) {
        if (line.startsWith("- ") || line.startsWith("* ")) return true;
        int dot = line.indexOf(". ");
        if (dot < 1 || dot > 3) return false;
        for (int k = 0; k < dot; k++) if (!Character.isDigit(line.charAt(k))) return false;
        return true;
    }

    private static void flush(StringBuilder paragraph, List<GuideBlocks.Block> blocks) {
        if (paragraph.isEmpty()) return;
        blocks.add(new GuideBlocks.Paragraph(paragraph.toString()));
        paragraph.setLength(0);
    }

    private static GuideBlocks.Block directive(String body) {
        int colon = body.indexOf(':');
        String name = (colon < 0 ? body : body.substring(0, colon)).trim().toLowerCase(Locale.ROOT);
        String arg = colon < 0 ? "" : body.substring(colon + 1).trim();
        String caption = "";
        if (name.equals("image") || name.equals("structure")) {
            int bar = arg.indexOf('|');
            if (bar >= 0) {
                caption = arg.substring(bar + 1).trim();
                arg = arg.substring(0, bar).trim();
            }
        }
        return switch (name) {
            case "hero" -> new GuideBlocks.Hero(arg);
            case "recipe" -> new GuideBlocks.Recipes(arg, GuideBlocks.Recipes.Mode.PRODUCES);
            case "uses" -> new GuideBlocks.Recipes(arg, GuideBlocks.Recipes.Mode.USES);
            case "recipes" -> new GuideBlocks.Recipes(arg, GuideBlocks.Recipes.Mode.MACHINE);
            case "items" -> new GuideBlocks.Items(arg);
            case "flow" -> new GuideBlocks.Flow(arg);
            case "cards" -> new GuideBlocks.Cards(arg);
            case "image" -> new GuideBlocks.Image(arg, caption);
            case "structure" -> new GuideBlocks.Structure(arg, caption);
            default -> new GuideBlocks.Paragraph("`{" + body + "}`");
        };
    }
}
