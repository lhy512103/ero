package client.gui;

import machine.process.ProcessLayout;
import machine.process.ProcessMachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The screen of every process machine. Nothing is a fixed texture: the panel, slots, tanks, gauge
 * and arrow are drawn from {@link ProcessLayout}, so a new machine needs no GUI work.
 */
public class ProcessMachineScreen extends AbstractContainerScreen<ProcessMachineMenu> {
    private static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, EDGE = 0xFF000000;
    private static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737;
    private static final int SCREEN = 0xFF1E2328, SCREEN_EDGE = 0xFF0E1114;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A, DIM = 0xFF9FD0B0;
    private static final int ENERGY = 0xFFE8C440, ENERGY_TOP = 0xFFFFF0A0;

    private final ProcessLayout layout;

    public ProcessMachineScreen(ProcessMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.layout().width, ProcessLayout.HEIGHT);
        this.layout = menu.layout();
        this.inventoryLabelX = layout.inventoryX;
        this.inventoryLabelY = ProcessLayout.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        if (menu.def().id().equals("cnc_machining_center") || menu.def().id().equals("single_screw_extruder")) {
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("›"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + imageWidth - 22, topPos + 4, 16, 14).build());
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        panel(g);
        for (var slot : menu.slots) slotFrame(g, slot.x, slot.y);
        energy(g);
        for (int i = 0; i < layout.tanks.size(); i++) tank(g, i);
        arrow(g);
        if (menu.def().id().equals("cnc_machining_center") || menu.def().id().equals("single_screw_extruder")) {
            String mold = menu.def().id().equals("single_screw_extruder") ? (menu.mold() == 0 ? "pe_pipe" : "drip_tape") : machine.process.ProcessMachineBlockEntity.toolItem(menu.mold());
            g.item(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("iuce:" + mold))), leftPos + imageWidth - 42, topPos + 3);
        }
        status(g);
        if (client.gui.component.MachineInfoPanel.hasInfo(menu.def().id())) infoButton(g);
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private void panel(GuiGraphicsExtractor g) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x + 1, y, x + w - 1, y + h, EDGE);
        g.fill(x, y + 1, x + w, y + h - 1, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADE);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADE);
    }

    /** The "i" button MachineInfoPanel listens for in the top-left corner. */
    private void infoButton(GuiGraphicsExtractor g) {
        int x = leftPos + 3, y = topPos + 3;
        g.fill(x, y, x + 13, y + 13, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 12, y + 12, 0xFF6A8CAF);
        g.fill(x + 6, y + 3, x + 8, y + 5, LIGHT);
        g.fill(x + 6, y + 6, x + 8, y + 11, LIGHT);
    }

    private void slotFrame(GuiGraphicsExtractor g, int sx, int sy) {
        int x = leftPos + sx - 1, y = topPos + sy - 1;
        g.fill(x, y, x + 18, y + 18, SLOT);
        g.fill(x, y, x + 17, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 17, x + 18, y + 18, LIGHT);
        g.fill(x + 17, y + 1, x + 18, y + 18, LIGHT);
    }

    /** A dark recessed rectangle with the panel's bevel around it. */
    private void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_DARK);
        g.fill(x, y + h, x + w + 1, y + h + 1, LIGHT);
        g.fill(x + w, y, x + w + 1, y + h + 1, LIGHT);
        g.fill(x, y, x + w, y + h, SCREEN);
    }

    private void energy(GuiGraphicsExtractor g) {
        int x = leftPos + layout.energyX, y = topPos + ProcessLayout.TANK_Y, h = ProcessLayout.TANK_H;
        inset(g, x, y, ProcessLayout.ENERGY_W, h);
        long max = menu.maxEnergy();
        int filled = max > 0 ? (int) Math.clamp(menu.energy() * h / max, 0, h) : 0;
        if (filled > 0) {
            g.fill(x, y + h - filled, x + ProcessLayout.ENERGY_W, y + h, ENERGY);
            g.fill(x, y + h - filled, x + ProcessLayout.ENERGY_W, y + h - filled + 1, ENERGY_TOP);
        }
    }

    private void tank(GuiGraphicsExtractor g, int index) {
        var pos = layout.tanks.get(index);
        int x = leftPos + pos.x(), y = topPos + pos.y(), w = ProcessLayout.TANK_W, h = ProcessLayout.TANK_H;
        inset(g, x, y, w, h);
        int capacity = menu.tankCapacity(index), amount = menu.tankAmount(index);
        int filled = capacity > 0 ? (int) Math.clamp((long) amount * h / capacity, amount > 0 ? 1 : 0, h) : 0;
        if (filled > 0) g.fill(x, y + h - filled, x + w, y + h, fluidColor(menu.tankFluid(index)));
        // scale marks on the left edge; output tanks get an amber cap so the direction is readable
        for (int mark = 1; mark < 4; mark++) g.fill(x, y + h * mark / 4, x + (mark == 2 ? 6 : 3), y + h * mark / 4 + 1, SCREEN_EDGE);
        if (index >= menu.def().fluidsIn().size()) g.fill(x, y - 1, x + w, y, 0xFFB07A20);
    }

    private void arrow(GuiGraphicsExtractor g) {
        int x = leftPos + layout.arrow.x(), y = topPos + layout.arrow.y();
        boolean blocked = !menu.reason().isEmpty();
        int base = blocked ? 0xFFA05050 : SLOT;
        int done = menu.maxProgress() > 0 ? Math.clamp(menu.progress() * ProcessLayout.ARROW_W / menu.maxProgress(), 0, ProcessLayout.ARROW_W) : 0;
        // shaft 15 wide, head 7 wide narrowing to a point
        for (int col = 0; col < ProcessLayout.ARROW_W; col++) {
            int half = col < 15 ? 2 : 7 - (col - 15);
            int color = col < done ? GREEN : base;
            g.fill(x + col, y + 8 - half, x + col + 1, y + 8 + half, color);
        }
    }

    private void status(GuiGraphicsExtractor g) {
        int right = imageWidth - (menu.def().upgrades() > 0 ? 28 : 8);
        int x = leftPos + 8, y = topPos + ProcessLayout.STATUS_Y, w = right - 8;
        inset(g, x, y, w, 10);
        Component text;
        int color;
        String reason = menu.reason();
        if (!reason.isEmpty()) {
            text = reason.equals("missing_utility") && menu.shortUtility() != null
                ? Component.translatable("gui.iuce.utility.shortage",
                    Component.translatable(menu.shortUtility().nameKey()), menu.needShares(), menu.haveShares())
                : Component.translatable("tooltip.iuce.status." + reason);
            color = reason.equals("no_recipe") ? AMBER : RED;
        } else if (menu.maxProgress() > 0 && menu.progress() > 0) {
            text = Component.translatable("gui.iuce.process.running", menu.progress() * 100 / menu.maxProgress());
            color = GREEN;
        } else {
            text = Component.translatable("gui.iuce.process.idle");
            color = DIM;
        }
        String line = font.plainSubstrByWidth(text.getString(), w - 4);
        g.text(font, line, x + 2, y + 1, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos, my = mouseY - topPos;
        if (inside(mx, my, layout.energyX, ProcessLayout.TANK_Y, ProcessLayout.ENERGY_W, ProcessLayout.TANK_H)) {
            g.setTooltipForNextFrame(font, Component.translatable("gui.iuce.process.energy", menu.energy(), menu.maxEnergy()), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < layout.tanks.size(); i++) {
            var pos = layout.tanks.get(i);
            if (!inside(mx, my, pos.x(), pos.y(), ProcessLayout.TANK_W, ProcessLayout.TANK_H)) continue;
            List<Component> lines = new ArrayList<>();
            Fluid fluid = menu.tankFluid(i);
            boolean input = i < menu.def().fluidsIn().size();
            lines.add(fluid == Fluids.EMPTY
                ? Component.translatable(input ? "gui.iuce.process.input_tank" : "gui.iuce.process.output_tank")
                : fluid.getFluidType().getDescription());
            lines.add(Component.literal(menu.tankAmount(i) + " / " + menu.tankCapacity(i) + " mB"));
            g.setTooltipForNextFrame(font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
            return;
        }
        if (inside(mx, my, layout.arrow.x(), layout.arrow.y(), ProcessLayout.ARROW_W, ProcessLayout.ARROW_H) && menu.maxProgress() > 0) {
            g.setTooltipForNextFrame(font, Component.translatable("gui.iuce.process.progress", menu.progress(), menu.maxProgress()), mouseX, mouseY);
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Water and lava get their usual colours, our chemicals their registered colour, the rest a stable colour from its id. */
    static int fluidColor(Fluid fluid) {
        if (fluid.isSame(Fluids.WATER)) return 0xFF3F76E4;
        if (fluid.isSame(Fluids.LAVA)) return 0xFFE0661A;
        int chemical = init.ModFluids.chemicalColor(fluid);
        if (chemical != 0) return chemical;
        String id = BuiltInRegistries.FLUID.getKey(fluid).toString().toLowerCase(Locale.ROOT);
        float hue = (id.hashCode() & 0xFFFF) / 65535f;
        return 0xFF000000 | (net.minecraft.util.Mth.hsvToRgb(hue, 0.55f, 0.85f) & 0xFFFFFF);
    }
}
