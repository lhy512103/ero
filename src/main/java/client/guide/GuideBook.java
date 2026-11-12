package client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The guide's content model, loaded from resources:
 * <pre>
 * assets/iuce/guide/&lt;lang&gt;/_index.json   categories, their order and the home page
 * assets/iuce/guide/&lt;lang&gt;/**&#47;*.md      one page per file (front matter + Markdown)
 * assets/iuce/guide/structures/*.json    block layouts for {structure:...}
 * </pre>
 * Adding a page only requires dropping a new .md file; see docs/GUIDE.md for the syntax.
 */
public final class GuideBook {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuideBook.class);
    public static final String FALLBACK_LANG = "zh_cn";

    public static final class Page {
        public final String id;
        public String title = "";
        public String category = "";
        public ItemStack icon = ItemStack.EMPTY;
        public int order = 1000;
        public final List<Item> items = new ArrayList<>();
        final List<GuideBlocks.Block> blocks = new ArrayList<>();
        String searchText = "";

        Page(String id) { this.id = id; }
    }

    public static final class Category {
        public final String id;
        public final String title;
        public final ItemStack icon;
        public final List<Page> pages = new ArrayList<>();

        Category(String id, String title, ItemStack icon) {
            this.id = id;
            this.title = title;
            this.icon = icon;
        }
    }

    @Nullable private static GuideBook instance;

    public final String title;
    public final String home;
    public final List<Category> categories = new ArrayList<>();
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<Item, Page> byItem = new HashMap<>();
    private final List<Page> ordered = new ArrayList<>();

    private GuideBook(String title, String home) {
        this.title = title;
        this.home = home;
    }

    public static GuideBook get() {
        if (instance == null) instance = load(Minecraft.getInstance().getResourceManager());
        return instance;
    }

    /** Called on resource reload (which also covers language changes). */
    public static void invalidate() {
        instance = null;
        RichText.clearWidthCache();
        GuideStructure.clearCache();
    }

    @Nullable public Page page(String id) { return pages.get(id); }

    @Nullable public Page pageFor(Item item) { return byItem.get(item); }

    /** All pages in reading order (category order, then page order). */
    public List<Page> ordered() { return ordered; }

    public Page homePage() {
        Page page = pages.get(home);
        if (page != null) return page;
        if (!ordered.isEmpty()) return ordered.get(0);
        Page empty = new Page("missing");
        empty.title = "IUCE";
        return empty;
    }

    public List<Page> search(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Page> titleHits = new ArrayList<>();
        List<Page> bodyHits = new ArrayList<>();
        for (Page page : ordered) {
            if (page.title.toLowerCase(Locale.ROOT).contains(needle)) titleHits.add(page);
            else if (page.searchText.contains(needle)) bodyHits.add(page);
        }
        titleHits.addAll(bodyHits);
        return titleHits;
    }

    /** Parses {@code namespace:path} or {@code namespace:path*count}; empty when unknown. */
    public static ItemStack stack(String spec) {
        String id = spec.trim();
        int parsed = 1;
        int star = id.lastIndexOf('*');
        if (star > 0) {
            try {
                parsed = Math.max(1, Integer.parseInt(id.substring(star + 1).trim()));
                id = id.substring(0, star).trim();
            } catch (NumberFormatException ignored) {
                // not a count suffix
            }
        }
        Identifier key = Identifier.tryParse(id);
        if (key == null) return ItemStack.EMPTY;
        int count = parsed;
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(key);
        return item.map(value -> new ItemStack(value, count)).orElse(ItemStack.EMPTY);
    }

    static Optional<Resource> resource(String path) {
        return Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("iuce", path));
    }

    /** Reads width and height from a PNG header; {0, 0} when unreadable. */
    static int[] imageSize(Identifier texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return new int[]{0, 0};
        try (InputStream in = resource.get().open(); DataInputStream data = new DataInputStream(in)) {
            data.skipBytes(16);
            return new int[]{data.readInt(), data.readInt()};
        } catch (Exception error) {
            return new int[]{0, 0};
        }
    }

    private static GuideBook load(ResourceManager manager) {
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        GuideBook book = loadLang(manager, selected);
        if (book == null && !FALLBACK_LANG.equals(selected)) book = loadLang(manager, FALLBACK_LANG);
        return book != null ? book : new GuideBook("IUCE", "welcome");
    }

    @Nullable
    private static GuideBook loadLang(ResourceManager manager, String lang) {
        String root = "guide/" + lang + "/";
        Optional<Resource> index = manager.getResource(Identifier.fromNamespaceAndPath("iuce", root + "_index.json"));
        if (index.isEmpty()) return null;
        GuideBook book;
        try (BufferedReader reader = index.get().openAsReader()) {
            JsonObject json = GsonHelper.parse(reader);
            book = new GuideBook(GsonHelper.getAsString(json, "title", "IUCE"), GsonHelper.getAsString(json, "home", "welcome"));
            JsonArray categories = GsonHelper.getAsJsonArray(json, "categories", new JsonArray());
            for (JsonElement element : categories) {
                JsonObject entry = element.getAsJsonObject();
                book.categories.add(new Category(GsonHelper.getAsString(entry, "id"),
                    GsonHelper.getAsString(entry, "title", GsonHelper.getAsString(entry, "id")),
                    stack(GsonHelper.getAsString(entry, "icon", "minecraft:book"))));
            }
        } catch (Exception error) {
            LOGGER.error("[IUCE guide] Cannot read {}_index.json", root, error);
            return null;
        }

        Map<Identifier, Resource> files = manager.listResources("guide/" + lang, id -> id.getPath().endsWith(".md"));
        List<Identifier> sorted = new ArrayList<>(files.keySet());
        sorted.sort(Comparator.comparing(Identifier::toString));
        for (Identifier file : sorted) {
            String path = file.getPath();
            String id = path.substring(root.length(), path.length() - 3);
            try (BufferedReader reader = files.get(file).openAsReader()) {
                List<String> lines = reader.lines().toList();
                Page page = GuideParser.parse(id, lines);
                book.pages.put(id, page);
            } catch (Exception error) {
                LOGGER.error("[IUCE guide] Cannot read page {}", file, error);
            }
        }

        Map<String, Category> byId = new HashMap<>();
        for (Category category : book.categories) byId.put(category.id, category);
        for (Page page : book.pages.values()) {
            Category category = byId.get(page.category);
            if (category == null) {
                // Unknown categories are appended rather than hidden, so a new page is never lost.
                category = new Category(page.category, page.category.isEmpty() ? "…" : page.category, stack("minecraft:book"));
                byId.put(page.category, category);
                book.categories.add(category);
            }
            category.pages.add(page);
            for (Item item : page.items) book.byItem.putIfAbsent(item, page);
        }
        for (Category category : book.categories) {
            category.pages.sort(Comparator.<Page>comparingInt(page -> page.order).thenComparing(page -> page.id));
            book.ordered.addAll(category.pages);
        }
        LOGGER.info("[IUCE guide] Loaded {} pages in {} categories ({})", book.pages.size(), book.categories.size(), lang);
        return book;
    }
}
