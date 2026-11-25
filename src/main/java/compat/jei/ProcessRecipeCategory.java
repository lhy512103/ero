package compat.jei;

import client.gui.MachineScreen;
import compat.jei.JeiPage.Bounds;
import compat.jei.JeiPage.Frame;
import compat.jei.JeiPage.Mark;
import compat.jei.JeiPage.Role;
import machine.process.ProcessLayout;
import machine.process.ProcessLayout.Box;
import machine.process.ProcessLayout.Pos;
import machine.process.ProcessMachineDef;
import machine.process.ProcessSchematic;
import machine.process.ProgressStyle;
import machine.process.ProgressStyle.Mode;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import recipe.ProcessRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The JEI page of one process machine, drawn like its process page on the screen's light panel: the
 * machine's picture where it has one (otherwise its usual progress sheet between the slot grids),
 * every slot where the machine has it, the fluids standing in the machine's own tanks, the energy
 * gauge draining and the run animated in the machine's window. The page is cut to those parts; the
 * energy and time of a run are the tooltips of the gauge and the window. Only what has no part of
 * its own (card, utilities, segments) is written underneath, and only on machines that need it.
 * <p>
 * As with the older machines, only certain products are real output slots so pattern encoders do not
 * count chance products. A catalyst says in its tooltip that it is not consumed, so it is not
 * mistaken for a second, optional ingredient.
 * </p>
 * <p>
 * A recipe that throws its input away (the vent stack) has no products at all. Its page shows a
 * flame where the arrow would be and a line saying the fluid is released, so the empty product
 * side is never left to be guessed at.
 * </p>
 */
public class ProcessRecipeCategory extends AbstractRecipeCategory<RecipeHolder<ProcessRecipe>> {
    private static final int LINE = 10;

    /** The content's frame (in panel coordinates) and the text written under it. */
    private record Size(Frame content, int lines, int textWidth) {
        Frame page() {
            int w = Math.max(Math.max(content.w(), textWidth + 2 * JeiPage.PAD), JeiPage.MIN_WIDTH);
            return new Frame(content.ox() - (w - content.w()) / 2, content.oy(), w, content.h() + lines * LINE);
        }
    }

    private final ProcessMachineDef def;
    private final ProcessLayout layout;
    private final IDrawable flame;
    private final Frame frame;
    private final int textY;

    /** @param recipes the machine's recipes, so the page has room for the text any of them needs */
    public ProcessRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType, ProcessMachineDef def,
                                 Component title, ItemLike icon, Collection<RecipeHolder<ProcessRecipe>> recipes) {
        this(helpers, recipeType, def, new ProcessLayout(def), title, icon, recipes);
    }

    private ProcessRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType, ProcessMachineDef def,
                                  ProcessLayout layout, Component title, ItemLike icon, Collection<RecipeHolder<ProcessRecipe>> recipes) {
        this(helpers, recipeType, def, layout, title, icon, size(layout, recipes));
    }

    private ProcessRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType, ProcessMachineDef def,
                                  ProcessLayout layout, Component title, ItemLike icon, Size size) {
        super(recipeType, title, helpers.getGuiHelper().createDrawableItemLike(icon), size.page().w(), size.page().h());
        this.def = def;
        this.layout = layout;
        this.flame = helpers.getGuiHelper().createAnimatedRecipeFlame(300);
        this.frame = size.page();
        this.textY = size.content().h() - JeiPage.PAD + 2;
    }

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

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return ingredient.items().map(holder -> new ItemStack(holder, count)).toList();
    }

    private int capacity(int tank) {
        int in = def.fluidsIn().size();
        List<Integer> list = tank < in ? def.fluidsIn() : def.fluidsOut();
        int index = tank < in ? tank : tank - in;
        return index < list.size() ? list.get(index) : 1000;
    }

    private int dx() { return -frame.ox(); }

    private int dy() { return -frame.oy(); }

    @Override
    public boolean needsRecipeBorder() {
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProcessRecipe> holder, IFocusGroup focuses) {
        ProcessRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        int in = 0;
        for (ProcessRecipe.ItemInput input : recipe.items()) {
            if (in >= layout.inputs.size()) break;
            Pos p = layout.inputs.get(in++);
            builder.addInputSlot(p.x() + dx, p.y() + dy).addItemStacks(stacks(input.ingredient(), input.count()));
        }
        int fluidsIn = def.fluidsIn().size();
        for (int i = 0; i < recipe.fluids().size() && i < fluidsIn && i < layout.tanks.size(); i++) {
            ProcessRecipe.FluidInput input = recipe.fluids().get(i);
            Box box = layout.tanks.get(i);
            // the fluid standing in the machine's own tank, at the level one run takes from it
            var slot = builder.addInputSlot(box.x() + dx, box.y() + dy)
                .setFluidRenderer(Math.max(capacity(i), input.amount()), false, box.w(), box.h())
                .setOverlay(new MachineRecipeCategory.TankMarks(box.w(), box.h()), 0, 0);
            input.ingredient().fluids().forEach(fluid -> slot.add(fluid.value(), input.amount()));
        }
        if (recipe.catalyst().isPresent() && layout.catalyst != null) {
            ProcessRecipe.Catalyst catalyst = recipe.catalyst().get();
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, layout.catalyst.x() + dx, layout.catalyst.y() + dy)
                .addItemStacks(stacks(catalyst.ingredient(), 1))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.iuce.catalyst_hint").withStyle(ChatFormatting.AQUA));
                    if (catalyst.damage() > 0) tooltip.add(Component.translatable("jei.iuce.catalyst_wear", catalyst.damage()).withStyle(ChatFormatting.GRAY));
                });
        }

        int out = 0;
        for (ProcessRecipe.Output output : recipe.results()) {
            if (out >= layout.outputs.size()) break;
            Pos p = layout.outputs.get(out++);
            boolean certain = output.chance() >= 1;
            String percent = String.valueOf(Math.round(output.chance() * 100));
            var slot = certain ? builder.addOutputSlot(p.x() + dx, p.y() + dy)
                : builder.addSlot(RecipeIngredientRole.RENDER_ONLY, p.x() + dx, p.y() + dy);
            slot.add(output.create());
            if (!certain) {
                builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(output.create());
                slot.addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.iuce.chance", percent).withStyle(ChatFormatting.YELLOW)));
            }
        }
        for (int i = 0; i < recipe.fluidResults().size() && fluidsIn + i < layout.tanks.size(); i++) {
            ProcessRecipe.FluidOutput output = recipe.fluidResults().get(i);
            Box box = layout.tanks.get(fluidsIn + i);
            builder.addOutputSlot(box.x() + dx, box.y() + dy)
                .setFluidRenderer(Math.max(capacity(fluidsIn + i), output.amount()), false, box.w(), box.h())
                .setOverlay(new MachineRecipeCategory.TankMarks(box.w(), box.h()), 0, 0)
                .add(output.fluid(), output.amount());
        }
    }

    @Override
    public void draw(RecipeHolder<ProcessRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        ProcessRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        long period = Math.clamp(recipe.time() * 50L, 1500L, 6000L), now = System.currentTimeMillis();
        float part = (now % period) / (float) period;
        JeiPage.panel(graphics, getWidth(), getHeight());
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
        JeiPage.slots(graphics, marks);

        int fluidsIn = def.fluidsIn().size();
        for (int i = 0; i < layout.tanks.size(); i++) {
            Box box = layout.tanks.get(i);
            JeiPage.tank(graphics, box.x() + dx, box.y() + dy, box.w(), box.h(), i < fluidsIn);
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
            flame.draw(graphics, x + (layout.progressW - 14) / 2, y + 1);
        } else {
            ProgressStyle style = def.progress();
            Identifier sheet = Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/progress/" + style.id() + ".png");
            window(graphics, sheet, x, y, style.width(), style.height(), style.mode(), style.frames(), part, frame, true);
        }

        var font = Minecraft.getInstance().font;
        List<Component> lines = lines(recipe);
        for (int i = 0; i < lines.size(); i++) graphics.text(font, lines.get(i), JeiPage.PAD, textY + i * LINE, MachineScreen.LABEL, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<ProcessRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
        ProcessRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        if (JeiPage.over(mouseX, mouseY, layout.energyX + dx - 1, ProcessLayout.TANK_Y + dy - 1, ProcessLayout.ENERGY_W + 2, ProcessLayout.TANK_H + 2)) {
            JeiPage.energy(tooltip, recipe.energy(), recipe.energyPerTick());
        } else if (JeiPage.over(mouseX, mouseY, layout.arrow.x() + dx, layout.arrow.y() + dy, layout.progressW, layout.progressH)) {
            tooltip.add(JeiPage.time(recipe.time()));
            if (releases(recipe)) tooltip.add(Component.translatable("jei.iuce.process.released").withStyle(ChatFormatting.GRAY));
        }
    }

    private static Identifier schematic(ProcessSchematic s, String suffix) {
        return Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/schematic/" + s.id() + suffix + ".png");
    }

    /**
     * One run of a progress sheet, {@code part} done. A schematic sheet's row 0 is the filling picture
     * and the overlay rows follow it; a plain progress sheet ({@code base}) has the empty picture in
     * row 0, the full one in row 1 and the overlay rows after them. PHASE sheets have a row per stage.
     */
    static void window(GuiGraphicsExtractor g, Identifier sheet, int x, int y, int w, int h, Mode mode, int frames,
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
