package client.guide;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The built-in guide: sidebar (search + categories) on the left, the current page on the right.
 * All interaction goes through the {@link Ctx.Zone}s collected while drawing the previous frame.
 */
public final class GuideScreen extends Screen {
    @Nullable private static String lastPage;
    private static int lastScroll;
    private static final Set<String> COLLAPSED = new HashSet<>();

    @Nullable private final Screen parent;
    @Nullable private final String requested;
    private final List<Ctx.Zone> zones = new ArrayList<>();
    private final Deque<String> back = new ArrayDeque<>();
    private final Deque<String> forward = new ArrayDeque<>();
    private GuideBook book;
    private GuideBook.Page page;
    private EditBox search;
    private String query = "";
    private int scroll, navScroll, maxScroll, maxNavScroll;
    private int px, py, pw, ph, sideW;
    private int navX, navY, navH, paperX, paperY, paperW, paperH;

    public GuideScreen(@Nullable Screen parent, @Nullable String pageId) {
        super(Component.translatable("guide.iuce.title"));
        this.parent = parent;
        this.requested = pageId;
    }

    @Override
    protected void init() {
        book = GuideBook.get();
        if (page == null || book.page(page.id) != page) {
            String id = requested != null ? requested : lastPage;
            GuideBook.Page found = id == null ? null : book.page(id);
            page = found != null ? found : book.homePage();
            scroll = requested == null && found != null ? lastScroll : 0;
        }
        pw = Math.min(width - 12, 600);
        ph = Math.min(height - 12, 340);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        sideW = pw < 440 ? 106 : 134;
        navX = px + 7;
        navY = py + 45;
        navH = py + ph - 7 - navY;
        paperX = navX + sideW + 5;
        paperY = navY;
        paperW = px + pw - 7 - paperX;
        paperH = navH;

        search = new EditBox(font, navX + 15, py + 30, sideW - 19, 10, Component.translatable("guide.iuce.search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setTextColor(GuideStyle.TEXT);
        search.setTextShadow(false);
        search.setHint(Component.translatable("guide.iuce.search").withStyle(Style.EMPTY.withColor(0x6A7882)));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            navScroll = 0;
        });
        addRenderableWidget(search);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractTransparentBackground(graphics);
    }

    @Override
    public void onClose() {
        lastPage = page.id;
        lastScroll = scroll;
        minecraft.setScreen(parent);
    }

    /** Jumps to a scroll offset; used by the development screenshot tool. */
    public void scrollTo(int offset) { scroll = Math.max(0, offset); }

    public int maxScroll() { return maxScroll; }

    // ------------------------------------------------------------------ navigation

    private void show(GuideBook.Page target, boolean remember) {
        if (target == page) return;
        if (remember) {
            back.push(page.id);
            forward.clear();
        }
        page = target;
        scroll = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void goBack() {
        while (!back.isEmpty()) {
            GuideBook.Page target = book.page(back.pop());
            if (target == null) continue;
            forward.push(page.id);
            show(target, false);
            return;
        }
    }

    private void goForward() {
        while (!forward.isEmpty()) {
            GuideBook.Page target = book.page(forward.pop());
            if (target == null) continue;
            back.push(page.id);
            show(target, false);
            return;
        }
    }

    private void follow(String target) {
        if (target.startsWith("item:")) {
            itemClicked(GuideBook.stack(target.substring(5)), false);
            return;
        }
        GuideBook.Page found = book.page(target);
        if (found != null) show(found, true);
    }

    private void itemClicked(ItemStack stack, boolean uses) {
        if (stack.isEmpty()) return;
        GuideBook.Page found = uses ? null : book.pageFor(stack.getItem());
        if (found != null && found != page) show(found, true);
        else Guide.showExternalRecipes(stack, uses);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        zones.clear();
        Ctx c = new Ctx(g, font, mouseX, mouseY, zones);

        GuideStyle.panel(g, px, py, pw, ph);
        g.fillGradient(px + 3, py + 3, px + pw - 3, py + 21, 0xFF3F5E7C, 0xFF2A4058);
        g.fill(px + 3, py + 21, px + pw - 3, py + 22, GuideStyle.ACCENT);
        g.fill(px + 3, py + 21, px + 60, py + 22, 0xFFFFD040);
        g.text(font, Component.literal(book.title).withStyle(Style.EMPTY.withBold(true)), px + 9, py + 8, 0xFFFFFFFF, true);
        boolean overClose = c.hover(px + pw - 19, py + 5, 14, 14);
        if (overClose) g.fill(px + pw - 19, py + 5, px + pw - 5, py + 19, 0x40FFFFFF);
        g.text(font, "✕", px + pw - 15, py + 8, 0xFFFFFFFF, false);
        c.actionZone(px + pw - 19, py + 5, 14, 14, this::onClose, null);

        // search box
        GuideStyle.inset(g, navX, py + 26, sideW, 16, GuideStyle.PAPER);
        g.fill(navX + 4, py + 30, navX + 9, py + 31, GuideStyle.MUTED);
        g.fill(navX + 4, py + 34, navX + 9, py + 35, GuideStyle.MUTED);
        g.fill(navX + 4, py + 30, navX + 5, py + 35, GuideStyle.MUTED);
        g.fill(navX + 8, py + 30, navX + 9, py + 35, GuideStyle.MUTED);
        g.fill(navX + 9, py + 35, navX + 10, py + 36, GuideStyle.MUTED);
        g.fill(navX + 10, py + 36, navX + 11, py + 37, GuideStyle.MUTED);

        // toolbar
        toolButton(c, paperX, 1, !back.isEmpty(), this::goBack, "guide.iuce.back");
        toolButton(c, paperX + 18, 0, !forward.isEmpty(), this::goForward, "guide.iuce.forward");
        toolButton(c, paperX + 36, 2, page != book.homePage(), () -> show(book.homePage(), true), "guide.iuce.home");
        String crumb = "";
        for (GuideBook.Category category : book.categories) {
            if (category.id.equals(page.category)) crumb = category.title + "  ›  ";
        }
        String trail = font.plainSubstrByWidth(crumb + page.title, paperW - 62);
        GuideStyle.text(g, font, trail, paperX + 58, py + 30, GuideStyle.MUTED);

        // sidebar
        GuideStyle.inset(g, navX, navY, sideW, navH, GuideStyle.SIDEBAR);
        g.enableScissor(navX + 1, navY + 1, navX + sideW - 1, navY + navH - 1);
        c.clip(navX + 1, navY + 1, navX + sideW - 1, navY + navH - 1);
        drawNav(c);
        g.disableScissor();

        // page
        GuideStyle.inset(g, paperX, paperY, paperW, paperH, GuideStyle.PAPER);
        g.enableScissor(paperX + 1, paperY + 1, paperX + paperW - 1, paperY + paperH - 1);
        c.clip(paperX + 1, paperY + 1, paperX + paperW - 1, paperY + paperH - 1);
        drawPage(c);
        g.disableScissor();
        c.noClip();

        super.extractRenderState(g, mouseX, mouseY, a);

        for (int i = zones.size() - 1; i >= 0; i--) {
            Ctx.Zone zone = zones.get(i);
            if (!zone.contains(mouseX, mouseY)) continue;
            if (!zone.stack().isEmpty()) g.setTooltipForNextFrame(font, zone.stack(), mouseX, mouseY);
            else if (zone.tip() != null) g.setTooltipForNextFrame(zone.tip(), mouseX, mouseY);
            break;
        }
    }

    private void toolButton(Ctx c, int x, int icon, boolean enabled, Runnable action, String tipKey) {
        int y = py + 26;
        boolean hover = GuideStyle.button(c.g, x, y, 16, 16, c.mouseX, c.mouseY, enabled);
        int color = !enabled ? GuideStyle.PANEL_LO : hover ? GuideStyle.ACCENT : GuideStyle.TEXT;
        if (icon == 2) {
            // house
            for (int i = 0; i < 4; i++) c.g.fill(x + 7 - i, y + 4 + i, x + 9 + i, y + 5 + i, color);
            c.g.fill(x + 5, y + 8, x + 11, y + 12, color);
            c.g.fill(x + 7, y + 10, x + 9, y + 12, enabled && hover ? GuideStyle.PANEL_HI : GuideStyle.PANEL);
        } else {
            GuideStyle.triangle(c.g, x + 6, y + 5, icon, color);
        }
        if (enabled) c.actionZone(x, y, 16, 16, action, Component.translatable(tipKey));
    }

    private void smallIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.625f);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void navRow(Ctx c, GuideBook.Page entry, int y, int indent) {
        int x = navX + 1;
        int w = sideW - 2;
        boolean selected = entry == page;
        boolean hover = c.hover(x, y, w, 14);
        if (selected) {
            c.g.fill(x, y, x + w, y + 14, GuideStyle.PAPER);
            c.g.fill(x, y, x + 2, y + 14, GuideStyle.ACCENT);
        } else if (hover) {
            c.g.fill(x, y, x + w, y + 14, 0x50FFFFFF);
        }
        smallIcon(c.g, entry.icon, x + indent, y + 2);
        String title = font.plainSubstrByWidth(entry.title, w - indent - 16);
        GuideStyle.text(c.g, font, title, x + indent + 13, y + 3, selected ? GuideStyle.ACCENT : GuideStyle.TEXT);
        c.linkZone(x, y, w, 14, entry.id);
    }

    private void drawNav(Ctx c) {
        int y = navY + 3 - navScroll;
        String needle = query.trim();
        if (!needle.isEmpty()) {
            List<GuideBook.Page> hits = book.search(needle);
            if (hits.isEmpty()) GuideStyle.text(c.g, font, I18n.get("guide.iuce.no_results"), navX + 7, y + 4, GuideStyle.MUTED);
            for (GuideBook.Page hit : hits) {
                navRow(c, hit, y, 4);
                y += 14;
            }
        } else {
            for (GuideBook.Category category : book.categories) {
                if (category.pages.isEmpty()) continue;
                boolean collapsed = COLLAPSED.contains(category.id);
                boolean hover = c.hover(navX + 1, y, sideW - 2, 15);
                GuideStyle.triangle(c.g, navX + 5, y + 4, collapsed ? 0 : 2, hover ? GuideStyle.ACCENT : GuideStyle.MUTED);
                c.g.text(font, Component.literal(category.title).withStyle(Style.EMPTY.withBold(true)),
                    navX + 15, y + 4, hover ? GuideStyle.ACCENT : GuideStyle.TEXT, false);
                String id = category.id;
                c.actionZone(navX + 1, y, sideW - 2, 15, () -> {
                    if (!COLLAPSED.remove(id)) COLLAPSED.add(id);
                }, null);
                y += 16;
                if (collapsed) continue;
                for (GuideBook.Page entry : category.pages) {
                    navRow(c, entry, y, 9);
                    y += 14;
                }
                y += 3;
            }
        }
        int total = y + navScroll - navY + 3;
        maxNavScroll = Math.max(0, total - navH);
        navScroll = Math.min(navScroll, maxNavScroll);
        scrollbar(c.g, navX + sideW - 4, navY, navH, total, navScroll, maxNavScroll);
    }

    private void drawPage(Ctx c) {
        int x = paperX + 10;
        int w = paperW - 26;
        int top = paperY + 1, bottom = paperY + paperH - 1;
        int y = paperY + 8 - scroll;
        List<GuideBlocks.Block> blocks = page.blocks;

        if (blocks.isEmpty() || !(blocks.get(0) instanceof GuideBlocks.Hero)) {
            int tx = x;
            if (!page.icon.isEmpty()) {
                c.g.item(page.icon, x, y + 1);
                tx += 21;
            }
            c.g.pose().pushMatrix();
            c.g.pose().translate(tx, y + 3);
            c.g.pose().scale(1.5f);
            c.g.text(font, Component.literal(page.title).withStyle(Style.EMPTY.withBold(true)), 0, 0, GuideStyle.TEXT, false);
            c.g.pose().popMatrix();
            c.g.fill(x, y + 21, x + w, y + 22, GuideStyle.PANEL_LO);
            c.g.fill(x, y + 20, x + 44, y + 22, GuideStyle.ACCENT);
            y += 30;
        }

        for (GuideBlocks.Block block : blocks) {
            int h = block.layout(font, w);
            if (y + h > top && y < bottom) block.draw(c, x, y, w);
            y += h + block.gap();
        }

        // previous / next
        List<GuideBook.Page> order = book.ordered();
        int at = order.indexOf(page);
        y += 6;
        c.g.fill(x, y, x + w, y + 1, GuideStyle.PANEL_LO);
        y += 6;
        int half = (w - 6) / 2;
        if (at > 0) pageLink(c, order.get(at - 1), x, y, half, true);
        if (at >= 0 && at < order.size() - 1) pageLink(c, order.get(at + 1), x + w - half, y, half, false);
        y += 22;

        int total = y + scroll - paperY + 6;
        maxScroll = Math.max(0, total - paperH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        scrollbar(c.g, paperX + paperW - 6, paperY, paperH, total, scroll, maxScroll);
    }

    private void pageLink(Ctx c, GuideBook.Page target, int x, int y, int w, boolean previous) {
        boolean hover = c.hover(x, y, w, 20);
        c.g.fill(x, y, x + w, y + 20, hover ? GuideStyle.ACCENT : GuideStyle.SLOT_DARK);
        c.g.fill(x + 1, y + 1, x + w - 1, y + 19, hover ? GuideStyle.PANEL_HI : GuideStyle.PANEL);
        GuideStyle.triangle(c.g, previous ? x + 5 : x + w - 9, y + 7, previous ? 1 : 0, hover ? GuideStyle.ACCENT : GuideStyle.MUTED);
        String title = font.plainSubstrByWidth(target.title, w - 24);
        int tx = previous ? x + 14 : x + w - 14 - font.width(title);
        GuideStyle.text(c.g, font, title, tx, y + 6, hover ? GuideStyle.ACCENT : GuideStyle.TEXT);
        c.linkZone(x, y, w, 20, target.id);
    }

    private static void scrollbar(GuiGraphicsExtractor g, int x, int y, int h, int total, int offset, int max) {
        if (max <= 0) return;
        g.fill(x, y + 2, x + 3, y + h - 2, 0x30000000);
        int track = h - 4;
        int thumb = Math.max(14, track * h / Math.max(h, total));
        int ty = y + 2 + (track - thumb) * offset / max;
        g.fill(x, ty, x + 3, ty + thumb, GuideStyle.SLOT_DARK);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (search.isFocused()) setFocused(null);
        int button = event.button();
        if (button == 3) { goBack(); return true; }
        if (button == 4) { goForward(); return true; }
        if (button != 0 && button != 1) return false;
        for (int i = zones.size() - 1; i >= 0; i--) {
            Ctx.Zone zone = zones.get(i);
            if (!zone.contains(event.x(), event.y())) continue;
            if (!zone.stack().isEmpty()) {
                itemClicked(zone.stack(), button == 1);
            } else if (button == 0 && zone.action() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                zone.action().run();
            } else if (button == 0 && zone.link() != null) {
                follow(zone.link());
            } else {
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = (int) Math.round(scrollY * 28);
        if (mouseX < paperX) navScroll = Math.max(0, Math.min(maxNavScroll, navScroll - delta));
        else scroll = Math.max(0, Math.min(maxScroll, scroll - delta));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!search.isFocused()) {
            switch (event.key()) {
                case 259 -> { goBack(); return true; }                                             // backspace
                case 266 -> { scroll = Math.max(0, scroll - paperH + 24); return true; }           // page up
                case 267 -> { scroll = Math.min(maxScroll, scroll + paperH - 24); return true; }   // page down
                case 268 -> { scroll = 0; return true; }                                           // home
                case 269 -> { scroll = maxScroll; return true; }                                   // end
                default -> { }
            }
            if (Guide.isOpenKey(event)) {
                onClose();
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
