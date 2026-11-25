package client.gui;

import blockentity.PlantControllerBlockEntity;
import multiblock.MultiblockMenu;
import multiblock.MultiblockMenu.Kind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * The multiblock controller and hatch screens on the plain panel: the status lines on a dark display,
 * the energy gauge and the status lamp beside it, the run's bar under it; the hatches show which way
 * the goods flow and how many of their slots are used.
 */
public final class MultiblockScreen extends MachineScreen<MultiblockMenu> {
    private static final int TEXT = 0xFFD8F5E0, DIM = 0xFF9FD0B0;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A, CYAN = 0xFF9AF0FF;
    private static final int LAMP_GREEN = 0xFF3CC85A, LAMP_AMBER = 0xFFE0A030, LAMP_RED = 0xFFD04030, LAMP_CYAN = 0xFF40B8D0;

    public MultiblockScreen(MultiblockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MultiblockMenu.WIDTH, MultiblockMenu.height(menu.kind()), null,
            MultiblockMenu.inventoryY(menu.kind()));
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        switch (menu.kind()) {
            case PLANT, MILL -> controller(g);
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
