package client.gui;

import factory.FactoryHubMenu;
import factory.FactoryRating;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Rating screen of the factory hub: stars, what the next star needs, and how the supply is doing. */
public class FactoryHubScreen extends AbstractContainerScreen<FactoryHubMenu> {
    private static final int BG = 0xF0141414, BORDER = 0xFF6A8CAF, GOLD = 0xFFFFD700, TEXT = 0xFFE0E0E0, DIM = 0xFF9AA4AE;
    private static final int CELL_BG = 0xFF2A2F36, BAR_BG = 0xFF3A4048, GOOD = 0xFF55D06A, LOW = 0xFFE0A030, BAD = 0xFFFF6060;
    private static final int COLUMNS = 8, CELL = 22, GRID_X = 11, GRID_Y = 46, ROW = 26;

    public FactoryHubScreen(FactoryHubMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 198, 150);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
        super.extractContents(g, mouseX, mouseY, a);

        int stars = menu.getStars();
        g.text(font, title, x + 8, y + 8, TEXT, false);
        String rating = "\u2605".repeat(stars) + "\u2606".repeat(Math.max(0, FactoryRating.MAX_STARS - stars));
        g.text(font, rating, x + w - 8 - font.width(rating), y + 8, GOLD, false);

        FactoryRating.Tier tier = FactoryRating.tier(stars);
        if (tier == null) {
            g.text(font, Component.translatable("gui.iuce.hub.maxed"), x + 8, y + 30, DIM, false);
            return;
        }
        g.text(font, Component.translatable("gui.iuce.hub.goal", tier.kinds(), tier.rate()), x + 8, y + 22, TEXT, false);
        g.text(font, Component.translatable("gui.iuce.hub.goal_time", tier.minutes()), x + 8, y + 33, DIM, false);

        List<Item> items = FactoryRating.candidates(stars);
        for (int i = 0; i < items.size(); i++) {
            int cx = x + GRID_X + (i % COLUMNS) * CELL, cy = y + GRID_Y + (i / COLUMNS) * ROW;
            int stock = menu.getStock(i);
            g.fill(cx - 1, cy - 1, cx + 17, cy + 17, CELL_BG);
            g.item(new ItemStack(items.get(i)), cx, cy);
            g.fill(cx - 1, cy + 18, cx + 17, cy + 21, BAR_BG);
            int filled = Math.min(18, stock * 18 / tier.capacity());
            if (filled > 0) g.fill(cx - 1, cy + 18, cx - 1 + filled, cy + 21, stock >= tier.rate() ? GOOD : LOW);
        }

        int py = y + 104;
        g.text(font, Component.translatable("gui.iuce.hub.progress", menu.getMinutes(), tier.minutes()), x + 8, py, TEXT, false);
        String clock = String.format("0:%02d", Math.max(0, 59 - menu.getSeconds()));
        g.text(font, clock, x + w - 8 - font.width(clock), py, DIM, false);
        int barW = w - 16;
        g.fill(x + 8, py + 12, x + 8 + barW, py + 18, BAR_BG);
        int done = Math.min(barW, (menu.getMinutes() * 60 + menu.getSeconds()) * barW / (tier.minutes() * 60));
        if (done > 0) g.fill(x + 8, py + 12, x + 8 + done, py + 18, menu.getMisses() > 0 ? BAD : GOOD);
        Component hint = menu.getMisses() > 0
            ? Component.translatable("gui.iuce.hub.grace").withStyle(ChatFormatting.RED)
            : Component.translatable("gui.iuce.hub.hint");
        g.text(font, hint, x + 8, py + 26, DIM, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        FactoryRating.Tier tier = FactoryRating.tier(menu.getStars());
        if (tier == null) return;
        List<Item> items = FactoryRating.candidates(menu.getStars());
        for (int i = 0; i < items.size(); i++) {
            int cx = leftPos + GRID_X + (i % COLUMNS) * CELL, cy = topPos + GRID_Y + (i / COLUMNS) * ROW;
            if (mouseX < cx - 1 || mouseX >= cx + 17 || mouseY < cy - 1 || mouseY >= cy + 21) continue;
            graphics.setComponentTooltipForNextFrame(font, List.of(
                new ItemStack(items.get(i)).getHoverName(),
                Component.translatable("gui.iuce.hub.item", menu.getStock(i), tier.capacity(), tier.rate()).withStyle(ChatFormatting.GRAY)),
                mouseX, mouseY);
            return;
        }
    }
}
