package machine.process;

import java.util.ArrayList;
import java.util.List;

/**
 * Where everything sits on a process machine's panel, worked out from its {@link ProcessMachineDef}
 * so the menu (slot positions) and the screen (drawing) always agree.
 * <p>
 * A machine with a {@link ProcessSchematic} takes its positions from that picture. Otherwise the
 * process page runs left to right in the order of the work: energy gauge, input tanks, input slots,
 * the progress picture, output slots and output tanks, with catalyst and card slots under the
 * progress picture. Upgrade slots live on their own page, in a row across the page area. The panel
 * grows wider than the usual 176 pixels only when a machine needs it. Slot coordinates are item
 * positions (the 18-pixel frame starts one pixel up and left).
 * </p>
 */
public final class ProcessLayout {
    public static final int HEIGHT = 181;
    public static final int CONTENT_TOP = 18, CONTENT_BOTTOM = 74;
    public static final int TANK_W = 16, TANK_H = 52, TANK_Y = 20;
    public static final int ENERGY_W = 7;
    public static final int PROGRESS_H = 16;
    public static final int STATUS_Y = 78;
    public static final int INVENTORY_LABEL_Y = 88;
    public static final int INVENTORY_Y = 99, HOTBAR_Y = 157;
    /** The area the other pages draw in: between the title and the status line. */
    public static final int PAGE_TOP = 16, PAGE_BOTTOM = 75;
    public static final int UPGRADE_Y = 30;

    public record Pos(int x, int y) {}

    public final int width;
    public final int energyX;
    public final List<Pos> tanks = new ArrayList<>();      // input tanks first, then output tanks
    public final List<Pos> inputs = new ArrayList<>();
    public final List<Pos> outputs = new ArrayList<>();
    public final List<Pos> upgrades = new ArrayList<>();
    /** Top-left of the progress picture, {@link #progressW} by {@link #progressH}. */
    public final Pos arrow;
    public final int progressW, progressH;
    public final Pos catalyst;                               // null without a catalyst slot
    public final Pos card;                                   // null without a card slot
    public final int inventoryX;
    /** The machine's own picture, or null for the generic row. */
    public final ProcessSchematic schematic;

    public ProcessLayout(ProcessMachineDef def) {
        this.schematic = ProcessSchematic.of(def);
        if (schematic != null) {
            this.width = ProcessSchematic.WIDTH;
            this.inventoryX = 8;
            this.energyX = schematic.energyX();
            tanks.addAll(schematic.tanks());
            inputs.addAll(schematic.inputs());
            outputs.addAll(schematic.outputs());
            this.arrow = schematic.window();
            this.progressW = schematic.windowW();
            this.progressH = schematic.windowH();
            this.catalyst = schematic.catalyst();
            this.card = schematic.card();
        } else {
            this.progressW = def.progress().width();
            this.progressH = PROGRESS_H;
            int content = ENERGY_W + 6
                + def.fluidsIn().size() * (TANK_W + 4) + 2
                + gridWidth(def.itemsIn()) + 6
                + progressW + 6
                + gridWidth(def.itemsOut()) + 2
                + def.fluidsOut().size() * (TANK_W + 4);
            this.width = Math.max(176, content + 16);
            this.inventoryX = (width - 176) / 2 + 8;

            int x = 8 + (width - 16 - content) / 2;
            this.energyX = x;
            x += ENERGY_W + 6;
            for (int i = 0; i < def.fluidsIn().size(); i++) {
                tanks.add(new Pos(x, TANK_Y));
                x += TANK_W + 4;
            }
            x += 2;
            x = grid(inputs, def.itemsIn(), x) + 6;
            int centerY = (CONTENT_TOP + CONTENT_BOTTOM) / 2;
            int lift = def.catalystSlot() || def.cardSlot() ? 9 : 0;   // make room for the slots below
            this.arrow = new Pos(x, centerY - PROGRESS_H / 2 - lift);
            int below = arrow.y() + PROGRESS_H + 4;
            int middle = x + progressW / 2;
            if (def.catalystSlot() && def.cardSlot()) {
                catalyst = new Pos(middle - 18, below);
                card = new Pos(middle + 1, below);
            } else {
                catalyst = def.catalystSlot() ? new Pos(middle - 8, below) : null;
                card = def.cardSlot() ? new Pos(middle - 8, below) : null;
            }
            x += progressW + 6;
            x = grid(outputs, def.itemsOut(), x) + 2;
            for (int i = 0; i < def.fluidsOut().size(); i++) {
                tanks.add(new Pos(x, TANK_Y));
                x += TANK_W + 4;
            }
        }
        int upgradeX = (width - def.upgrades() * 22) / 2 + 3;
        for (int i = 0; i < def.upgrades(); i++) upgrades.add(new Pos(upgradeX + i * 22, UPGRADE_Y));
    }

    private static int rows(int count) {
        return count <= 3 ? 1 : count <= 6 ? 2 : 3;
    }

    private static int gridWidth(int count) {
        if (count <= 0) return 0;
        return ceilDiv(count, rows(count)) * 18;
    }

    /** Lays {@code count} slots out in a grid starting at {@code x}; returns the x after it. */
    private static int grid(List<Pos> into, int count, int x) {
        if (count <= 0) return x;
        int rows = rows(count);
        int cols = ceilDiv(count, rows);
        int top = (CONTENT_TOP + CONTENT_BOTTOM) / 2 - rows * 9 + 1;
        for (int i = 0; i < count; i++) into.add(new Pos(x + 1 + (i % cols) * 18, top + (i / cols) * 18));
        return x + cols * 18;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
