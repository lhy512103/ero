package compat.jei;

import client.gui.MachineScreen;
import compat.jei.JeiPage.Mark;
import compat.jei.JeiPage.Role;
import machine.process.ProgressStyle;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import multiblock.PlantBatch;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JEI page of one industrial multiblock, in the look of the machine pages: on the light panel
 * the energy gauge, the batch's inputs, the run in an arrow window with the tools the plant keeps
 * under it, the products, and the extruder segments the batch needs along the bottom. Inputs,
 * products and kept parts are marked like the machine pages' slots; the energy and time of a batch
 * are the tooltips of the gauge and the arrow. The waste heat power plant makes power rather than
 * things, so its product side is a second gauge filling up.
 * <p>
 * The ore processing plant and the rolling mill run the macerator, ore washer, electric furnace and
 * metal former recipes, so they are shown as stations on those machines' pages instead.
 * </p>
 */
final class PlantRecipeCategory extends AbstractRecipeCategory<PlantBatch> {
    static final List<String> PLANTS = List.of("twin_screw_extruder", "bio_ethylene_plant", "carbide_furnace_plant", "coke_oven_battery", "waste_heat_power_plant");
    static final Map<String, IRecipeType<PlantBatch>> TYPES = new LinkedHashMap<>();
    static { for (String name : PLANTS) TYPES.put(name, IRecipeType.create(Identifier.parse("iuce:" + name), PlantBatch.class)); }

    private static final int PAD = JeiPage.PAD, TOP = PAD + 2, GAUGE_W = 7, ARROW_W = 22, ARROW_H = 16;
    private static final int POWER = 512, STEAM = 64;
    private static final Identifier ARROW = Identifier.fromNamespaceAndPath("iuce", "textures/gui/process/progress/" + ProgressStyle.ARROW.id() + ".png");

    /** Where a batch's parts go on the page (item positions), and the page's size. */
    private record Layout(int rows, int[][] inputs, int[][] outputs, int[][] tools, int[][] segments,
                          int arrowX, int arrowY, int outX, int width, int height) {
        int gaugeH() { return rows * 18 - 2; }
    }

    private final String name;
    private final Layout page;

    static ItemStack icon(String ref) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(ref))); }

    PlantRecipeCategory(IJeiHelpers helpers, String name) {
        this(helpers, name, size(name));
    }

    private PlantRecipeCategory(IJeiHelpers helpers, String name, Layout page) {
        super(TYPES.get(name), icon("iuce:" + name).getHoverName(), helpers.getGuiHelper().createDrawableItemStack(icon("iuce:" + name)),
            page.width, page.height);
        this.name = name;
        this.page = page;
    }

    static List<PlantBatch> recipes(String name) {
        if (name.equals("waste_heat_power_plant"))
            return List.of(new PlantBatch("power", Map.of("minecraft:coal", 1), Map.of(), Map.of(), Map.of(), Map.of(), POWER, 1600, List.of()));
        return PlantBatch.all(name);
    }

    private static boolean power(PlantBatch batch) { return batch.id().equals("power"); }

    /** Every batch of the plant on one page size: the largest of their layouts. */
    private static Layout size(String name) {
        int in = 1, out = 0, tools = 0, segments = 0;
        boolean power = false;
        for (PlantBatch b : recipes(name)) {
            in = Math.max(in, b.inputs().size() + b.fluids().size());
            out = Math.max(out, b.outputs().size() + b.products().size());
            tools = Math.max(tools, b.tools().size());
            segments = Math.max(segments, b.segments().size());
            power |= power(b);
        }
        return layout(in, out, tools, segments, power);
    }

    private static int rowsOf(int n) { return n <= 1 ? 1 : 2; }

    private static int[][] grid(int n, int x, int top, int rows) {
        int[][] at = new int[n][];
        if (n == 0) return at;
        int r = rowsOf(n), cols = (n + r - 1) / r, y = top + (rows - r) * 9;
        for (int i = 0; i < n; i++) at[i] = new int[]{x + (i % cols) * 18, y + (i / cols) * 18};
        return at;
    }

    private static int cols(int n) { return n == 0 ? 0 : (n + rowsOf(n) - 1) / rowsOf(n); }

    private static Layout layout(int in, int out, int tools, int segments, boolean power) {
        int rows = Math.max(Math.max(rowsOf(in), rowsOf(out)), tools > 0 ? 2 : 1);
        // energy gauge, then the inputs, the arrow column and the products, with a 4-pixel gap between
        int inX = PAD + 1 + GAUGE_W + 1 + 4 + 2;
        int[][] inputs = grid(in, inX, TOP, rows);
        int column = inX + cols(in) * 18 + 4;
        int columnW = Math.max(ARROW_W, tools * 18 + 2);
        int arrowX = column + (columnW - ARROW_W) / 2;
        int arrowY = tools > 0 ? TOP : TOP + (rows * 18 - 2 - ARROW_H) / 2;
        int[][] toolAt = new int[tools][];
        int toolX = column + (columnW - tools * 18) / 2 + 2;
        for (int i = 0; i < tools; i++) toolAt[i] = new int[]{toolX + i * 18, TOP + 18 + 2};
        int outX = column + columnW + 4 + 2;
        int[][] outputs = grid(out, outX, TOP, rows);
        int right = power ? outX - 1 + GAUGE_W + 1 : outX + cols(out) * 18 - 2;
        int bottom = TOP + rows * 18 - 2 + (tools > 0 ? 2 : 0);
        int[][] segmentAt = new int[segments][];
        for (int i = 0; i < segments; i++) segmentAt[i] = new int[]{inX + i * 18, bottom + 4 + 2};
        if (segments > 0) {
            bottom = bottom + 4 + 2 + 16;
            right = Math.max(right, inX + segments * 18 - 2);
        }
        return new Layout(rows, inputs, outputs, toolAt, segmentAt, arrowX, arrowY, outX, right + 2 + PAD, bottom + 2 + PAD);
    }

    /** The stacks a reference stands for: an item id, or every item of a {@code #tag}. */
    private static List<ItemStack> stacks(String ref, int count) {
        if (ref.startsWith("#")) {
            return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s -> PlantBatch.matches(s, ref))
                .map(s -> s.copyWithCount(Math.max(1, count))).toList();
        }
        return List.of(icon(ref).copyWithCount(Math.max(1, count)));
    }

    /** The parts of this batch the page marks: inputs, products, tools, segments. */
    private List<Mark> marks(PlantBatch batch) {
        List<Mark> marks = new ArrayList<>();
        int in = batch.inputs().size() + batch.fluids().size();
        for (int i = 0; i < in && i < page.inputs.length; i++) marks.add(new Mark(page.inputs[i][0], page.inputs[i][1], Role.INPUT));
        int out = batch.outputs().size() + batch.products().size();
        for (int i = 0; i < out && i < page.outputs.length; i++) marks.add(new Mark(page.outputs[i][0], page.outputs[i][1], Role.OUTPUT));
        for (int i = 0; i < batch.tools().size() && i < page.tools.length; i++) marks.add(new Mark(page.tools[i][0], page.tools[i][1], Role.STATION));
        for (int i = 0; i < batch.segments().size() && i < page.segments.length; i++)
            marks.add(new Mark(page.segments[i][0], page.segments[i][1], Role.STATION));
        return marks;
    }

    @Override
    public boolean needsRecipeBorder() {
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PlantBatch batch, IFocusGroup focus) {
        int i = 0;
        for (var entry : batch.inputs().entrySet()) {
            if (i >= page.inputs.length) break;
            int[] p = page.inputs[i++];
            builder.addInputSlot(p[0], p[1]).addItemStacks(stacks(entry.getKey(), entry.getValue()));
        }
        for (var entry : batch.fluids().entrySet()) {
            if (i >= page.inputs.length) break;
            int[] p = page.inputs[i++];
            builder.addInputSlot(p[0], p[1]).setFluidRenderer(entry.getValue(), true, 16, 16)
                .add(BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())), entry.getValue());
        }
        i = 0;
        for (var entry : batch.tools().entrySet()) {
            if (i >= page.tools.length) break;
            int[] p = page.tools[i++];
            int wear = entry.getValue();
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, p[0], p[1]).addItemStacks(stacks(entry.getKey(), 1))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.iuce.catalyst_hint").withStyle(ChatFormatting.AQUA));
                    if (wear > 0) tooltip.add(Component.translatable("jei.iuce.catalyst_wear", wear).withStyle(ChatFormatting.GRAY));
                });
        }
        i = 0;
        for (var entry : batch.outputs().entrySet()) {
            if (i >= page.outputs.length) break;
            int[] p = page.outputs[i++];
            builder.addOutputSlot(p[0], p[1]).add(icon(entry.getKey()).copyWithCount(entry.getValue()));
        }
        for (var entry : batch.products().entrySet()) {
            if (i >= page.outputs.length) break;
            int[] p = page.outputs[i++];
            builder.addOutputSlot(p[0], p[1]).setFluidRenderer(entry.getValue(), true, 16, 16)
                .add(BuiltInRegistries.FLUID.getValue(Identifier.parse(entry.getKey())), entry.getValue());
        }
        for (int n = 0; n < batch.segments().size() && n < page.segments.length; n++) {
            int[] p = page.segments[n];
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, p[0], p[1])
                .add(icon("iuce:" + batch.segments().get(n) + "_segment"));
            slot.addRichTooltipCallback((view, tooltip) ->
                tooltip.add(Component.translatable("jei.iuce.plant.segment").withStyle(ChatFormatting.AQUA)));
        }
    }

    @Override
    public void draw(PlantBatch batch, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        long period = Math.clamp(batch.time() * 50L, 1500L, 6000L), now = System.currentTimeMillis();
        float part = (now % period) / (float) period;
        JeiPage.panel(g, getWidth(), getHeight());
        MachineScreen.energyGauge(g, PAD + 1, TOP, GAUGE_W, page.gaugeH(), power(batch) ? 1f : 1f - part * 0.75f);
        if (power(batch)) MachineScreen.energyGauge(g, page.outX - 1, TOP, GAUGE_W, page.gaugeH(), part);
        ProgressStyle arrow = ProgressStyle.ARROW;
        ProcessRecipeCategory.window(g, ARROW, page.arrowX, page.arrowY, arrow.width(), arrow.height(), arrow.mode(), arrow.frames(),
            part, 0, true);
        JeiPage.slots(g, marks(batch));
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, PlantBatch batch, IRecipeSlotsView slots, double mouseX, double mouseY) {
        int h = page.gaugeH() + 2;
        if (JeiPage.over(mouseX, mouseY, PAD, TOP - 1, GAUGE_W + 2, h)) {
            if (power(batch)) tooltip.add(Component.translatable("jei.iuce.plant.fuel").withStyle(ChatFormatting.GRAY));
            else JeiPage.energy(tooltip, batch.energy(), Math.max(1, batch.energy() / Math.max(1, batch.time())));
        } else if (power(batch) && JeiPage.over(mouseX, mouseY, page.outX - 2, TOP - 1, GAUGE_W + 2, h)) {
            tooltip.add(Component.translatable("jei.iuce.plant.power", POWER).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("jei.iuce.plant.steam", STEAM).withStyle(ChatFormatting.GRAY));
        } else if (JeiPage.over(mouseX, mouseY, page.arrowX, page.arrowY, ARROW_W, ARROW_H)) {
            tooltip.add(JeiPage.time(batch.time()));
        }
    }
}
