package client.recipe;

import client.gui.MachineScreen;
import client.recipe.RecipePage.Bounds;
import client.recipe.RecipePage.Frame;
import client.recipe.RecipePage.Mark;
import client.recipe.RecipePage.Role;
import machine.process.ProcessLayout;
import machine.process.ProcessLayout.Box;
import machine.process.ProcessLayout.Pos;
import machine.process.ProcessMachineDef;
import machine.process.ProcessSchematic;
import machine.process.ProgressStyle;
import machine.process.ProgressStyle.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import recipe.ProcessRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ProcessRecipePage {
    private static final int LINE = 10;

    /** The content's frame (in panel coordinates) and the text written under it. */
    private record Size(Frame content, int lines, int textWidth) {
        Frame page() {
            int w = Math.max(Math.max(content.w(), textWidth + 2 * RecipePage.PAD), RecipePage.MIN_WIDTH);
            return new Frame(content.ox() - (w - content.w()) / 2, content.oy(), w, content.h() + lines * LINE);
        }
    }

    private final ProcessMachineDef def;
    private final ProcessLayout layout;
    private final Frame frame;
    private final int textY;

    public ProcessRecipePage(ProcessMachineDef def, Collection<RecipeHolder<ProcessRecipe>> recipes) {
        this.def = def;
        this.layout = new ProcessLayout(def);
        Size size = size(layout, recipes);
        this.frame = size.page();
        this.textY = size.content().h() - RecipePage.PAD + 2;
    }
    public int width() { return frame.w(); }
    public int height() { return frame.h(); }
    public ProcessLayout layout() { return layout; }
    public int dx() { return -frame.ox(); }
    public int dy() { return -frame.oy(); }

    private static Size size(ProcessLayout layout, Collection<RecipeHolder<ProcessRecipe>> recipes) {
        Bounds bounds = new Bounds();
        ProcessSchematic s = layout.schematic;
        if (s != null) bounds.picture(schematic(s, ""), 0, ProcessSchematic.TOP, ProcessSchematic.WIDTH, ProcessSchematic.HEIGHT);
        for (Pos p : layout.inputs) bounds.slot(p.x(), p.y());
        for (Pos p : layout.outputs) bounds.slot(p.x(), p.y());
        if (layout.catalyst != null) bounds.slot(layout.catalyst.x(), layout.catalyst.y());
        if (layout.card != null) bounds.slot(layout.card.x(), layout.card.y());
        for (Box box : layout.tanks) bounds.tank(box.x(), box.y(), box.w(), box.h());
        bounds.gauge(layout.energyX, ProcessLayout.TANK_Y, ProcessLayout.ENERGY_W, ProcessLayout.TANK_H);
        bounds.add(layout.arrow.x(), layout.arrow.y(), layout.progressW, layout.progressH);
        int lines = 0, width = 0;
        var font = Minecraft.getInstance().font;
        for (RecipeHolder<ProcessRecipe> holder : recipes) {
            List<Component> text = lines(holder.value());
            lines = Math.max(lines, text.size());
            for (Component line : text) width = Math.max(width, font.width(line));
        }
        return new Size(bounds.frame(), lines, width);
    }

    /** What the page writes under the machine: only what has no slot, tank or tooltip of its own. */
    private static List<Component> lines(ProcessRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        recipe.card().ifPresent(card -> lines.add(Component.translatable("jei.iuce.process.card",
            machine.process.ProcessCards.label(card))));
        if (!recipe.utilities().isEmpty()) lines.add(Component.translatable("jei.iuce.process.utilities", names(recipe.utilities(), "utility")));
        if (!recipe.segments().isEmpty()) lines.add(Component.translatable("jei.iuce.process.segments", names(recipe.segments(), "segment")));
        if (releases(recipe)) lines.add(Component.translatable("jei.iuce.process.released_line"));
        return lines;
    }

    /** True when the recipe consumes its input and produces nothing: a vent or a flare. */
    private static boolean releases(ProcessRecipe recipe) {
        return recipe.results().isEmpty() && recipe.fluidResults().isEmpty();
    }

    public void draw(RecipeHolder<ProcessRecipe> holder, GuiGraphicsExtractor graphics) {
        ProcessRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        long period = Math.clamp(recipe.time() * 50L, 1500L, 6000L), now = System.currentTimeMillis();
        float part = (now % period) / (float) period;
        RecipePage.panel(graphics, width(), height());
        ProcessSchematic s = layout.schematic;
        if (s != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, schematic(s, ""), dx, ProcessSchematic.TOP + dy, 0, 0,
                ProcessSchematic.WIDTH, ProcessSchematic.HEIGHT, ProcessSchematic.WIDTH, ProcessSchematic.HEIGHT);
        }
        // the machine's slots: the ones this recipe uses are marked by role, the others stay empty
        List<Mark> marks = new ArrayList<>();
        for (int i = 0; i < layout.inputs.size(); i++) {
            Pos p = layout.inputs.get(i);
            if (i < recipe.items().size()) marks.add(new Mark(p.x() + dx, p.y() + dy, Role.INPUT));
            else MachineScreen.slotFrame(graphics, p.x() - 1 + dx, p.y() - 1 + dy);
        }
        for (int i = 0; i < layout.outputs.size(); i++) {
            Pos p = layout.outputs.get(i);
            if (i < recipe.results().size()) marks.add(new Mark(p.x() + dx, p.y() + dy, Role.OUTPUT));
            else MachineScreen.slotFrame(graphics, p.x() - 1 + dx, p.y() - 1 + dy);
        }
        if (layout.catalyst != null) {
            if (recipe.catalyst().isPresent()) marks.add(new Mark(layout.catalyst.x() + dx, layout.catalyst.y() + dy, Role.STATION));
            else MachineScreen.slotFrame(graphics, layout.catalyst.x() - 1 + dx, layout.catalyst.y() - 1 + dy);
        }
        if (layout.card != null) MachineScreen.slotFrame(graphics, layout.card.x() - 1 + dx, layout.card.y() - 1 + dy);
        RecipePage.slots(graphics, marks);

        int fluidsIn = def.fluidsIn().size();
        for (int i = 0; i < layout.tanks.size(); i++) {
            Box box = layout.tanks.get(i);
            RecipePage.tank(graphics, box.x() + dx, box.y() + dy, box.w(), box.h(), i < fluidsIn);
            boolean used = i < fluidsIn ? i < recipe.fluids().size() : i - fluidsIn < recipe.fluidResults().size();
            if (!used) MachineScreen.tankMarks(graphics, box.x() + dx, box.y() + dy, box.w(), box.h());
        }
        MachineScreen.energyGauge(graphics, layout.energyX + dx, ProcessLayout.TANK_Y + dy, ProcessLayout.ENERGY_W, ProcessLayout.TANK_H,
            1f - part * 0.75f);

        int x = layout.arrow.x() + dx, y = layout.arrow.y() + dy;
        int frame = (int) (now / 120 % 1000);
        if (s != null) {
            window(graphics, schematic(s, "_progress"), x, y, s.windowW(), s.windowH(), s.mode(), s.frames(), part, frame, false);
        } else if (releases(recipe)) {
            int flameX = x + (layout.progressW - 14) / 2;
            int filled = Math.max(1, Math.round((1f - part) * 14));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("container/furnace/lit_progress"),
                14, 14, 0, 14 - filled, flameX, y + 15 - filled, 14, filled);
        } else {
            ProgressStyle style = def.progress();
            Identifier sheet = Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/progress/" + style.id() + ".png");
            window(graphics, sheet, x, y, style.width(), style.height(), style.mode(), style.frames(), part, frame, true);
        }

        var font = Minecraft.getInstance().font;
        List<Component> lines = lines(recipe);
        for (int i = 0; i < lines.size(); i++) graphics.text(font, lines.get(i), RecipePage.PAD, textY + i * LINE, MachineScreen.LABEL, false);
    }

    private static Identifier schematic(ProcessSchematic s, String suffix) {
        return Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/schematic/" + s.id() + suffix + ".png");
    }

    /**
     * One run of a progress sheet, {@code part} done. A schematic sheet's row 0 is the filling picture
     * and the overlay rows follow it; a plain progress sheet ({@code base}) has the empty picture in
     * row 0, the full one in row 1 and the overlay rows after them. PHASE sheets have a row per stage.
     */
    public static void window(GuiGraphicsExtractor g, Identifier sheet, int x, int y, int w, int h, Mode mode, int frames,
                       float part, int tick, boolean base) {
        int first = base ? 1 : 0;
        int texH = h * (mode == Mode.PHASE ? frames : first + 1 + frames);
        switch (mode) {
            case PHASE -> {
                int row = Math.min(frames - 1, (int) (part * frames));
                g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, row * h, w, h, w, texH);
                return;
            }
            case RIGHT -> {
                if (base) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, 0, w, h, w, texH);
                int filled = Math.round(part * w);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, first * h, filled, h, w, texH);
            }
            case UP -> {
                if (base) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, 0, w, h, w, texH);
                int filled = Math.round(part * h);
                if (filled > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y + h - filled, 0, (first + 1) * h - filled, w, filled, w, texH);
            }
        }
        if (frames > 0) g.blit(RenderPipelines.GUI_TEXTURED, sheet, x, y, 0, (first + 1 + tick % frames) * h, w, h, w, texH);
    }

    /** The names the machines use: a utility's own name, a segment's block name ({@code <id>_segment}). */
    private static Component names(List<String> ids, String kind) {
        var text = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) text.append(", ");
            String id = ids.get(i);
            if ("utility".equals(kind)) {
                machine.utility.Utility utility = machine.utility.Utility.of(id);
                text.append(utility == null ? Component.literal(id) : Component.translatable(utility.nameKey()));
            } else {
                text.append(Component.translatable("block.iuce." + id + "_segment"));
            }
        }
        return text;
    }
}
