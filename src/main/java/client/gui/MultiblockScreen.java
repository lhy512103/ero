package client.gui;

import blockentity.PlantControllerBlockEntity;
import multiblock.MultiblockMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class MultiblockScreen extends AbstractContainerScreen<MultiblockMenu> {
    private static final int SHELL = 0xF2222D36, INSET = 0xFF131F29, EDGE = 0xFF3E829E;
    private static final int CYAN = 0xFF70E2FF, TEXT = 0xFFE6F3F9, DIM = 0xFFA2BAC8;
    private static final int GREEN = 0xFF89E3A1, AMBER = 0xFFFFCD80, RED = 0xFFFF8C83;

    public MultiblockScreen(MultiblockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MultiblockMenu.WIDTH, MultiblockMenu.HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        panel(g, 0, 0, imageWidth, imageHeight, SHELL, EDGE);
        panel(g, 7, 7, 186, 22, INSET, EDGE);
        fill(g, 7, 7, 3, 22, CYAN);
        label(g, title, 16, 14, TEXT);
        fill(g, 176, 11, 3, 2, CYAN);
        fill(g, 181, 11, 3, 2, EDGE);
        fill(g, 186, 11, 3, 2, EDGE);
        panel(g, 7, 34, 186, 77, INSET, EDGE);
        switch (menu.kind()) {
            case PLANT, MILL -> controller(g);
            case INPUT, OUTPUT -> {
                label(g, Component.translatable(menu.kind() == MultiblockMenu.Kind.INPUT ? "gui.iuce.multiblock.feed" : "gui.iuce.multiblock.collect"), 15, 39, DIM);
            }
            case ENERGY -> {
                label(g, Component.translatable("gui.iuce.multiblock.energy", menu.energy(), menu.capacity()), 15, 46, CYAN);
                meter(g, 15, 61, 170, menu.energy(), menu.capacity(), CYAN);
                label(g, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), 15, 82, DIM);
            }
        }
        label(g, playerInventoryTitle, 20, 119, DIM);
        for (var slot : menu.slots) {
            panel(g, slot.x - 1, slot.y - 1, 18, 18, INSET, EDGE);
            fill(g, slot.x, slot.y, 16, 1, 0xFF0A151D);
        }
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private void controller(GuiGraphicsExtractor g) {
        var status = menu.status();
        int signal = switch (status) {
            case RUNNING -> GREEN;
            case CHECKING, NO_MOLD, LOW_POWER -> AMBER;
            case INCOMPLETE, BLOCKED -> RED;
            case IDLE -> CYAN;
        };
        fill(g, 15, 44, 4, 4, signal);
        label(g, Component.translatable("gui.iuce.multiblock.status." + status.name().toLowerCase(Locale.ROOT)), 23, 42, signal);
        int percent = Math.clamp(menu.progress() * 100 / PlantControllerBlockEntity.DURATION, 0, 100);
        label(g, Component.translatable("gui.iuce.multiblock.progress", percent), 15, 57, TEXT);
        boolean mill = menu.kind() == MultiblockMenu.Kind.MILL;
        meter(g, 15, 70, mill ? 122 : 170, menu.progress(), PlantControllerBlockEntity.DURATION, signal);
        label(g, Component.translatable("gui.iuce.multiblock.parallel", menu.parallel()), 15, 89, DIM);
        if (mill) {
            label(g, Component.translatable("gui.iuce.multiblock.mold"), 149, 57, DIM);
            label(g, Component.translatable("gui.iuce.multiblock.length", menu.length()), 110, 89, DIM);
        }
    }

    private void label(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font, text, leftPos + x, topPos + y, color, false);
    }

    private void meter(GuiGraphicsExtractor g, int x, int y, int width, int value, int max, int color) {
        panel(g, x, y, width, 9, 0xFF0B151E, EDGE);
        int filled = max > 0 ? Math.clamp((long) value * (width - 4) / max, 0, width - 4) : 0;
        if (filled > 0) fill(g, x + 2, y + 2, filled, 5, color);
    }

    private void panel(GuiGraphicsExtractor g, int x, int y, int width, int height, int background, int border) {
        fill(g, x, y, width, height, border);
        fill(g, x + 1, y + 1, width - 2, height - 2, background);
    }

    private void fill(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }
}
