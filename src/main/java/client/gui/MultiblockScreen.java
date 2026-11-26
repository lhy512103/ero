package client.gui;

import blockentity.PlantControllerBlockEntity;
import client.StructureProjection;
import multiblock.MultiblockMenu;
import multiblock.MultiblockMenu.Kind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The multiblock controller and hatch screens on the plain panel: the status lines on a dark display,
 * the energy gauge and the status lamp beside it, the run's bar under it; the hatches show which way
 * the goods flow and how many of their slots are used. Controllers get a side panel with three tabs:
 * the structure at a glance, its material store, and building (world preview, build, dismantle, length).
 */
public final class MultiblockScreen extends MachineScreen<MultiblockMenu> {
    private static final int TEXT = 0xFFD8F5E0, DIM = 0xFF9FD0B0;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A, CYAN = 0xFF9AF0FF;
    private static final int LAMP_GREEN = 0xFF3CC85A, LAMP_AMBER = 0xFFE0A030, LAMP_RED = 0xFFD04030, LAMP_CYAN = 0xFF40B8D0;

    // side panel, in panel coordinates to the right of the main panel
    private static final int SIDE_GAP = 2, SIDE_W = 124, TAB_H = 18, TAB_W = 40;
    private static final int STATUS = 0, MATERIALS = 1, BUILD = 2;
    private int tab = STATUS;
    private final List<Button> buttons = new ArrayList<>();

    private record Button(int x, int y, int w, int h, Runnable action) {
        boolean over(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    public MultiblockScreen(MultiblockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MultiblockMenu.WIDTH, MultiblockMenu.height(menu.kind()), null,
            MultiblockMenu.inventoryY(menu.kind()));
    }

    private boolean hasSide() { return MultiblockMenu.isController(menu.kind()); }

    private PlantControllerBlockEntity controller() {
        return minecraft != null && minecraft.level != null
            && minecraft.level.getBlockEntity(menu.pos()) instanceof PlantControllerBlockEntity plant ? plant : null;
    }

    @Override
    protected void init() {
        super.init();
        // keep the main panel and the side panel centred together
        if (hasSide()) leftPos = (width - imageWidth - SIDE_GAP - SIDE_W) / 2;
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        switch (menu.kind()) {
            case PLANT, MILL -> { controller(g); side(g, mouseX, mouseY); }
            case INPUT, OUTPUT -> hatch(g);
            case ENERGY -> energyHatch(g);
        }
    }

    private void controller(GuiGraphicsExtractor g) {
        var status = menu.status();
        int lamp, color;
        switch (status) {
            case RUNNING -> { lamp = LAMP_GREEN; color = GREEN; }
            case CHECKING, NO_MOLD, LOW_POWER -> { lamp = LAMP_AMBER; color = AMBER; }
            case INCOMPLETE, BLOCKED -> { lamp = LAMP_RED; color = RED; }
            default -> { lamp = LAMP_CYAN; color = CYAN; }
        }
        boolean mill = menu.kind() == Kind.MILL;
        display(g, 8, 20, 120, 64);
        int percent = Math.clamp(menu.progress() * 100L / PlantControllerBlockEntity.DURATION, 0, 100);
        line(g, 0, Component.translatable("gui.iuce.multiblock.status." + status.name().toLowerCase(Locale.ROOT)), color);
        line(g, 1, Component.translatable("gui.iuce.multiblock.progress", percent), TEXT);
        line(g, 2, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), TEXT);
        line(g, 3, Component.translatable("gui.iuce.multiblock.stored", compact(menu.energy()), compact(menu.capacity())), DIM);
        if (mill) line(g, 4, Component.translatable("gui.iuce.multiblock.length", menu.length()), DIM);
        energy(g, 133, 22, 12, 60, menu.energy(), menu.capacity());
        lamp(g, 154, 46, 12, lamp);
        inset(g, leftPos + 9, topPos + 90, 158, 6, SCREEN);
        int filled = 158 * percent / 100;
        if (filled > 0) g.fill(leftPos + 9, topPos + 90, leftPos + 9 + filled, topPos + 96, lamp == LAMP_GREEN ? LAMP_GREEN : LAMP_AMBER);
    }

    // ---- side panel

    private int sideX() { return imageWidth + SIDE_GAP; }

    private void side(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        buttons.clear();
        PlantControllerBlockEntity plant = controller();
        int sx = leftPos + sideX(), sy = topPos;
        // tabs on top, the open one joined to the panel
        String[] names = {"status", "materials", "build"};
        for (int i = 0; i < 3; i++) {
            int tx = sx + i * (TAB_W + 2), open = i == tab ? 1 : 0;
            raised(g, tx, sy, TAB_W, TAB_H + open * 3, open == 1 ? PANEL : 0xFFA8A8A8, LIGHT, SHADE);
            Component label = Component.translatable("gui.iuce.multiblock.tab." + names[i]);
            g.text(font, label, tx + (TAB_W - font.width(label)) / 2, sy + 5, LABEL, false);
            int index = i;
            buttons.add(new Button(tx, sy, TAB_W, TAB_H, () -> tab = index));
        }
        int py = sy + TAB_H, ph = imageHeight - TAB_H;
        raised(g, sx, py, SIDE_W, ph, PANEL, LIGHT, SHADE);
        if (plant == null) return;
        List<Item> kinds = new ArrayList<>(plant.needs().keySet());
        Map<Item, Integer> needs = plant.needs();
        switch (tab) {
            case STATUS -> status(g, plant, kinds, needs, sx, py);
            case MATERIALS -> materials(g, kinds, needs, sx, py, mouseX, mouseY);
            default -> build(g, plant, sx, py, mouseX, mouseY);
        }
    }

    /** Completion ring, a lamp per kind of hatch and the parallel batteries. */
    private void status(GuiGraphicsExtractor g, PlantControllerBlockEntity plant, List<Item> kinds, Map<Item, Integer> needs, int sx, int py) {
        int total = 0, fitted = 0, wrong = 0;
        for (int i = 0; i < kinds.size() && i < MultiblockMenu.KINDS; i++) {
            total += needs.get(kinds.get(i));
            fitted += menu.fitted(i);
            wrong += menu.wrong(i);
        }
        // completion as a big segmented bar: green fitted, red wrong, dark missing
        inset(g, sx + 8, py + 8, SIDE_W - 16, 14, SCREEN);
        int bw = SIDE_W - 16;
        int fw = total > 0 ? bw * fitted / total : 0, ww = total > 0 ? bw * wrong / total : 0;
        if (fw > 0) g.fill(sx + 8, py + 8, sx + 8 + fw, py + 22, LAMP_GREEN);
        if (ww > 0) g.fill(sx + 8 + fw, py + 8, sx + 8 + fw + ww, py + 22, LAMP_RED);
        for (int m = 1; m < 10; m++) g.fill(sx + 8 + bw * m / 10, py + 8, sx + 9 + bw * m / 10, py + 22, 0x40000000);
        String pct = (total > 0 ? fitted * 100 / total : 0) + "%";
        g.text(font, pct, sx + 8 + (bw - font.width(pct)) / 2, py + 11, 0xFFFFFFFF, true);
        hit(sideX() + 8, TAB_H + 8, bw, 14, List.of(Component.translatable("gui.iuce.multiblock.completion", fitted, total, wrong)));

        // one icon and lamp per kind of block; green when complete, amber when partly, red when a wrong block sits there
        int cols = 5, x0 = sx + 8, y0 = py + 30;
        for (int i = 0; i < kinds.size() && i < MultiblockMenu.KINDS && i < 15; i++) {
            int x = x0 + (i % cols) * 22, y = y0 + (i / cols) * 24;
            int need = needs.get(kinds.get(i)), have = menu.fitted(i), bad = menu.wrong(i);
            slotFrame(g, x, y);
            g.item(new ItemStack(kinds.get(i)), x + 1, y + 1);
            int lampColor = bad > 0 ? LAMP_RED : have >= need ? LAMP_GREEN : have > 0 ? LAMP_AMBER : 0xFF505050;
            g.fill(x + 2, y + 19, x + 16, y + 21, lampColor);
            hit(x - leftPos, y - topPos, 18, 22, List.of(new ItemStack(kinds.get(i)).getHoverName(),
                Component.translatable("gui.iuce.multiblock.kind_progress", have, need, bad)));
        }

        // parallel as batteries: one cell per 16
        int cells = Math.min(8, (menu.parallel() + 15) / 16), by = py + imageHeight - TAB_H - 22;
        for (int c = 0; c < 8; c++) {
            int x = sx + 8 + c * 14;
            inset(g, x, by, 10, 12, SCREEN);
            g.fill(x + 3, by - 2, x + 7, by, 0xFF606060);
            if (c < cells) g.fill(x + 1, by + 1, x + 9, by + 11, c < 4 ? LAMP_GREEN : LAMP_CYAN);
        }
        hit(sideX() + 8, imageHeight - 22 - 2, 8 * 14, 14, List.of(Component.translatable("gui.iuce.multiblock.parallel", menu.parallel())));
    }

    /** The store: one slot per kind, "stored/needed" under it; click to put in or take out. */
    private void materials(GuiGraphicsExtractor g, List<Item> kinds, Map<Item, Integer> needs, int sx, int py, int mouseX, int mouseY) {
        int cols = 5, x0 = sx + 8, y0 = py + 8;
        for (int i = 0; i < kinds.size() && i < MultiblockMenu.KINDS; i++) {
            int x = x0 + (i % cols) * 22, y = y0 + (i / cols) * 28;
            Item item = kinds.get(i);
            int need = needs.get(item), stored = menu.stored(i);
            slotFrame(g, x, y);
            g.item(new ItemStack(item), x + 1, y + 1);
            // fill gauge under the slot
            inset(g, x, y + 19, 18, 3, SCREEN);
            int fill = need > 0 ? Math.min(18, 18 * stored / need) : 0;
            if (fill > 0) g.fill(x, y + 19, x + fill, y + 22, stored >= need ? LAMP_GREEN : LAMP_AMBER);
            String count = compact(stored);
            g.pose().pushMatrix();
            g.pose().translate(x + 9, y + 23);
            g.pose().scale(0.5f, 0.5f);
            g.text(font, count + "/" + compact(need), -font.width(count + "/" + compact(need)) / 2, 0, LABEL, false);
            g.pose().popMatrix();
            hit(x - leftPos, y - topPos, 18, 26, List.of(new ItemStack(item).getHoverName(),
                Component.translatable("gui.iuce.multiblock.store_count", stored, need),
                Component.translatable("gui.iuce.multiblock.store_hint")));
            int index = i;
            buttons.add(new Button(x, y, 18, 26, () -> storeClick(index)));
        }
    }

    private boolean shift, right;

    private void storeClick(int index) {
        int action = right ? (shift ? MultiblockMenu.WITHDRAW_ALL : MultiblockMenu.WITHDRAW)
            : (shift ? MultiblockMenu.DEPOSIT_ALL : MultiblockMenu.DEPOSIT);
        send(action + index);
    }

    /** World preview toggle, build, dismantle and the length control. */
    private void build(GuiGraphicsExtractor g, PlantControllerBlockEntity plant, int sx, int py, int mouseX, int mouseY) {
        int bx = sx + 8, bw = SIDE_W - 16, y = py + 8;
        boolean pinned = StructureProjection.pinned(menu.pos());
        button(g, bx, y, bw, Component.translatable(pinned ? "gui.iuce.multiblock.preview_on" : "gui.iuce.multiblock.preview_off"),
            pinned, mouseX, mouseY, () -> StructureProjection.togglePin(menu.pos()));
        y += 22;
        button(g, bx, y, bw, Component.translatable("gui.iuce.multiblock.build"), false, mouseX, mouseY,
            () -> send(MultiblockMenu.BUILD));
        hit(bx - leftPos, y - topPos, bw, 16, List.of(Component.translatable("gui.iuce.multiblock.build_hint")));
        y += 20;
        button(g, bx, y, bw, Component.translatable("gui.iuce.multiblock.dismantle"), false, mouseX, mouseY,
            () -> send(MultiblockMenu.DISMANTLE));
        hit(bx - leftPos, y - topPos, bw, 16, List.of(Component.translatable("gui.iuce.multiblock.dismantle_hint")));
        y += 26;
        if (!plant.extensible()) return;
        // length: [<] ====|==== [>] with the current length over the track
        int len = PlantControllerBlockEntity.targetLength(plant.getBlockState());
        int min = plant.minimumLength(), max = plant.maximumLength();
        Component label = Component.translatable("gui.iuce.multiblock.length", len);
        g.text(font, label, sx + (SIDE_W - font.width(label)) / 2, y, LABEL, false);
        y += 12;
        button(g, bx, y, 14, Component.literal("<"), false, mouseX, mouseY, () -> send(MultiblockMenu.SHORTER));
        button(g, bx + bw - 14, y, 14, Component.literal(">"), false, mouseX, mouseY, () -> send(MultiblockMenu.LONGER));
        int tx = bx + 18, tw = bw - 36;
        inset(g, tx, y + 6, tw, 4, SCREEN);
        int steps = Math.max(1, (max - min) / plant.lengthStep());
        for (int s = 0; s <= steps; s++) g.fill(tx + tw * s / steps, y + 3, tx + tw * s / steps + 1, y + 13, 0xFF707070);
        int knob = tx + (max > min ? tw * (len - min) / (max - min) : 0);
        raised(g, knob - 3, y + 1, 7, 14, 0xFFE0E0E0, LIGHT, SHADE);
        hit(tx - leftPos, y - topPos, tw, 16, List.of(Component.translatable("gui.iuce.multiblock.length_range", min, max)));
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, Component text, boolean on, int mx, int my, Runnable action) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 16;
        raised(g, x, y, w, 16, on ? 0xFF5FA8C8 : hover ? 0xFFDADADA : 0xFFC0C0C0, LIGHT, SHADE);
        g.text(font, text, x + (w - font.width(text)) / 2, y + 4, on ? 0xFFFFFFFF : LABEL, false);
        buttons.add(new Button(x, y, w, 16, action));
    }

    private void send(int action) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (hasSide() && (event.button() == 0 || event.button() == 1)) {
            shift = event.hasShiftDown();
            right = event.button() == 1;
            for (Button b : List.copyOf(buttons)) {
                if (b.action() != null && b.over(event.x(), event.y())) {
                    if (right && tab != MATERIALS) return true;
                    b.action().run();
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        if (hasSide() && mx >= leftPos + sideX() && mx < leftPos + sideX() + SIDE_W && my >= topPos && my < topPos + imageHeight) return false;
        return super.hasClickedOutside(mx, my, xo, yo);
    }

    // ---- hatches

    private void hatch(GuiGraphicsExtractor g) {
        boolean input = menu.kind() == Kind.INPUT;
        int used = 0;
        for (int index = 0; index < 9; index++) if (menu.getSlot(index).hasItem()) used++;
        int flowX = input ? 7 : 121, infoX = input ? 121 : 7, width = input ? 50 : 48, otherWidth = input ? 48 : 50;
        display(g, flowX, 20, width, 54);
        display(g, infoX, 20, otherWidth, 54);
        Component flow = Component.translatable(input ? "gui.iuce.multiblock.in" : "gui.iuce.multiblock.out");
        centered(g, flow, flowX, width, 26, input ? GREEN : AMBER);
        arrow(g, flowX + (width - 28) / 2, 42, input ? LAMP_GREEN : LAMP_AMBER);
        centered(g, Component.translatable("gui.iuce.multiblock.slots"), infoX, otherWidth, 26, DIM);
        centered(g, Component.literal(used + "/9"), infoX, otherWidth, 44, used == 9 ? RED : TEXT);
        int lamp = used == 9 ? LAMP_RED : used > 0 ? (input ? LAMP_GREEN : LAMP_AMBER) : LAMP_CYAN;
        lamp(g, infoX + (otherWidth - 12) / 2, 58, 12, lamp);
    }

    private void energyHatch(GuiGraphicsExtractor g) {
        int energy = menu.energy(), capacity = menu.capacity();
        int percent = capacity > 0 ? (int) (energy * 100L / capacity) : 0;
        display(g, 8, 20, 120, 46);
        line(g, 0, Component.translatable("gui.iuce.multiblock.charge", percent), percent > 0 ? GREEN : AMBER);
        line(g, 1, Component.translatable("gui.iuce.multiblock.stored", compact(energy), compact(capacity)), TEXT);
        line(g, 2, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), TEXT);
        line(g, 3, Component.translatable("gui.iuce.multiblock.voltage", menu.parallel() >= 64 ? "HV" : "MV"), DIM);
        energy(g, 133, 21, 12, 54, energy, capacity);
        lamp(g, 154, 23, 12, percent > 0 ? LAMP_GREEN : LAMP_AMBER);
    }

    /** A 28 x 20 arrow pointing right: the way the goods go through the hatch. */
    private void arrow(GuiGraphicsExtractor g, int x, int y, int color) {
        for (int col = 0; col < 28; col++) {
            int top, bottom;
            if (col < 17) { top = 7; bottom = 12; }
            else { int half = Math.round(9.5f - (col - 17) * 0.95f); top = 10 - half; bottom = 9 + half; }
            if (bottom < top) continue;
            g.fill(leftPos + x + col, topPos + y + top, leftPos + x + col + 1, topPos + y + bottom + 1, color);
            g.fill(leftPos + x + col, topPos + y + top, leftPos + x + col + 1, topPos + y + top + 1, lighter(color));
        }
    }

    private void line(GuiGraphicsExtractor g, int row, Component text, int color) {
        text(g, text, 13, 25 + row * 11, color);
    }

    private void centered(GuiGraphicsExtractor g, Component text, int x, int width, int y, int color) {
        text(g, text, x + (width - font.width(text)) / 2, y, color);
    }

    private static String compact(int value) {
        return value >= 10_000 ? String.format(Locale.ROOT, "%.1fk", value / 1000.0) : Integer.toString(value);
    }
}
