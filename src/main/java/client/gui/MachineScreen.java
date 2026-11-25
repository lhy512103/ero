package client.gui;

import machine.process.ProgressStyle.Mode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * The screens of the older machines (macerator, generator, ...) in the look of the process machines:
 * the vanilla panel and slot bevels drawn in code, the machine itself drawn behind the slots from
 * {@code textures/gui/machine/schematic/<picture>.png} (as wide as the panel, from row {@link #TOP} down
 * to just above the inventory label), and the run shown in a {@link Window} of that picture from the
 * sheet {@code <picture>_progress.png}, laid out like the sheets of
 * {@link machine.process.ProcessSchematic}: for {@link Mode#RIGHT} / {@link Mode#UP} row 0 is revealed as
 * the run advances and the next {@link Window#frames()} rows loop on top while it runs; for
 * {@link Mode#PHASE} there is one row per stage of the run.
 * <p>
 * gui.js {@code paintMachines} paints the pictures from the same window positions.
 * </p>
 */
public abstract class MachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    public static final int TOP = 16;
    static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, EDGE = 0xFF000000;
    static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737;
    static final int SCREEN = 0xFF1E2328, SCREEN_EDGE = 0xFF0E1114;
    static final int ENERGY = 0xFFE8C440, ENERGY_TOP = 0xFFFFF0A0;
    static final int LABEL = 0xFF404040;

    /** Where the run is shown, in panel coordinates, and how its sheet is laid out. */
    public record Window(int x, int y, int w, int h, Mode mode, int frames, boolean idleOverlay) {
        public int rows() { return mode == Mode.PHASE ? frames : 1 + frames; }

        public Rect2i area() { return new Rect2i(x, y, w, h); }
    }

    private record Hit(int x, int y, int w, int h, List<Component> lines) {}

    private final String picture;
    private final int inventoryY;
    private final List<Hit> hits = new ArrayList<>();
    /** False where machine slots sit on the inventory label's row. */
    protected boolean inventoryLabel = true;

    /**
     * @param picture    the picture's name under {@code textures/gui/machine/schematic/}
     * @param inventoryY the row of the player inventory's first slot
     */
    protected MachineScreen(M menu, Inventory inventory, Component title, int width, int height, String picture, int inventoryY) {
        super(menu, inventory, title, width, height);
        this.picture = picture;
        this.inventoryY = inventoryY;
        this.inventoryLabelY = inventoryY - 11;
    }

    public static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("iuce", "textures/gui/machine/schematic/" + name + ".png");
    }

    /** Rows of the picture: from {@link #TOP} to the row above the inventory label. */
    public static int pictureHeight(int inventoryY) { return inventoryY - 11 - TOP; }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        raised(g, leftPos, topPos, imageWidth, imageHeight, PANEL, LIGHT, SHADE);
        int h = pictureHeight(inventoryY);
        g.blit(RenderPipelines.GUI_TEXTURED, texture(picture), leftPos, topPos + TOP, 0, 0, imageWidth, h, imageWidth, h);
        for (Slot slot : menu.slots) if (slot.isActive()) slotFrame(g, slot);
        machine(g, mouseX, mouseY);
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    /** Draws the gauges and the run over the picture; called before the items are drawn. */
    protected abstract void machine(GuiGraphicsExtractor g, int mouseX, int mouseY);

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, LABEL, false);
        if (inventoryLabel) g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    // ---- parts

    /** A raised block in the vanilla bevel: black rounded edge, light top and left, shade bottom and right. */
    static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int face, int light, int shade) {
        g.fill(x + 1, y, x + w - 1, y + h, EDGE);
        g.fill(x, y + 1, x + w, y + h - 1, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, light);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, light);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, shade);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, shade);
    }

    /** A recessed rectangle with the panel's bevel around it. */
    static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_DARK);
        g.fill(x, y + h, x + w + 1, y + h + 1, LIGHT);
        g.fill(x + w, y, x + w + 1, y + h + 1, LIGHT);
        g.fill(x, y, x + w, y + h, fill);
    }

    static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT);
        g.fill(x, y, x + 17, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 17, x + 18, y + 18, LIGHT);
        g.fill(x + 17, y + 1, x + 18, y + 18, LIGHT);
    }

    private void slotFrame(GuiGraphicsExtractor g, Slot slot) {
        slotFrame(g, leftPos + slot.x - 1, topPos + slot.y - 1);
    }

    /** A tooltip over the panel area x, y, w, h. */
    protected void hit(int x, int y, int w, int h, List<Component> lines) {
        hits.add(new Hit(leftPos + x, topPos + y, w, h, lines));
    }

    /** A vertical energy gauge at panel position x, y, filling from the bottom. */
    protected void energy(GuiGraphicsExtractor g, int x, int y, int w, int h, long energy, long max) {
        int sx = leftPos + x, sy = topPos + y;
        inset(g, sx, sy, w, h, SCREEN);
        int filled = max > 0 ? (int) Math.clamp(energy * h / max, 0, h) : 0;
        if (filled > 0) {
            g.fill(sx, sy + h - filled, sx + w, sy + h, ENERGY);
            g.fill(sx, sy + h - filled, sx + w, sy + h - filled + 1, ENERGY_TOP);
        }
        for (int mark = 1; mark < 4; mark++) g.fill(sx, sy + h * mark / 4, sx + 2, sy + h * mark / 4 + 1, SCREEN_EDGE);
        hit(x, y, w, h, List.of(Component.translatable("gui.iuce.process.energy", energy, max)));
    }

    /** A horizontal energy gauge at panel position x, y, filling from the left. */
    protected void energyBar(GuiGraphicsExtractor g, int x, int y, int w, int h, long energy, long max) {
        int sx = leftPos + x, sy = topPos + y;
        inset(g, sx, sy, w, h, SCREEN);
        int filled = max > 0 ? (int) Math.clamp(energy * w / max, 0, w) : 0;
        if (filled > 0) {
            g.fill(sx, sy, sx + filled, sy + h, ENERGY);
            g.fill(sx, sy, sx + filled, sy + 1, ENERGY_TOP);
        }
        for (int mark = 1; mark < 4; mark++) g.fill(sx + w * mark / 4, sy + h - (mark == 2 ? 5 : 3), sx + w * mark / 4 + 1, sy + h, SCREEN_EDGE);
        hit(x, y, w, h, List.of(Component.translatable("gui.iuce.process.energy", energy, max)));
    }

    /** A tank gauge at panel position x, y; output tanks get an amber cap, as on the process machines. */
    protected void tank(GuiGraphicsExtractor g, int x, int y, int w, int h, Fluid fluid, int amount, int capacity, boolean input) {
        int sx = leftPos + x, sy = topPos + y;
        inset(g, sx, sy, w, h, SCREEN);
        int filled = capacity > 0 ? (int) Math.clamp((long) amount * h / capacity, amount > 0 ? 1 : 0, h) : 0;
        if (filled > 0 && fluid != Fluids.EMPTY) {
            int color = ProcessMachineScreen.fluidColor(fluid);
            g.fill(sx, sy + h - filled, sx + w, sy + h, color);
            g.fill(sx, sy + h - filled, sx + w, sy + h - filled + 1, lighter(color));
        }
        for (int mark = 1; mark < 4; mark++) g.fill(sx, sy + h * mark / 4, sx + (mark == 2 ? 6 : 3), sy + h * mark / 4 + 1, SCREEN_EDGE);
        if (!input) g.fill(sx, sy - 1, sx + w, sy, 0xFFB07A20);
        List<Component> lines = new ArrayList<>();
        lines.add(fluid == Fluids.EMPTY
            ? Component.translatable(input ? "gui.iuce.process.input_tank" : "gui.iuce.process.output_tank")
            : fluid.getFluidType().getDescription());
        lines.add(Component.literal(amount + " / " + capacity + " mB"));
        hit(x, y, w, h, lines);
    }

    /** The run in the picture's own window. */
    protected void run(GuiGraphicsExtractor g, Window window, int progress, int max) {
        run(g, picture + "_progress", window, progress, max, progress > 0, false);
        if (max > 0) hit(window.x(), window.y(), window.w(), window.h(),
            List.of(Component.translatable("gui.iuce.process.progress", progress, max)));
    }

    /**
     * Draws the sheet {@code sheet} in {@code window}: {@code progress / max} of the run is done,
     * {@code running} loops the overlay, {@code blocked} tints it red.
     */
    protected void run(GuiGraphicsExtractor g, String sheet, Window window, int progress, int max, boolean running, boolean blocked) {
        Identifier id = texture(sheet);
        int x = leftPos + window.x(), y = topPos + window.y(), w = window.w(), h = window.h();
        int texH = h * window.rows();
        int tint = blocked ? 0xFFE09090 : -1;
        float part = max > 0 ? Math.clamp((float) progress / max, 0f, 1f) : 0f;
        switch (window.mode()) {
            case PHASE -> {
                int row = Math.min(window.frames() - 1, (int) (part * window.frames()));
                g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, row * h, w, h, w, texH, tint);
            }
            case RIGHT -> {
                int filled = Math.round(part * w);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, filled, h, w, texH);
            }
            case UP -> {
                int filled = Math.round(part * h);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, id, x, y + h - filled, 0, h - filled, w, filled, w, texH);
            }
        }
        if (window.mode() != Mode.PHASE && window.frames() > 0 && (running || window.idleOverlay())) {
            int frame = running ? (int) (System.currentTimeMillis() / 120 % window.frames()) : 0;
            g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, (1 + frame) * h, w, h, w, texH, tint);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;
        for (Hit hit : hits) {
            if (mouseX < hit.x() || mouseX >= hit.x() + hit.w() || mouseY < hit.y() || mouseY >= hit.y() + hit.h()) continue;
            g.setComponentTooltipForNextFrame(font, hit.lines(), mouseX, mouseY);
            return;
        }
    }

    static int lighter(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40), gr = Math.min(255, ((color >> 8) & 0xFF) + 40), b = Math.min(255, (color & 0xFF) + 40);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }
}
