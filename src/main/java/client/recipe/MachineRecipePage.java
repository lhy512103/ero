package client.recipe;

import client.gui.MachineScreen;
import client.gui.MachineScreen.Window;
import client.recipe.RecipePage.Bounds;
import client.recipe.RecipePage.Frame;
import client.recipe.RecipePage.Mark;
import client.recipe.RecipePage.Role;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.crafting.RecipeHolder;
import recipe.MachineRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class MachineRecipePage {
    private static final int WIDTH = 176, PICTURE_H = MachineScreen.pictureHeight(87);
    private static final int ENERGY_X = 12, ENERGY_Y = 22, ENERGY_W = 7, ENERGY_H = 48;

    /**
     * Where the machine's screen puts each part, in panel coordinates: item positions for slots,
     * {x, y, w, h} for the water tank, the mould's mode or -1.
     */
    public record Layout(String picture, Window window, int[][] inputs, int[][] outputs, int[] fluidIn, int[] catalyst, int mold) {}

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
    private final Frame frame;

    public MachineRecipePage(String machineType, Collection<RecipeHolder<MachineRecipe>> recipes) {
        this.layout = LAYOUTS.getOrDefault(machineType, single(null, null));
        this.frame = frame(layout, recipes);
    }
    public int width() { return frame.w(); }
    public int height() { return frame.h(); }
    public Layout layout() { return layout; }
    public int dx() { return -frame.ox(); }
    public int dy() { return -frame.oy(); }

    /** The parts of the screen the page shows, and every slot any of the recipes uses. */
    private static Frame frame(Layout layout, Collection<RecipeHolder<MachineRecipe>> recipes) {
        Bounds bounds = new Bounds();
        if (layout.picture != null) bounds.picture(MachineScreen.texture(layout.picture), 0, MachineScreen.TOP, WIDTH, PICTURE_H);
        bounds.gauge(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H);
        Window window = layout.window;
        if (window != null) bounds.add(window.x(), window.y(), window.w(), window.h());
        if ("planter".equals(layout.picture)) bounds.add(PlanterScreen.SKY.x(), PlanterScreen.SKY.y(), PlanterScreen.SKY.w(), PlanterScreen.SKY.h());
        if (layout.fluidIn != null) bounds.tank(layout.fluidIn[0], layout.fluidIn[1], layout.fluidIn[2], layout.fluidIn[3]);
        bounds.slot(layout.inputs[0][0], layout.inputs[0][1]);
        bounds.slot(layout.outputs[0][0], layout.outputs[0][1]);
        for (RecipeHolder<MachineRecipe> holder : recipes) {
            for (Mark mark : places(layout, holder.value(), 0, 0)) bounds.slot(mark.x(), mark.y());
        }
        return bounds.frame();
    }

    /** Slot {@code i} of {@code list}; past its end, further along the row ({@code step} pixels apart). */
    private static int[] at(int[][] list, int i, int step) {
        if (i < list.length) return list[i];
        int[] last = list[list.length - 1];
        return new int[]{last[0] + step * (i - list.length + 1), last[1]};
    }

    private static Mark mark(int[] p, Role role, int dx, int dy) {
        return new Mark(p[0] + dx, p[1] + dy, role);
    }

    /**
     * The recipe's slots, moved by dx, dy: the inputs, a fluid input without a tank, the catalyst,
     * the mould, the product, the by-products and a fluid product, in that order.
     */
    public static List<Mark> places(Layout layout, MachineRecipe recipe, int dx, int dy) {
        List<Mark> marks = new ArrayList<>();
        int in = 0;
        marks.add(mark(at(layout.inputs, in++, -18), Role.INPUT, dx, dy));
        if (recipe.getInput2().isPresent()) marks.add(mark(at(layout.inputs, in++, -18), Role.INPUT, dx, dy));
        if (recipe.getFluidInputTemplate().isPresent() && layout.fluidIn == null) marks.add(mark(at(layout.inputs, in++, -18), Role.INPUT, dx, dy));
        if (recipe.getCatalyst().isPresent()) {
            int[] p = layout.catalyst != null && layout.mold < 0 ? layout.catalyst : at(layout.inputs, in++, -18);
            marks.add(mark(p, Role.STATION, dx, dy));
        }
        if (layout.mold >= 0 && MoldItem.forMode(layout.mold) != null) marks.add(mark(layout.catalyst, Role.STATION, dx, dy));
        int out = 0;
        marks.add(mark(at(layout.outputs, out++, 18), Role.OUTPUT, dx, dy));
        for (int i = 0; i < recipe.getExtraResults().size(); i++) marks.add(mark(at(layout.outputs, out++, 18), Role.OUTPUT, dx, dy));
        if (recipe.getFluidOutputTemplate().isPresent()) marks.add(mark(at(layout.outputs, out, 18), Role.OUTPUT, dx, dy));
        return marks;
    }

    public void draw(RecipeHolder<MachineRecipe> holder, GuiGraphicsExtractor graphics) {
        MachineRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        // one run of the page's animation: the recipe's own time, kept between 1.5 and 6 seconds
        long period = Math.clamp(recipe.getProcessTime() * 50L, 1500L, 6000L);
        long now = System.currentTimeMillis();
        float part = (now % period) / (float) period;
        RecipePage.panel(graphics, width(), height());
        if (layout.picture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MachineScreen.texture(layout.picture), dx, MachineScreen.TOP + dy,
                0, 0, WIDTH, PICTURE_H, WIDTH, PICTURE_H);
        }
        MachineScreen.energyGauge(graphics, ENERGY_X + dx, ENERGY_Y + dy, ENERGY_W, ENERGY_H, 1f - part * 0.75f);
        if (layout.fluidIn != null && recipe.getFluidInputTemplate().isPresent()) {
            int[] tank = layout.fluidIn;
            RecipePage.tank(graphics, tank[0] + dx, tank[1] + dy, tank[2], tank[3], true);
        }
        Window window = layout.window;
        if (window != null) {
            int frame = window.frames() > 0 ? (int) (now / 120 % window.frames()) : -1;
            MachineScreen.sheet(graphics, layout.picture + "_progress", window, window.x() + dx, window.y() + dy, part, frame, false);
            if ("planter".equals(layout.picture)) {
                Window sky = PlanterScreen.SKY;
                MachineScreen.sheet(graphics, "planter_sky", sky, sky.x() + dx, sky.y() + dy, 1f, -1, false);
            }
        }
        RecipePage.slots(graphics, places(layout, recipe, dx, dy));
    }

}
