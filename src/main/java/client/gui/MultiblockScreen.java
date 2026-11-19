package client.gui;

import blockentity.PlantControllerBlockEntity;
import multiblock.MultiblockMenu;
import multiblock.MultiblockMenu.Kind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Control panels of the large machines, drawn on the steel-plate textures in
 * {@code textures/gui/multiblock}. Each texture keeps its gauges and lamps to the right of the panel.
 */
public final class MultiblockScreen extends AbstractContainerScreen<MultiblockMenu> {
    private static final Identifier CONTROLLER = Identifier.parse("iuce:textures/gui/multiblock/controller.png");
    private static final Identifier HATCH = Identifier.parse("iuce:textures/gui/multiblock/hatch.png");
    private static final Identifier ENERGY_HATCH = Identifier.parse("iuce:textures/gui/multiblock/energy_hatch.png");
    private static final int TITLE = 0xFF2B3036, TEXT = 0xFFD8F5E0, DIM = 0xFF9FD0B0;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A, CYAN = 0xFF9AF0FF;
    private static final int LAMP_GREEN = 0, LAMP_AMBER = 1, LAMP_RED = 2, LAMP_CYAN = 3;

    public MultiblockScreen(MultiblockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MultiblockMenu.WIDTH, MultiblockMenu.height(menu.kind()));
    }

    private Identifier texture() {
        return switch (menu.kind()) {
            case PLANT, MILL -> CONTROLLER;
            case INPUT, OUTPUT -> HATCH;
            case ENERGY -> ENERGY_HATCH;
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        blit(g, 0, 0, 0, 0, imageWidth, imageHeight);
        g.text(font, title, leftPos + (imageWidth - font.width(title)) / 2, topPos + 6, TITLE, false);
        switch (menu.kind()) {
            case PLANT, MILL -> controller(g);
            case INPUT, OUTPUT -> hatch(g);
            case ENERGY -> energyHatch(g);
        }
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private void controller(GuiGraphicsExtractor g) {
        var status = menu.status();
        int lamp = switch (status) {
            case RUNNING -> LAMP_GREEN;
            case CHECKING, NO_MOLD, LOW_POWER -> LAMP_AMBER;
            case INCOMPLETE, BLOCKED -> LAMP_RED;
            case IDLE -> LAMP_CYAN;
        };
        int color = switch (lamp) { case LAMP_GREEN -> GREEN; case LAMP_AMBER -> AMBER; case LAMP_RED -> RED; default -> CYAN; };
        boolean mill = menu.kind() == Kind.MILL;
        if (mill) blit(g, 149, 18, 180, 64, 22, 22);
        blit(g, 154, 46, 200 + lamp * 14, 0, 12, 12);
        int percent = Math.clamp(menu.progress() * 100L / PlantControllerBlockEntity.DURATION, 0, 100);
        line(g, 0, Component.translatable("gui.iuce.multiblock.status." + status.name().toLowerCase(Locale.ROOT)), color);
        line(g, 1, Component.translatable("gui.iuce.multiblock.progress", percent), TEXT);
        line(g, 2, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), TEXT);
        line(g, 3, Component.translatable("gui.iuce.multiblock.stored", compact(menu.energy()), compact(menu.capacity())), DIM);
        if (mill) line(g, 4, Component.translatable("gui.iuce.multiblock.length", menu.length()), DIM);
        verticalGauge(g, 133, 22, 60, 180, 0, menu.energy(), menu.capacity());
        int filled = 158 * percent / 100;
        if (filled > 0) blit(g, 9, 90, 0, lamp == LAMP_GREEN ? 200 : 208, filled, 6);
    }

    private void hatch(GuiGraphicsExtractor g) {
        boolean input = menu.kind() == Kind.INPUT;
        int used = 0;
        for (int index = 0; index < 9; index++) if (menu.getSlot(index).hasItem()) used++;
        int flowX = input ? 7 : 121, infoX = input ? 121 : 7, width = input ? 50 : 48;
        Component flow = Component.translatable(input ? "gui.iuce.multiblock.in" : "gui.iuce.multiblock.out");
        centered(g, flow, flowX, width, 26, input ? GREEN : AMBER);
        blit(g, flowX + (width - 28) / 2, 42, 180, input ? 0 : 24, 28, 20);
        int otherWidth = input ? 48 : 50;
        centered(g, Component.translatable("gui.iuce.multiblock.slots"), infoX, otherWidth, 26, DIM);
        centered(g, Component.literal(used + "/9"), infoX, otherWidth, 44, used == 9 ? RED : TEXT);
        int lamp = used == 9 ? LAMP_RED : used > 0 ? (input ? LAMP_GREEN : LAMP_AMBER) : LAMP_CYAN;
        blit(g, infoX + (otherWidth - 12) / 2, 58, 180 + lamp * 14, 48, 12, 12);
    }

    private void energyHatch(GuiGraphicsExtractor g) {
        int energy = menu.energy(), capacity = menu.capacity();
        int percent = capacity > 0 ? (int) (energy * 100L / capacity) : 0;
        line(g, 0, Component.translatable("gui.iuce.multiblock.charge", percent), percent > 0 ? GREEN : AMBER);
        line(g, 1, Component.translatable("gui.iuce.multiblock.stored", compact(energy), compact(capacity)), TEXT);
        line(g, 2, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), TEXT);
        line(g, 3, Component.translatable("gui.iuce.multiblock.voltage", menu.parallel() >= 64 ? "HV" : "MV"), DIM);
        verticalGauge(g, 133, 21, 54, 180, 0, energy, capacity);
        blit(g, 154, 23, 180 + (percent > 0 ? LAMP_GREEN : LAMP_AMBER) * 14, 60, 12, 12);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (menu.kind() == Kind.INPUT || menu.kind() == Kind.OUTPUT) return;
        int bottom = MultiblockMenu.isController(menu.kind()) ? 85 : 77;
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= 131 && x < 147 && y >= 19 && y < bottom) {
            g.setTooltipForNextFrame(font, Component.translatable("gui.iuce.multiblock.energy", menu.energy(), menu.capacity()), mouseX, mouseY);
        }
    }

    /** Fills a bottom-up gauge from the strip at (u, v), whose full height equals {@code height}. */
    private void verticalGauge(GuiGraphicsExtractor g, int x, int y, int height, int u, int v, int value, int max) {
        int filled = max > 0 ? (int) Math.clamp((long) value * height / max, 0, height) : 0;
        if (filled > 0) blit(g, x, y + height - filled, u, v + height - filled, 12, filled);
    }

    private void line(GuiGraphicsExtractor g, int row, Component text, int color) {
        g.text(font, text, leftPos + 13, topPos + 25 + row * 11, color, false);
    }

    private void centered(GuiGraphicsExtractor g, Component text, int x, int width, int y, int color) {
        g.text(font, text, leftPos + x + (width - font.width(text)) / 2, topPos + y, color, false);
    }

    private void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int width, int height) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture(), leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }

    private static String compact(int value) {
        return value >= 10_000 ? String.format(Locale.ROOT, "%.1fk", value / 1000.0) : Integer.toString(value);
    }
}
