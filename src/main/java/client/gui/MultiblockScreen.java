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
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.HashMap;
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

    // side tabs down the left edge, as on the process machines
    private static final int TAB_W = 22, TAB_H = 22, TAB_STEP = 24, TAB_Y = 6;
    private static final int TAB_OFF = 0xFFA9A9A9, TAB_OFF_LIGHT = 0xFFDADADA, TAB_OFF_SHADE = 0xFF6A6A6A;
    private static final int MACHINE = 0, MATERIALS = 1, BUILD = 2, TABS = 3;
    private int tab = MACHINE;
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
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        switch (menu.kind()) {
            case PLANT, MILL -> side(g, mouseX, mouseY);
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
        int row = 2;
        if (menu.parallel() > 1) line(g, row++, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), TEXT);
        line(g, row++, Component.translatable("gui.iuce.multiblock.stored", compact(menu.energy()), compact(menu.capacity())), DIM);
        if (mill) line(g, row, Component.translatable("gui.iuce.multiblock.length", menu.length()), DIM);
        energy(g, 133, 22, 12, 60, menu.energy(), menu.capacity());
        lamp(g, 154, 46, 12, lamp);
        inset(g, leftPos + 9, topPos + 90, 158, 6, SCREEN);
        int filled = 158 * percent / 100;
        if (filled > 0) g.fill(leftPos + 9, topPos + 90, leftPos + 9 + filled, topPos + 96, lamp == LAMP_GREEN ? LAMP_GREEN : LAMP_AMBER);
    }

    // ---- side tabs

    private static final String[] TAB_NAMES = {"machine", "status", "materials", "build"};
    private static final int STATUS_TAB = 3;
    /** Run history sampled on the client once a second: 0 idle, 1 running, 2 stalled. Costs the server nothing. */
    private final byte[] history = new byte[60];
    private int historyAt, historyTicks;
    private final Map<Integer, List<Component>> storeTips = new HashMap<>();

    private int tabCount() { return hasSide() ? TAB_NAMES.length : 0; }

    private static int tabPage(int index) { return switch (index) { case 0 -> MACHINE; case 1 -> STATUS_TAB; case 2 -> MATERIALS; default -> BUILD; }; }

    /** The side tabs, for JEI so it keeps its item list clear of them. */
    public List<Rect2i> tabAreas() {
        if (!hasSide()) return List.of();
        return List.of(new Rect2i(leftPos - TAB_W + 1, topPos + TAB_Y, TAB_W, tabCount() * TAB_STEP));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!hasSide() || ++historyTicks < 20) return;
        historyTicks = 0;
        var status = menu.status();
        history[historyAt] = (byte) (status == MultiblockMenu.Status.RUNNING ? 1
            : status == MultiblockMenu.Status.BLOCKED || status == MultiblockMenu.Status.LOW_POWER ? 2 : 0);
        historyAt = (historyAt + 1) % history.length;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (hasSide()) {
            menu.storeShown = tab == MATERIALS;
            menu.machineShown = tab == MACHINE;
            for (int i = 0; i < tabCount(); i++) if (tabPage(i) != tab) tab(g, i, false);
        }
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private void tab(GuiGraphicsExtractor g, int index, boolean active) {
        int x = leftPos - TAB_W + 1, y = topPos + TAB_Y + index * TAB_STEP;
        if (active) {
            g.fill(x + 1, y, leftPos + 3, y + TAB_H, EDGE);
            g.fill(x, y + 1, leftPos + 3, y + TAB_H - 1, EDGE);
            g.fill(x + 1, y + 1, leftPos + 3, y + TAB_H - 1, PANEL);
            g.fill(x + 1, y + 1, leftPos + 1, y + 3, LIGHT);
            g.fill(x + 1, y + 1, x + 3, y + TAB_H - 2, LIGHT);
            g.fill(x + 3, y + TAB_H - 3, leftPos + 1, y + TAB_H - 1, SHADE);
        } else {
            raised(g, x, y, TAB_W, TAB_H, TAB_OFF, TAB_OFF_LIGHT, TAB_OFF_SHADE);
        }
        ItemStack icon = new ItemStack(switch (tabPage(index)) {
            case MACHINE -> menu.kind() == Kind.MILL ? Items.PISTON : Items.BLAST_FURNACE;
            case STATUS_TAB -> Items.CLOCK;
            case MATERIALS -> Items.CHEST;
            default -> Items.IRON_PICKAXE;
        });
        g.item(icon, x + 4, y + 3);
    }

    private void side(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        buttons.clear();
        for (int i = 0; i < tabCount(); i++) {
            int page = tabPage(i), x = leftPos - TAB_W + 1, y = topPos + TAB_Y + i * TAB_STEP;
            buttons.add(new Button(x, y, TAB_W, TAB_H, () -> tab = page));
            hit(x - leftPos, y - topPos, TAB_W, TAB_H, List.of(Component.translatable("gui.iuce.multiblock.tab." + TAB_NAMES[i])));
            if (page == tab) tab(g, i, true);
        }
        PlantControllerBlockEntity plant = controller();
        if (tab == MACHINE) { controller(g); return; }
        if (plant == null) return;
        switch (tab) {
            case STATUS_TAB -> status(g, plant);
            case MATERIALS -> materials(g, plant);
            default -> build(g, plant, mouseX, mouseY);
        }
    }

    /** Placement bar, a minute of run history as a line, and the stored energy as a pie. */
    private void status(GuiGraphicsExtractor g, PlantControllerBlockEntity plant) {
        Map<Item, Integer> needs = plant.needs();
        List<Item> kinds = new ArrayList<>(needs.keySet());
        int total = 0, fitted = 0, wrong = 0;
        for (int i = 0; i < kinds.size() && i < MultiblockMenu.KINDS; i++) {
            total += needs.get(kinds.get(i));
            fitted += menu.fitted(i);
            wrong += menu.wrong(i);
        }
        int x = leftPos + 8, w = 160;
        text(g, Component.translatable("gui.iuce.multiblock.placement"), 8, 17, LABEL);
        inset(g, x, topPos + 27, w, 10, SCREEN);
        int fw = total > 0 ? w * fitted / total : 0, ww = total > 0 ? w * wrong / total : 0;
        if (fw > 0) g.fill(x, topPos + 27, x + fw, topPos + 37, LAMP_GREEN);
        if (ww > 0) g.fill(x + fw, topPos + 27, x + fw + ww, topPos + 37, LAMP_RED);
        String pct = (total > 0 ? fitted * 100 / total : 0) + "%";
        g.text(font, pct, x + (w - font.width(pct)) / 2, topPos + 28, 0xFFFFFFFF, true);
        hit(8, 27, w, 10, List.of(Component.translatable("gui.iuce.multiblock.completion", fitted, total, wrong)));

        // run history: one point per second over the last minute
        text(g, Component.translatable("gui.iuce.multiblock.history"), 8, 42, LABEL);
        int cx = leftPos + 8, cy = topPos + 52, cw = 120, ch = 42;
        inset(g, cx, cy, cw, ch, SCREEN);
        for (int m = 1; m < 4; m++) g.fill(cx, cy + ch * m / 4, cx + cw, cy + ch * m / 4 + 1, 0xFF2A3036);
        int running = 0, prevY = -1;
        for (int k = 0; k < history.length; k++) {
            int v = history[(historyAt + k) % history.length];
            if (v == 1) running++;
            int px = cx + k * cw / history.length, py = cy + (v == 1 ? 4 : v == 2 ? ch / 2 : ch - 5);
            int color = v == 1 ? LAMP_GREEN : v == 2 ? LAMP_AMBER : 0xFF607080;
            if (prevY >= 0) g.fill(px, Math.min(prevY, py), px + 1, Math.max(prevY, py) + 1, color);
            g.fill(px, py, px + cw / history.length + 1, py + 1, color);
            prevY = py;
        }
        hit(8, 52, cw, ch, List.of(Component.translatable("gui.iuce.multiblock.uptime", running * 100 / history.length)));

        // energy pie
        int energy = menu.energy(), capacity = menu.capacity();
        float share = capacity > 0 ? (float) energy / capacity : 0f;
        int r = 17, ox = leftPos + 150, oy = topPos + 73;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy > r * r) continue;
                double a = (Math.atan2(dx, -dy) / (2 * Math.PI) + 1) % 1;
                g.fill(ox + dx, oy + dy, ox + dx + 1, oy + dy + 1, a < share ? ENERGY : SCREEN);
            }
        }
        text(g, Component.literal("EU"), 144, 42, LABEL);
        hit(133, 56, 2 * r, 2 * r, List.of(Component.translatable("gui.iuce.multiblock.stored", compact(energy), compact(capacity))));
    }

    /** Tints the store's slots: green placed, cyan enough in the store, amber missing, red a wrong block in the way. */
    private void materials(GuiGraphicsExtractor g, PlantControllerBlockEntity plant) {
        Map<Item, Integer> needs = plant.needs();
        List<Item> kinds = new ArrayList<>(needs.keySet());
        storeTips.clear();
        for (Slot slot : menu.slots) {
            if (!(slot instanceof MultiblockMenu.StoreSlot store) || !store.isActive()) continue;
            int i = store.index();
            if (i >= kinds.size()) continue;
            Item item = kinds.get(i);
            int x = leftPos + slot.x, y = topPos + slot.y;
            int need = needs.get(item), fit = menu.fitted(i), bad = menu.wrong(i), stored = menu.stored(i);
            int color = bad > 0 ? 0xA0D04030 : fit >= need ? 0xA03CC85A : fit + stored >= need ? 0xA040B8D0 : 0xA0E0A030;
            g.fill(x, y, x + 16, y + 16, color);
            if (!slot.hasItem()) {
                g.item(new ItemStack(item), x, y);
                g.fill(x, y, x + 16, y + 16, 0x90C6C6C6);
            }
            List<Component> lines = new ArrayList<>();
            lines.add(new ItemStack(item).getHoverName());
            lines.add(Component.translatable("gui.iuce.multiblock.kind_progress", fit, need, bad));
            lines.add(Component.translatable("gui.iuce.multiblock.store_count", stored, need));
            if (bad > 0) lines.add(Component.translatable("gui.iuce.multiblock.store_wrong", bad).withColor(RED));
            else if (fit >= need) lines.add(Component.translatable("gui.iuce.multiblock.store_done").withColor(GREEN));
            else if (fit + stored < need) lines.add(Component.translatable("gui.iuce.multiblock.store_missing", need - fit - stored).withColor(AMBER));
            else lines.add(Component.translatable("gui.iuce.multiblock.store_ready").withColor(CYAN));
            storeTips.put(i, lines);
        }
    }

    /** World preview toggle, build, dismantle and the length control. */
    private void build(GuiGraphicsExtractor g, PlantControllerBlockEntity plant, int mouseX, int mouseY) {
        int bx = leftPos + 8, bw = 160, y = topPos + 20;
        boolean pinned = StructureProjection.pinned(menu.pos());
        button(g, bx, y, bw, Component.translatable(pinned ? "gui.iuce.multiblock.preview_on" : "gui.iuce.multiblock.preview_off"),
            pinned, mouseX, mouseY, () -> StructureProjection.togglePin(menu.pos()));
        y += 20;
        int half = (bw - 4) / 2;
        button(g, bx, y, half, Component.translatable("gui.iuce.multiblock.build"), false, mouseX, mouseY,
            () -> send(MultiblockMenu.BUILD));
        hit(bx - leftPos, y - topPos, half, 16, List.of(Component.translatable("gui.iuce.multiblock.build_hint")));
        button(g, bx + half + 4, y, half, Component.translatable("gui.iuce.multiblock.dismantle"), false, mouseX, mouseY,
            () -> send(MultiblockMenu.DISMANTLE));
        hit(bx + half + 4 - leftPos, y - topPos, half, 16, List.of(Component.translatable("gui.iuce.multiblock.dismantle_hint")));
        y += 24;
        if (!plant.extensible()) return;
        int len = PlantControllerBlockEntity.targetLength(plant.getBlockState());
        int min = plant.minimumLength(), max = plant.maximumLength();
        Component label = Component.translatable("gui.iuce.multiblock.length", len);
        g.text(font, label, leftPos + (imageWidth - font.width(label)) / 2, y, LABEL, false);
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
        if (hasSide() && event.button() == 0) {
            for (Button b : List.copyOf(buttons)) {
                if (b.over(event.x(), event.y())) {
                    b.action().run();
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (tab == MATERIALS && hoveredSlot instanceof MultiblockMenu.StoreSlot store && storeTips.containsKey(store.index())) {
            g.setComponentTooltipForNextFrame(font, storeTips.get(store.index()), mouseX, mouseY);
            return;
        }
        super.extractTooltip(g, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        for (Rect2i area : tabAreas()) if (area.contains((int) mx, (int) my)) return false;
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
