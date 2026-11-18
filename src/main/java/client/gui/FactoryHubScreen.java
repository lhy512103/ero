package client.gui;

import factory.FactoryHubMenu;
import factory.FactoryRating;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Rating screen of the factory hub, on the steel panel drawn by {@code tools/draw_hub_gui.py}: stars,
 * what the next star needs, how full each product's store is, and how long the supply has held.
 */
public class FactoryHubScreen extends AbstractContainerScreen<FactoryHubMenu> {
    private static final Identifier TEXTURE = Identifier.parse("iuce:textures/gui/factory_hub.png");
    private static final int TITLE = 0xFF2B3036, TEXT = 0xFFD8F5E0, DIM = 0xFF9FD0B0, ENGRAVED = 0xFF3A4047;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A;
    private static final int COLUMNS = 8, GRID_X = 11, GRID_Y = 55, CELL = 22, ROW = 24;
    private static final int BAR_X = 10, BAR_Y = 122, BAR_W = 176;

    public FactoryHubScreen(FactoryHubMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 196, 160);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        blit(g, 0, 0, 0, 0, imageWidth, imageHeight);
        super.extractContents(g, mouseX, mouseY, a);

        int stars = menu.getStars();
        text(g, title, 20, 7, TITLE);
        for (int i = 0; i < FactoryRating.MAX_STARS; i++) {
            blit(g, 127 + i * 10, 6, i < stars ? 200 : 210, 0, 9, 9);
        }

        FactoryRating.Tier tier = FactoryRating.tier(stars);
        if (tier == null) {
            text(g, Component.translatable("gui.iuce.hub.maxed"), 12, 30, GREEN);
            text(g, Component.translatable("gui.iuce.hub.hint"), 12, 140, ENGRAVED);
            return;
        }
        text(g, Component.translatable("gui.iuce.hub.goal", tier.kinds(), tier.rate()), 12, 25, TEXT);
        text(g, Component.translatable("gui.iuce.hub.goal_time", tier.minutes()), 12, 36, DIM);

        List<Item> items = FactoryRating.candidates(stars);
        int stocked = 0;
        for (int i = 0; i < items.size(); i++) {
            int cx = GRID_X + (i % COLUMNS) * CELL, cy = GRID_Y + (i / COLUMNS) * ROW;
            int stock = menu.getStock(i);
            g.item(new ItemStack(items.get(i)), leftPos + cx, topPos + cy);
            int filled = Math.min(18, stock * 18 / tier.capacity());
            if (filled > 0) blit(g, cx - 1, cy + 18, 200, stock >= tier.rate() ? 12 : 16, filled, 3);
            if (stock >= tier.rate()) stocked++;
        }
        // Lamp in the corner of the goal display: are enough products stocked for this minute?
        boolean enough = stocked >= tier.kinds();
        blit(g, 180, 24, enough ? 220 : 226, 0, 5, 5);
        Component count = Component.translatable("gui.iuce.hub.stocked", stocked, tier.kinds());
        text(g, count, 177 - font.width(count), 36, enough ? GREEN : AMBER);

        boolean missed = menu.getMisses() > 0;
        text(g, Component.translatable("gui.iuce.hub.progress", menu.getMinutes(), tier.minutes()), 12, 110, missed ? RED : TEXT);
        String clock = String.format("0:%02d", Math.max(0, 59 - menu.getSeconds()));
        text(g, Component.literal(clock), 184 - font.width(clock), 110, DIM);
        int done = Math.min(BAR_W, (menu.getMinutes() * 60 + menu.getSeconds()) * BAR_W / (tier.minutes() * 60));
        if (done > 0) blit(g, BAR_X, BAR_Y, 0, missed ? 208 : 200, done, 6);

        Component hint = missed ? Component.translatable("gui.iuce.hub.grace") : Component.translatable("gui.iuce.hub.hint");
        text(g, hint, 12, 140, missed ? 0xFFB02818 : ENGRAVED);
        blit(g, 178, 141, missed ? 232 : enough ? 220 : 226, 0, 5, 5);
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

    private void text(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font, text, leftPos + x, topPos + y, color, false);
    }

    private void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int width, int height) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }
}
