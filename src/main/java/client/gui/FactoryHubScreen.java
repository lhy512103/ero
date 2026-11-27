package client.gui;

import factory.FactoryHubMenu;
import factory.FactoryRating;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The factory hub on the plain panel: the goal on the upper display with its lamp, the products to
 * stock in slot frames with a stock bar under each, and the minute count and timer bar on the lower
 * display. The stars beside the title show the factory's rating.
 */
public class FactoryHubScreen extends MachineScreen<FactoryHubMenu> {
    private static final int TEXT = 0xFFD8F5E0, DIM = 0xFF9FD0B0;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A;
    private static final int LAMP_GREEN = 0xFF3CC85A, LAMP_AMBER = 0xFFE0A030, LAMP_RED = 0xFFD04030;
    private static final int STAR = 0xFFF0C030, STAR_EDGE = 0xFF9A6A00, STAR_OFF = 0xFF8B8B8B;
    private static final int COLUMNS = 8, GRID_X = 11, GRID_Y = 55, CELL = 22, ROW = 24;
    private static final int BAR_X = 10, BAR_Y = 122, BAR_W = 176;
    private static final String[] STAR_SHAPE = {
        "....#....", "...###...", "#########", ".#######.", "..#####..", "..#####..", ".###.###.", ".##...##.", "#.......#"};

    public FactoryHubScreen(FactoryHubMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 196, 160, null, 160);
        inventoryLabel = false;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 20;
        titleLabelY = 7;
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int stars = menu.getStars();
        for (int i = 0; i < FactoryRating.MAX_STARS; i++) star(g, 127 + i * 10, 6, i < stars);
        display(g, 8, 21, 180, 27);
        FactoryRating.Tier tier = FactoryRating.tier(stars);
        if (tier == null) {
            text(g, Component.translatable("gui.iuce.hub.maxed"), 12, 30, GREEN);
            text(g, Component.translatable("gui.iuce.hub.hint"), 12, 140, LABEL);
            return;
        }
        text(g, Component.translatable("gui.iuce.hub.goal", tier.kinds(), tier.rate()), 12, 25, TEXT);
        text(g, Component.translatable("gui.iuce.hub.goal_time", tier.minutes()), 12, 36, DIM);
        List<Item> items = FactoryRating.candidates(stars);
        int stocked = 0;
        for (int i = 0; i < items.size(); i++) {
            int cx = GRID_X + (i % COLUMNS) * CELL, cy = GRID_Y + (i / COLUMNS) * ROW;
            int stock = menu.getStock(i);
            ItemStack stack = new ItemStack(items.get(i));
            slotFrame(g, leftPos + cx - 1, topPos + cy - 1);
            g.item(stack, leftPos + cx, topPos + cy);
            inset(g, leftPos + cx - 1, topPos + cy + 18, 18, 3, SCREEN);
            int filled = Math.min(18, stock * 18 / tier.capacity());
            if (filled > 0) g.fill(leftPos + cx - 1, topPos + cy + 18, leftPos + cx - 1 + filled, topPos + cy + 21,
                stock >= tier.rate() ? LAMP_GREEN : LAMP_AMBER);
            if (stock >= tier.rate()) stocked++;
            hit(cx - 1, cy - 1, 18, 22, List.of(stack.getHoverName(),
                Component.translatable("gui.iuce.hub.item", stock, tier.capacity(), tier.rate()).withStyle(ChatFormatting.GRAY)));
        }
        // the lamp in the corner of the goal display: are enough products stocked for this minute?
        boolean enough = stocked >= tier.kinds();
        lamp(g, 180, 24, 5, enough ? LAMP_GREEN : LAMP_AMBER);
        Component count = Component.translatable("gui.iuce.hub.stocked", stocked, tier.kinds());
        text(g, count, 177 - font.width(count), 36, enough ? GREEN : AMBER);

        boolean missed = menu.getMisses() > 0;
        display(g, 8, 107, 180, 23);
        text(g, Component.translatable("gui.iuce.hub.progress", menu.getMinutes(), tier.minutes()), 12, 110, missed ? RED : TEXT);
        String clock = String.format("0:%02d", Math.max(0, 59 - menu.getSeconds()));
        text(g, Component.literal(clock), 184 - font.width(clock), 110, DIM);
        inset(g, leftPos + BAR_X, topPos + BAR_Y, BAR_W, 6, SCREEN_EDGE);
        int done = Math.min(BAR_W, (menu.getMinutes() * 60 + menu.getSeconds()) * BAR_W / (tier.minutes() * 60));
        if (done > 0) g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + done, topPos + BAR_Y + 6, missed ? LAMP_RED : LAMP_GREEN);
        Component hint = missed ? Component.translatable("gui.iuce.hub.grace") : Component.translatable("gui.iuce.hub.hint");
        text(g, hint, 12, 140, missed ? 0xFFB02818 : LABEL);
        lamp(g, 178, 141, 5, missed ? LAMP_RED : enough ? LAMP_GREEN : LAMP_AMBER);
    }

    private void star(GuiGraphicsExtractor g, int x, int y, boolean on) {
        for (int row = 0; row < STAR_SHAPE.length; row++) for (int col = 0; col < STAR_SHAPE[row].length(); col++) {
            if (STAR_SHAPE[row].charAt(col) != '#') continue;
            int color = on ? (row >= 6 || col == 0 || col == 8 ? STAR_EDGE : STAR) : STAR_OFF;
            g.fill(leftPos + x + col, topPos + y + row, leftPos + x + col + 1, topPos + y + row + 1, color);
        }
    }
}
