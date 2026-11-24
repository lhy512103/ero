package client.gui;

import item.OverclockerUpgradeItem;
import machine.process.ProcessLayout;
import machine.process.ProcessMachineBlockEntity;
import machine.process.ProcessMachineMenu;
import machine.process.ProcessMachineMenu.Page;
import machine.process.ProcessSchematic;
import machine.process.ProgressStyle;
import machine.utility.Utility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import recipe.ModRecipes;
import recipe.ProcessRecipe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The screen of every process machine, in the light grey of the vanilla containers, with tabs down
 * its left side. A machine with a {@link ProcessSchematic} shows a picture of itself on the process
 * page, the way the brewing stand does, with its slots where the work goes in and out and the
 * progress drawn inside the machine; the others lay their slots, tanks and progress picture out in
 * the order of the work. The other pages pick a mould or die, hold the upgrades, preview the
 * machine's recipes, show its power and utility use, and explain what it does. Positions come from
 * {@link ProcessLayout}.
 */
public class ProcessMachineScreen extends AbstractContainerScreen<ProcessMachineMenu> {
    private static final Identifier GHOST_CATALYST = texture("ghost_catalyst"), GHOST_CARD = texture("ghost_card"), GHOST_UPGRADE = texture("ghost_upgrade");

    // vanilla container colours
    private static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, EDGE = 0xFF000000;
    private static final int SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737;
    private static final int TAB_OFF = 0xFFA9A9A9, TAB_OFF_LIGHT = 0xFFDADADA, TAB_OFF_SHADE = 0xFF6A6A6A;
    // small dark displays: gauges, tanks and the status line
    private static final int SCREEN = 0xFF1E2328, SCREEN_EDGE = 0xFF0E1114;
    private static final int GREEN = 0xFF7DFF9A, AMBER = 0xFFFFD080, RED = 0xFFFF9A8A, DIM = 0xFF9FD0B0;
    private static final int ENERGY = 0xFFE8C440, ENERGY_TOP = 0xFFFFF0A0;
    // light text pages
    private static final int PAPER = 0xFFE4E4E4, TEXT = 0xFF404040, TEXT_DIM = 0xFF7A7A7A, HEAD = 0xFF1F4E8C, WARN = 0xFFB02020, CHANCE = 0xFF9A6A00;

    /** Top and bottom of the area the pages other than the process page draw in. */
    private static final int PAGE_TOP = ProcessLayout.PAGE_TOP, PAGE_BOTTOM = ProcessLayout.INVENTORY_LABEL_Y - 2;
    private static final int TAB_W = 22, TAB_H = 22, TAB_STEP = 24, TAB_Y = 6;
    private static final int RECIPE_ROW = 22;

    private final ProcessLayout layout;
    private final List<Page> pages = new ArrayList<>();
    private final List<String> molds;
    private List<RecipeHolder<ProcessRecipe>> recipes = List.of();
    private int recipeScroll, textScroll;

    /** What the mouse can point at this frame; filled while drawing, read by the tooltip pass. */
    private record Hit(int x, int y, int w, int h, ItemStack stack, List<Component> lines) {}
    private final List<Hit> hits = new ArrayList<>();

    public ProcessMachineScreen(ProcessMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.layout().width, ProcessLayout.HEIGHT);
        this.layout = menu.layout();
        this.inventoryLabelX = layout.inventoryX;
        this.inventoryLabelY = ProcessLayout.INVENTORY_LABEL_Y;
        this.molds = ProcessMachineBlockEntity.moldItems(menu.def().id());
        pages.add(Page.PROCESS);
        if (!molds.isEmpty()) pages.add(Page.MOLD);
        if (menu.def().upgrades() > 0) pages.add(Page.UPGRADES);
        pages.add(Page.RECIPES);
        pages.add(Page.ENERGY);
        if (client.gui.component.MachineInfoPanel.hasInfo(menu.def().id())) pages.add(Page.INFO);
    }

    private static Identifier texture(String name) { return Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/" + name + ".png"); }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        var map = client.guide.Guide.recipes();
        if (map != null) {
            List<RecipeHolder<ProcessRecipe>> list = new ArrayList<>();
            for (RecipeHolder<ProcessRecipe> holder : map.byType(ModRecipes.PROCESS_TYPE.get())) {
                if (holder.value().machine().equals(menu.def().id())) list.add(holder);
            }
            recipes = list;
        }
    }

    public Page page() { return menu.page(); }

    /** The tabs this machine has, in the order they are drawn. */
    public List<Page> pages() { return List.copyOf(pages); }

    /** Shows {@code page} if this machine has it, as clicking its tab would. */
    public void selectPage(Page page) {
        if (!pages.contains(page) || page == page()) return;
        menu.setPage(page);
        recipeScroll = 0;
        textScroll = 0;
    }

    /** The side tabs, for JEI so it keeps its item list clear of them. */
    public List<Rect2i> tabAreas() {
        return List.of(new Rect2i(leftPos - TAB_W + 1, topPos + TAB_Y, TAB_W, pages.size() * TAB_STEP));
    }

    // ---- drawing ----

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        for (int i = 0; i < pages.size(); i++) if (pages.get(i) != page()) tab(g, i, false);
        panel(g);
        tab(g, pages.indexOf(page()), true);
        if (page() == Page.PROCESS && layout.schematic != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, schematicTexture(""), leftPos, topPos + ProcessSchematic.TOP, 0, 0,
                ProcessSchematic.WIDTH, ProcessSchematic.HEIGHT, ProcessSchematic.WIDTH, ProcessSchematic.HEIGHT);
        }
        for (Slot slot : menu.slots) if (slot.isActive()) slotFrame(g, slot);
        switch (page()) {
            case PROCESS -> processPage(g);
            case MOLD -> moldPage(g);
            case UPGRADES -> upgradePage(g);
            case RECIPES -> recipePage(g);
            case ENERGY -> textPage(g, energyLines());
            case INFO -> textPage(g, infoLines());
        }
        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private Identifier schematicTexture(String suffix) {
        return Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/schematic/" + layout.schematic.id() + suffix + ".png");
    }

    private void panel(GuiGraphicsExtractor g) {
        raised(g, leftPos, topPos, imageWidth, imageHeight, PANEL, LIGHT, SHADE);
    }

    /** A raised block in the vanilla bevel: black rounded edge, light top and left, shade bottom and right. */
    private static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int face, int light, int shade) {
        g.fill(x + 1, y, x + w - 1, y + h, EDGE);
        g.fill(x, y + 1, x + w, y + h - 1, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, light);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, light);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, shade);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, shade);
    }

    private void tab(GuiGraphicsExtractor g, int index, boolean active) {
        Page page = pages.get(index);
        int x = leftPos - TAB_W + 1, y = topPos + TAB_Y + index * TAB_STEP;
        if (active) {
            // joins the panel: no edge on the right, and it covers the panel's own left edge and bevel
            g.fill(x + 1, y, leftPos + 3, y + TAB_H, EDGE);
            g.fill(x, y + 1, leftPos + 3, y + TAB_H - 1, EDGE);
            g.fill(x + 1, y + 1, leftPos + 3, y + TAB_H - 1, PANEL);
            g.fill(x + 1, y + 1, leftPos + 1, y + 3, LIGHT);
            g.fill(x + 1, y + 1, x + 3, y + TAB_H - 2, LIGHT);
            g.fill(x + 3, y + TAB_H - 3, leftPos + 1, y + TAB_H - 1, SHADE);
        } else {
            raised(g, x, y, TAB_W, TAB_H, TAB_OFF, TAB_OFF_LIGHT, TAB_OFF_SHADE);
        }
        String icon = switch (page) {
            case PROCESS -> "process";
            case MOLD -> "mold";
            case UPGRADES -> "upgrades";
            case RECIPES -> "recipes";
            case ENERGY -> "energy";
            case INFO -> "info";
        };
        g.blit(RenderPipelines.GUI_TEXTURED, texture("icon_" + icon), x + 4, y + 3, 0, 0, 16, 16, 16, 16, active ? -1 : 0xFFC8C8C8);
        hits.add(new Hit(x, y, TAB_W, TAB_H, ItemStack.EMPTY, List.of(Component.translatable("gui.iuce.process.tab." + icon))));
    }

    private void slotFrame(GuiGraphicsExtractor g, Slot slot) {
        int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
        g.fill(x, y, x + 18, y + 18, SLOT);
        g.fill(x, y, x + 17, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 17, x + 18, y + 18, LIGHT);
        g.fill(x + 17, y + 1, x + 18, y + 18, LIGHT);
        if (slot.hasItem() || slot.index >= menu.slots.size() - 36) return;
        Identifier ghost = null;
        int index = slot.getContainerSlot();
        if (index == menu.def().catalystIndex()) ghost = GHOST_CATALYST;
        else if (index == menu.def().cardIndex()) ghost = GHOST_CARD;
        else if (index >= menu.def().upgradeStart()) ghost = GHOST_UPGRADE;
        if (ghost != null) g.blit(RenderPipelines.GUI_TEXTURED, ghost, x + 1, y + 1, 0, 0, 16, 16, 16, 16, 0x90FFFFFF);
    }

    /** A recessed rectangle with the panel's bevel around it. */
    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_DARK);
        g.fill(x, y + h, x + w + 1, y + h + 1, LIGHT);
        g.fill(x + w, y, x + w + 1, y + h + 1, LIGHT);
        g.fill(x, y, x + w, y + h, fill);
    }

    // ---- process page

    private void processPage(GuiGraphicsExtractor g) {
        energy(g);
        for (int i = 0; i < layout.tanks.size(); i++) tank(g, i);
        if (layout.schematic != null) schematicProgress(g);
        else progress(g);
        status(g);
    }

    private void energy(GuiGraphicsExtractor g) {
        int x = leftPos + layout.energyX, y = topPos + ProcessLayout.TANK_Y, w = ProcessLayout.ENERGY_W, h = ProcessLayout.TANK_H;
        inset(g, x, y, w, h, SCREEN);
        long max = menu.maxEnergy();
        int filled = max > 0 ? (int) Math.clamp(menu.energy() * h / max, 0, h) : 0;
        if (filled > 0) {
            g.fill(x, y + h - filled, x + w, y + h, ENERGY);
            g.fill(x, y + h - filled, x + w, y + h - filled + 1, ENERGY_TOP);
        }
        hits.add(new Hit(x, y, w, h, ItemStack.EMPTY,
            List.of(Component.translatable("gui.iuce.process.energy", menu.energy(), menu.maxEnergy()))));
    }

    private void tank(GuiGraphicsExtractor g, int index) {
        var pos = layout.tanks.get(index);
        int x = leftPos + pos.x(), y = topPos + pos.y(), w = ProcessLayout.TANK_W, h = ProcessLayout.TANK_H;
        boolean input = index < menu.def().fluidsIn().size();
        inset(g, x, y, w, h, SCREEN);
        int capacity = menu.tankCapacity(index), amount = menu.tankAmount(index);
        int filled = capacity > 0 ? (int) Math.clamp((long) amount * h / capacity, amount > 0 ? 1 : 0, h) : 0;
        Fluid fluid = menu.tankFluid(index);
        if (filled > 0) {
            int color = fluidColor(fluid);
            g.fill(x, y + h - filled, x + w, y + h, color);
            g.fill(x, y + h - filled, x + w, y + h - filled + 1, lighter(color));
        }
        // scale marks on the left edge; output tanks get an amber cap so the direction is readable
        for (int mark = 1; mark < 4; mark++) g.fill(x, y + h * mark / 4, x + (mark == 2 ? 6 : 3), y + h * mark / 4 + 1, SCREEN_EDGE);
        if (!input) g.fill(x, y - 1, x + w, y, 0xFFB07A20);
        List<Component> lines = new ArrayList<>();
        lines.add(fluid == Fluids.EMPTY
            ? Component.translatable(input ? "gui.iuce.process.input_tank" : "gui.iuce.process.output_tank")
            : fluid.getFluidType().getDescription());
        lines.add(Component.literal(amount + " / " + capacity + " mB"));
        hits.add(new Hit(x, y, w, h, ItemStack.EMPTY, lines));
    }

    private float part() {
        int max = menu.maxProgress();
        return max > 0 ? Math.clamp((float) menu.progress() / max, 0f, 1f) : 0f;
    }

    private boolean running() { return menu.progress() > 0 && menu.reason().isEmpty(); }

    private void progressHit(int x, int y, int w, int h) {
        if (menu.maxProgress() > 0) hits.add(new Hit(x, y, w, h, ItemStack.EMPTY,
            List.of(Component.translatable("gui.iuce.process.progress", menu.progress(), menu.maxProgress()))));
    }

    /** The work inside the machine's picture: the filling row, then the looping overlay on top. */
    private void schematicProgress(GuiGraphicsExtractor g) {
        ProcessSchematic s = layout.schematic;
        Identifier sheet = schematicTexture("_progress");
        int x = leftPos + layout.arrow.x(), y = topPos + layout.arrow.y(), w = s.windowW(), h = s.windowH();
        int texH = h * s.rows();
        int tint = menu.reason().isEmpty() ? -1 : 0xFFE09090;
        switch (s.mode()) {
            case PHASE -> {
                int row = Math.min(s.frames() - 1, (int) (part() * s.frames()));
                g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, row * h, w, h, w, texH, tint);
            }
            case RIGHT -> {
                int filled = Math.round(part() * w);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, 0, filled, h, w, texH);
            }
            case UP -> {
                int filled = Math.round(part() * h);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y + h - filled, 0, h - filled, w, filled, w, texH);
            }
        }
        if (s.mode() != ProgressStyle.Mode.PHASE && s.frames() > 0 && (running() || s.idleOverlay())) {
            int frame = running() ? (int) (System.currentTimeMillis() / 120 % s.frames()) : 0;
            g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, (1 + frame) * h, w, h, w, texH, tint);
        }
        progressHit(x, y, w, h);
    }

    /** Draws the machine's progress picture; see {@link ProgressStyle} for the sheet layout. */
    private void progress(GuiGraphicsExtractor g) {
        ProgressStyle style = menu.def().progress();
        Identifier sheet = Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/progress/" + style.id() + ".png");
        int x = leftPos + layout.arrow.x(), y = topPos + layout.arrow.y(), w = style.width(), h = style.height();
        int texH = h * style.rows();
        float part = part();
        boolean running = running();
        int tint = menu.reason().isEmpty() ? -1 : 0xFFE0A0A0;
        int frame = (int) (System.currentTimeMillis() / 120 % Math.max(1, style.frames()));
        switch (style.mode()) {
            case PHASE -> {
                int row = Math.min(style.frames() - 1, (int) (part * style.frames()));
                g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, row * h, w, h, w, texH, tint);
            }
            case RIGHT -> {
                g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, 0, w, h, w, texH, tint);
                int filled = Math.round(part * w);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, h, filled, h, w, texH);
                if (running && style.frames() > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, (2 + frame) * h, w, h, w, texH);
            }
            case UP -> {
                g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, 0, w, h, w, texH, tint);
                int filled = Math.round(part * h);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y + h - filled, 0, 2 * h - filled, w, filled, w, texH);
                if (running && style.frames() > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, (2 + frame) * h, w, h, w, texH);
            }
        }
        progressHit(x, y, w, h);
    }

    private void status(GuiGraphicsExtractor g) {
        // the bevel ends one row above the inventory label at INVENTORY_LABEL_Y
        int x = leftPos + 8, y = topPos + ProcessLayout.INVENTORY_LABEL_Y - 12, w = imageWidth - 16, h = 10;
        inset(g, x, y, w, h, SCREEN);
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
        g.text(font, font.plainSubstrByWidth(text.getString(), w - 4), x + 2, y + 1, color, false);
        hits.add(new Hit(x, y, w, h, ItemStack.EMPTY, List.of(text)));
    }

    // ---- mould / die page

    private int moldColumns() { return Math.max(1, (imageWidth - 16) / 22); }

    private void moldPage(GuiGraphicsExtractor g) {
        int cols = moldColumns();
        int x0 = leftPos + (imageWidth - cols * 22 + 2) / 2;
        for (int i = 0; i < molds.size(); i++) {
            int x = x0 + (i % cols) * 22, y = topPos + PAGE_TOP + 2 + (i / cols) * 22;
            if (i == menu.mold()) inset(g, x + 1, y + 1, 18, 18, SLOT);
            else raised(g, x, y, 20, 20, PANEL, LIGHT, SHADE);
            ItemStack stack = moldStack(i);
            g.item(stack, x + 2, y + 2);
            hits.add(new Hit(x, y, 20, 20, stack, null));
        }
        ItemStack current = moldStack(menu.mold());
        Component line = Component.translatable("gui.iuce.process.mold.selected", current.getHoverName());
        g.text(font, font.plainSubstrByWidth(line.getString(), imageWidth - 16), leftPos + 8, topPos + PAGE_BOTTOM - 10, TEXT, false);
    }

    private ItemStack moldStack(int index) {
        if (index < 0 || index >= molds.size()) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("iuce", molds.get(index))));
    }

    // ---- upgrade page

    private void upgradePage(GuiGraphicsExtractor g) {
        g.text(font, Component.translatable("gui.iuce.process.upgrades.hint"), leftPos + 8, topPos + PAGE_TOP + 2, TEXT, false);
        OverclockerUpgradeItem.Effect effect = OverclockerUpgradeItem.effect(menu.blockEntity());
        int y = topPos + ProcessLayout.UPGRADE_Y + 24;
        if (effect == OverclockerUpgradeItem.Effect.NONE) {
            g.text(font, Component.translatable("gui.iuce.process.upgrades.none"), leftPos + 8, y, TEXT, false);
        } else {
            g.text(font, Component.translatable("gui.iuce.process.upgrades.speed", factor(1 / effect.time())), leftPos + 8, y, TEXT, false);
            g.text(font, Component.translatable("gui.iuce.process.upgrades.power", factor(effect.energy())), leftPos + 8, y + 10, TEXT, false);
        }
    }

    private static String factor(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    // ---- recipe preview page

    private int visibleRecipes() { return (PAGE_BOTTOM - PAGE_TOP - 4) / RECIPE_ROW; }

    private void recipePage(GuiGraphicsExtractor g) {
        int x = leftPos + 8, y = topPos + PAGE_TOP, w = imageWidth - 16, h = PAGE_BOTTOM - PAGE_TOP;
        inset(g, x, y, w, h, PAPER);
        if (recipes.isEmpty()) {
            g.text(font, Component.translatable("gui.iuce.process.recipes.empty"), x + 4, y + 4, TEXT_DIM, false);
            return;
        }
        recipeScroll = Math.clamp(recipeScroll, 0, Math.max(0, recipes.size() - visibleRecipes()));
        long tick = System.currentTimeMillis() / 1000;
        for (int row = 0; row < visibleRecipes() && recipeScroll + row < recipes.size(); row++) {
            ProcessRecipe recipe = recipes.get(recipeScroll + row).value();
            int ry = y + 3 + row * RECIPE_ROW, cx = x + 3;
            if (row > 0) g.fill(x + 2, ry - 3, x + w - 2, ry - 2, 0xFFC8C8C8);
            int room = (w - 6 - 14) / 18;
            int used = 0;
            for (ProcessRecipe.ItemInput input : recipe.items()) {
                if (used++ >= room - 2) break;
                List<ItemStack> options = input.ingredient().items().map(holder -> new ItemStack(holder, input.count())).toList();
                if (!options.isEmpty()) itemIcon(g, options.get((int) (tick % options.size())), cx, ry);
                cx += 18;
            }
            for (ProcessRecipe.FluidInput input : recipe.fluids()) {
                if (used++ >= room - 2) break;
                var fluids = input.ingredient().fluids();
                if (!fluids.isEmpty()) fluidIcon(g, fluids.get((int) (tick % fluids.size())).value(), input.amount(), cx, ry);
                cx += 18;
            }
            g.text(font, "→", cx + 2, ry + 5, TEXT, false);
            cx += 14;
            for (ProcessRecipe.Output output : recipe.results()) {
                if (used++ >= room) break;
                itemIcon(g, output.create(), cx, ry);
                if (output.chance() < 1f) g.text(font, Math.round(output.chance() * 100) + "%", cx, ry - 1, CHANCE, false);
                cx += 18;
            }
            for (ProcessRecipe.FluidOutput output : recipe.fluidResults()) {
                if (used++ >= room) break;
                fluidIcon(g, output.fluid(), output.amount(), cx, ry);
                cx += 18;
            }
            hits.add(new Hit(x + 1, ry - 1, w - 2, RECIPE_ROW - 2, ItemStack.EMPTY, recipeLines(recipe)));
        }
        if (recipes.size() > visibleRecipes()) {
            String page = (recipeScroll + 1) + "-" + Math.min(recipes.size(), recipeScroll + visibleRecipes()) + " / " + recipes.size();
            g.text(font, page, x + w - 3 - font.width(page), y + h - 10, TEXT_DIM, false);
        }
    }

    private List<Component> recipeLines(ProcessRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.iuce.process.recipes.time", String.format(Locale.ROOT, "%.1f", recipe.time() / 20f), recipe.energy()));
        if (recipe.energyPerTick() > 0) lines.add(Component.translatable("gui.iuce.process.recipes.draw", recipe.energyPerTick()));
        if (!recipe.utilities().isEmpty()) lines.add(Component.translatable("gui.iuce.process.recipes.utilities", utilityNames(recipe.utilities())));
        return lines;
    }

    private void itemIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.item(stack, x, y);
        g.itemDecorations(font, stack, x, y);
        hits.add(0, new Hit(x, y, 16, 16, stack, null));
    }

    private void fluidIcon(GuiGraphicsExtractor g, Fluid fluid, int amount, int x, int y) {
        int color = fluidColor(fluid);
        g.fill(x + 2, y + 2, x + 14, y + 15, SLOT_DARK);
        g.fill(x + 3, y + 5, x + 13, y + 14, color);
        g.fill(x + 3, y + 5, x + 13, y + 6, lighter(color));
        String text = amount >= 1000 ? (amount / 1000) + "B" : String.valueOf(amount);
        g.text(font, text, x + 17 - font.width(text), y + 9, -1, true);
        hits.add(0, new Hit(x, y, 16, 16, ItemStack.EMPTY,
            List.of(fluid.getFluidType().getDescription(), Component.literal(amount + " mB"))));
    }

    // ---- text pages: power and utilities, machine info

    private List<Component> energyLines() {
        var def = menu.def();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.iuce.process.power.title").withColor(HEAD));
        lines.add(Component.translatable("gui.iuce.process.power.voltage", def.voltage().getName(), def.voltage().getMaxVoltage()));
        lines.add(Component.translatable("gui.iuce.process.power.stored", menu.energy(), menu.maxEnergy()));
        lines.add(menu.energyPerTick() > 0
            ? Component.translatable("gui.iuce.process.power.draw", menu.energyPerTick())
            : Component.translatable("gui.iuce.process.power.idle"));
        long lowest = Long.MAX_VALUE, highest = 0;
        Set<String> utilities = new LinkedHashSet<>();
        for (RecipeHolder<ProcessRecipe> holder : recipes) {
            long draw = holder.value().energyPerTick();
            lowest = Math.min(lowest, draw);
            highest = Math.max(highest, draw);
            utilities.addAll(holder.value().utilities());
        }
        if (highest > 0) lines.add(Component.translatable("gui.iuce.process.power.range", lowest, highest));
        lines.add(Component.translatable("gui.iuce.process.utility.title").withColor(HEAD));
        lines.add(utilities.isEmpty()
            ? Component.translatable("gui.iuce.process.utility.none")
            : Component.translatable("gui.iuce.process.utility.uses", utilityNames(utilities)));
        Utility shortUtility = menu.shortUtility();
        if (shortUtility != null) lines.add(Component.translatable("gui.iuce.utility.shortage",
            Component.translatable(shortUtility.nameKey()), menu.needShares(), menu.haveShares()).withColor(WARN));
        return lines;
    }

    private static Component utilityNames(Iterable<String> ids) {
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            Utility utility = Utility.of(id);
            names.add(utility == null ? id : I18n.get(utility.nameKey()));
        }
        return Component.literal(String.join(I18n.get("gui.iuce.process.list_separator"), names));
    }

    private List<Component> infoLines() {
        String id = menu.def().id();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("info.iuce." + id + ".title").withColor(HEAD));
        for (int n = 1; I18n.exists("info.iuce." + id + "." + n); n++) lines.add(Component.translatable("info.iuce." + id + "." + n));
        return lines;
    }

    /** Wrapped dark text on a light recessed page; scrolls with the mouse wheel when it does not fit. */
    private void textPage(GuiGraphicsExtractor g, List<Component> lines) {
        int x = leftPos + 8, y = topPos + PAGE_TOP, w = imageWidth - 16, h = PAGE_BOTTOM - PAGE_TOP;
        inset(g, x, y, w, h, PAPER);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(font.split(line, w - 10));
        int fits = (h - 6) / 10;
        textScroll = Math.clamp(textScroll, 0, Math.max(0, wrapped.size() - fits));
        for (int i = 0; i < fits && textScroll + i < wrapped.size(); i++) {
            g.text(font, wrapped.get(textScroll + i), x + 4, y + 4 + i * 10, TEXT, false);
        }
        if (wrapped.size() > fits) {
            int track = h - 4, knob = Math.max(6, track * fits / wrapped.size());
            int top = y + 2 + (track - knob) * textScroll / Math.max(1, wrapped.size() - fits);
            g.fill(x + w - 4, top, x + w - 2, top + knob, SLOT);
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            for (int i = 0; i < pages.size(); i++) {
                int x = leftPos - TAB_W + 1, y = topPos + TAB_Y + i * TAB_STEP;
                if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) {
                    if (pages.get(i) != page()) {
                        selectPage(pages.get(i));
                        click();
                    }
                    return true;
                }
            }
            if (page() == Page.MOLD) {
                int cols = moldColumns(), x0 = leftPos + (imageWidth - cols * 22 + 2) / 2;
                for (int i = 0; i < molds.size(); i++) {
                    int x = x0 + (i % cols) * 22, y = topPos + PAGE_TOP + 2 + (i / cols) * 22;
                    if (mx >= x && mx < x + 20 && my >= y && my < y + 20) {
                        if (i != menu.mold()) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                            click();
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0 && (page() == Page.RECIPES || page() == Page.ENERGY || page() == Page.INFO)) {
            int step = scrollY > 0 ? -1 : 1;
            if (page() == Page.RECIPES) recipeScroll += step;
            else textScroll += step;
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        for (Rect2i area : tabAreas()) if (area.contains((int) mx, (int) my)) return false;
        return super.hasClickedOutside(mx, my, xo, yo);
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem()) return;
        for (Hit hit : hits) {
            if (mouseX < hit.x() || mouseX >= hit.x() + hit.w() || mouseY < hit.y() || mouseY >= hit.y() + hit.h()) continue;
            if (!hit.stack().isEmpty()) g.setTooltipForNextFrame(font, hit.stack(), mouseX, mouseY);
            else if (hit.lines() != null) g.setComponentTooltipForNextFrame(font, hit.lines(), mouseX, mouseY);
            return;
        }
    }

    private static int lighter(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 40), gr = Math.min(255, ((color >> 8) & 0xFF) + 40), b = Math.min(255, (color & 0xFF) + 40);
        return 0xFF000000 | r << 16 | gr << 8 | b;
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
