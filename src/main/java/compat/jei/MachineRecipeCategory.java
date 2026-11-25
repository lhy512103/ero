package compat.jei;

import client.gui.MachineScreen;
import client.gui.MachineScreen.Window;
import item.MoldItem;
import machine.compressor.screen.CompressorScreen;
import machine.electric_furnace.screen.ElectricFurnaceScreen;
import machine.electrolyzer.screen.ElectrolyzerScreen;
import machine.extractor.screen.ExtractorScreen;
import machine.induction_furnace.screen.InductionFurnaceScreen;
import machine.macerator.screen.MaceratorScreen;
import machine.metal_former.menu.MetalFormerMenu;
import machine.metal_former.screen.MetalFormerScreen;
import machine.ore_washer.screen.OreWasherScreen;
import machine.planter.screen.PlanterScreen;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import recipe.MachineRecipe;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One older machine's JEI page, drawn like its screen: the machine's picture with the slots where the
 * screen has them, the water in the tank the screen draws, the energy gauge draining and the run
 * animated in the machine's own window; the cost is written underneath.
 * <p>
 * Every input is shown with the amount one operation consumes. Inputs and guaranteed outputs are
 * ordinary JEI input / output slots with their real amounts, which is what Applied Energistics reads
 * when the "+" button encodes a processing pattern from this page; by-products that only appear by
 * chance are left out of it. The catalyst and the metal former's mould are crafting-station slots:
 * the machine keeps them across runs.
 * </p>
 */
public class MachineRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {
    /** The page shows the panel from this row down, so the planter's top slot still fits. */
    private static final int OY = 12, WIDTH = 176, TEXT_Y = 74, HEIGHT = 84;
    private static final int PICTURE_H = MachineScreen.pictureHeight(87);
    private static final int ENERGY_X = 12, ENERGY_Y = 22, ENERGY_W = 7, ENERGY_H = 48;

    /**
     * Where the machine's screen puts each part, in panel coordinates: item positions for slots,
     * {x, y, w, h} for the water tank, the mould's mode or -1.
     */
    private record Layout(String picture, Window window, int[][] inputs, int[][] outputs, int[] fluidIn, int[] catalyst, int mold) {}

    private static final int[][] SINGLE_IN = {{79, 17}}, SINGLE_OUT = {{79, 61}, {97, 61}, {115, 61}};

    private static Layout single(String picture, Window window) {
        return new Layout(picture, window, SINGLE_IN, SINGLE_OUT, null, null, -1);
    }

    private static Layout former(int mode) {
        return new Layout("metal_former", MetalFormerScreen.WINDOW, SINGLE_IN, SINGLE_OUT, null,
            new int[]{MetalFormerMenu.MOLD_X, MetalFormerMenu.MOLD_Y}, mode);
    }

    private static final Map<String, Layout> LAYOUTS = Map.ofEntries(
        Map.entry("macerator", single("macerator", MaceratorScreen.WINDOW)),
        Map.entry("compressor", single("compressor", CompressorScreen.WINDOW)),
        Map.entry("extractor", single("extractor", ExtractorScreen.WINDOW)),
        Map.entry("electric_furnace", single("electric_furnace", ElectricFurnaceScreen.WINDOW)),
        Map.entry("metal_former_rolling", former(0)),
        Map.entry("metal_former_cutting", former(1)),
        Map.entry("metal_former_extruding", former(2)),
        Map.entry("alloy_furnace", new Layout("induction_furnace", InductionFurnaceScreen.WINDOW,
            new int[][]{{62, 17}, {98, 17}}, new int[][]{{80, 66}, {98, 66}, {116, 66}}, null, null, -1)),
        Map.entry("ore_washer", new Layout("ore_washer", OreWasherScreen.WINDOW,
            new int[][]{{51, 54}, {51, 23}}, new int[][]{{99, 39}, {99, 62}, {117, 62}}, new int[]{126, 20, 12, 54}, null, -1)),
        Map.entry("electrolyzer", new Layout("electrolyzer", ElectrolyzerScreen.WINDOW,
            new int[][]{{79, 21}}, new int[][]{{47, 56}, {79, 56}, {111, 56}}, new int[]{25, 22, 10, 48}, null, -1)),
        Map.entry("planter", new Layout("planter", PlanterScreen.WINDOW,
            new int[][]{{62, 13}, {62, 62}}, new int[][]{{98, 38}, {116, 38}, {134, 38}}, null, null, -1)));

    private final Layout layout;

    public MachineRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<MachineRecipe>> recipeType, String machineType,
                                 String machineName, ItemLike iconItem) {
        super(recipeType, Component.translatable(machineName), helpers.getGuiHelper().createDrawableItemLike(iconItem), WIDTH, HEIGHT);
        this.layout = LAYOUTS.getOrDefault(machineType, single(null, null));
    }

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return ingredient.items().map(holder -> new ItemStack(holder, count)).toList();
    }

    /** Slot {@code i} of {@code list}; past its end, further along the row ({@code step} pixels apart). */
    private static int[] at(int[][] list, int i, int step) {
        if (i < list.length) return list[i];
        int[] last = list[list.length - 1];
        return new int[]{last[0] + step * (i - list.length + 1), last[1]};
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        int in = 0;
        int[] p = at(layout.inputs, in++, -18);
        builder.addInputSlot(p[0], p[1] - OY).setStandardSlotBackground()
            .addItemStacks(stacks(recipe.getInput(), recipe.getInputCount()));
        if (recipe.getInput2().isPresent()) {
            p = at(layout.inputs, in++, -18);
            builder.addInputSlot(p[0], p[1] - OY).setStandardSlotBackground()
                .addItemStacks(stacks(recipe.getInput2().get(), recipe.getInput2Count()));
        }
        if (recipe.getFluidInputTemplate().isPresent()) {
            var fluid = recipe.getFluidInputTemplate().get();
            int[] tank = layout.fluidIn;
            if (tank != null) {
                // the water in the machine's own tank, half full: the level reads as what one run takes
                builder.addInputSlot(tank[0], tank[1] - OY)
                    .setFluidRenderer(Math.max(1000, fluid.amount() * 2L), false, tank[2], tank[3])
                    .setOverlay(new TankMarks(tank[2], tank[3]), 0, 0)
                    .add(fluid.fluid().value(), fluid.amount());
            } else {
                // Without the renderer JEI paints only the frame and the fluid appears from nowhere.
                p = at(layout.inputs, in++, -18);
                builder.addInputSlot(p[0], p[1] - OY).setStandardSlotBackground()
                    .setFluidRenderer(Math.max(1000, fluid.amount()), true, 16, 16)
                    .add(fluid.fluid().value(), fluid.amount());
            }
        }
        if (recipe.getCatalyst().isPresent()) {
            p = layout.catalyst != null && layout.mold < 0 ? layout.catalyst : at(layout.inputs, in++, -18);
            station(builder, p, stacks(recipe.getCatalyst().get(), 1));
        }
        if (layout.mold >= 0) {
            Item mold = MoldItem.forMode(layout.mold);
            if (mold != null) station(builder, layout.catalyst, List.of(new ItemStack(mold)));
        }

        int out = 0;
        p = at(layout.outputs, out++, 18);
        builder.addOutputSlot(p[0], p[1] - OY).setStandardSlotBackground().add(recipe.getOutput());
        for (MachineRecipe.ExtraResult extra : recipe.getExtraResults()) {
            p = at(layout.outputs, out++, 18);
            String percent = extra.chance() >= 1 ? "100" : String.valueOf(Math.round(extra.chance() * 100));
            boolean certain = extra.chance() >= 1;
            var slot = certain ? builder.addOutputSlot(p[0], p[1] - OY)
                : builder.addSlot(RecipeIngredientRole.RENDER_ONLY, p[0], p[1] - OY);
            if (!certain) builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(extra.template().create());
            slot.setStandardSlotBackground().add(extra.template().create())
                .setOverlay(new ChanceBadge(percent + "%"), 0, 0)
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.iuce.chance", percent).withStyle(ChatFormatting.YELLOW)));
        }
        if (recipe.getFluidOutputTemplate().isPresent()) {
            var fluid = recipe.getFluidOutputTemplate().get();
            p = at(layout.outputs, out, 18);
            builder.addOutputSlot(p[0], p[1] - OY).setStandardSlotBackground()
                .setFluidRenderer(Math.max(1000, fluid.amount()), true, 16, 16)
                .add(fluid.fluid().value(), fluid.amount());
        }
    }

    /** A slot for something the machine keeps across runs: a catalyst or the mould. */
    private static void station(IRecipeLayoutBuilder builder, int[] p, List<ItemStack> stacks) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, p[0], p[1] - OY).setStandardSlotBackground()
            .addItemStacks(stacks)
            .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                Component.translatable("jei.iuce.catalyst_hint").withStyle(ChatFormatting.AQUA)));
    }

    @Override
    public void draw(RecipeHolder<MachineRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        MachineRecipe recipe = holder.value();
        // one run of the page's animation: the recipe's own time, kept between 1.5 and 6 seconds
        long period = Math.clamp(recipe.getProcessTime() * 50L, 1500L, 6000L);
        long now = System.currentTimeMillis();
        float part = (now % period) / (float) period;
        if (layout.picture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MachineScreen.texture(layout.picture), 0, MachineScreen.TOP - OY,
                0, 0, WIDTH, PICTURE_H, WIDTH, PICTURE_H);
            MachineScreen.energyGauge(graphics, ENERGY_X, ENERGY_Y - OY, ENERGY_W, ENERGY_H, 1f - part * 0.75f);
        }
        if (layout.fluidIn != null && recipe.getFluidInputTemplate().isPresent()) {
            int[] tank = layout.fluidIn;
            MachineScreen.tankFrame(graphics, tank[0], tank[1] - OY, tank[2], tank[3], true);
        }
        Window window = layout.window;
        if (window != null) {
            int frame = window.frames() > 0 ? (int) (now / 120 % window.frames()) : -1;
            MachineScreen.sheet(graphics, layout.picture + "_progress", window, window.x(), window.y() - OY, part, frame, false);
            if ("planter".equals(layout.picture)) {
                Window sky = PlanterScreen.SKY;
                MachineScreen.sheet(graphics, "planter_sky", sky, sky.x(), sky.y() - OY, 1f, -1, false);
            }
        }
        String seconds = String.format(Locale.ROOT, "%.1f", recipe.getProcessTime() / 20.0);
        Component cost = Component.translatable("guide.iuce.recipe.cost", recipe.getEnergyCost(), seconds, recipe.getEnergyPerTick());
        graphics.text(Minecraft.getInstance().font, cost, 8, TEXT_Y, MachineScreen.LABEL, false);
    }

    /** The level marks of a tank gauge, drawn over the fluid. */
    record TankMarks(int w, int h) implements IDrawable {
        @Override public int getWidth() { return w; }

        @Override public int getHeight() { return h; }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
            MachineScreen.tankMarks(graphics, x, y, w, h);
        }
    }

    /** Small yellow percentage in the top-right corner of a by-product slot. */
    private record ChanceBadge(String text) implements IDrawable {
        @Override public int getWidth() { return 16; }

        @Override public int getHeight() { return 16; }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
            var font = Minecraft.getInstance().font;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 17, y - 1);
            graphics.pose().scale(0.5f, 0.5f);
            graphics.text(font, text, -font.width(text), 0, 0xFFFFE040, true);
            graphics.pose().popMatrix();
        }
    }
}
