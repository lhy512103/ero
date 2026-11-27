package client.guide;

import client.gui.MachineScreen;
import client.recipe.MachineRecipePage;
import client.recipe.ProcessRecipePage;
import client.recipe.RecipePage;
import client.recipe.RecipePage.Mark;
import item.MoldItem;
import machine.process.ProcessMachines;
import machine.process.ProcessLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import recipe.MachineRecipe;
import recipe.ProcessRecipe;

import java.util.List;

/** Machine recipe artwork shared with JEI, with guide-owned ingredient interactions. */
final class GuideMachineRecipe {
    private final MachineRecipePage machine;
    private final ProcessRecipePage process;
    private final GuideRecipes.View view;

    GuideMachineRecipe(GuideRecipes.View view) {
        this.view = view;
        if (view.kind() == GuideRecipes.Kind.MACHINE) {
            machine = new MachineRecipePage(view.machineType(), List.of(machineHolder()));
            process = null;
        } else {
            var entry = ProcessMachines.get(view.machineType());
            process = entry == null ? null : new ProcessRecipePage(entry.def(), List.of(processHolder()));
            machine = null;
        }
    }

    @SuppressWarnings("unchecked")
    private RecipeHolder<MachineRecipe> machineHolder() { return (RecipeHolder<MachineRecipe>) view.holder(); }
    @SuppressWarnings("unchecked")
    private RecipeHolder<ProcessRecipe> processHolder() { return (RecipeHolder<ProcessRecipe>) view.holder(); }
    boolean available() { return machine != null || process != null; }
    int width() { return machine != null ? machine.width() : process.width(); }
    int height() { return machine != null ? machine.height() : process.height(); }
    int height(int availableWidth) { return (int) Math.ceil(height() * scale(availableWidth)); }
    private float scale(int width) { return Math.min(1f, width / (float) width()); }

    void draw(Ctx c, int x, int y, int availableWidth) {
        float scale = scale(availableWidth);
        int left = x + Math.max(0, (availableWidth - Math.round(width() * scale)) / 2);
        c.g.pose().pushMatrix();
        c.g.pose().translate(left, y);
        c.g.pose().scale(scale, scale);
        if (machine != null) {
            machine.draw(machineHolder(), c.g);
            drawMachine(c, left, y, scale);
        } else {
            process.draw(processHolder(), c.g);
            drawProcess(c, left, y, scale);
        }
        c.g.pose().popMatrix();
    }

    private static int screen(int origin, int value, float scale) { return origin + Math.round(value * scale); }
    private static int extent(int value, float scale) { return Math.max(1, Math.round(value * scale)); }

    private void item(Ctx c, ItemStack stack, int x, int y, int left, int top, float scale) {
        item(c, stack, x, y, left, top, scale, null);
    }

    private void item(Ctx c, ItemStack stack, int x, int y, int left, int top, float scale, Component tip) {
        if (stack.isEmpty()) return;
        c.g.item(stack, x, y);
        c.g.itemDecorations(c.font, stack, x, y);
        int sx = screen(left, x, scale), sy = screen(top, y, scale), size = extent(16, scale);
        if (c.hover(sx, sy, size, size)) c.g.fill(x, y, x + 16, y + 16, GuideStyle.HOVER);
        c.itemZone(sx, sy, size, size, stack, tip);
    }

    private static ItemStack choice(Ingredient ingredient, int count) {
        return GuideStyle.cycle(ingredient.items().map(holder -> new ItemStack(holder, count)).toList());
    }

    private void fluid(Ctx c, Fluid fluid, int amount, int capacity, int x, int y, int w, int h,
                       int left, int top, float scale) {
        int filled = (int) Math.clamp((long) amount * h / Math.max(1, capacity), 1, h);
        var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        var tint = model.fluidTintSource();
        int color = tint == null ? -1 : tint.colorAsStack(new FluidStack(fluid, amount));
        c.g.enableScissor(x, y + h - filled, x + w, y + h);
        for (int tx = 0; tx < w; tx += 16) {
            for (int ty = 0; ty < filled; ty += 16) {
                c.g.blitSprite(RenderPipelines.GUI_TEXTURED, model.stillMaterial().sprite(),
                    x + tx, y + h - ty - 16, 16, 16, color);
            }
        }
        c.g.disableScissor();
        MachineScreen.tankMarks(c.g, x, y, w, h);
        c.fluidZone(screen(left, x, scale), screen(top, y, scale), extent(w, scale), extent(h, scale), fluid,
            Component.empty().append(fluid.getFluidType().getDescription()).append(" · " + amount + " mB"));
    }

    private void drawMachine(Ctx c, int left, int top, float scale) {
        MachineRecipe recipe = machineHolder().value();
        var layout = machine.layout();
        int dx = machine.dx(), dy = machine.dy(), i = 0;
        List<Mark> marks = MachineRecipePage.places(layout, recipe, dx, dy);
        Mark m = marks.get(i++);
        item(c, choice(recipe.getInput(), recipe.getInputCount()), m.x(), m.y(), left, top, scale);
        if (recipe.getInput2().isPresent()) {
            m = marks.get(i++);
            item(c, choice(recipe.getInput2().get(), recipe.getInput2Count()), m.x(), m.y(), left, top, scale);
        }
        if (recipe.getFluidInputTemplate().isPresent()) {
            var input = recipe.getFluidInputTemplate().get();
            int[] tank = layout.fluidIn();
            if (tank == null) {
                m = marks.get(i++);
                fluid(c, input.fluid().value(), input.amount(), Math.max(1000, input.amount()), m.x(), m.y(), 16, 16, left, top, scale);
            } else {
                fluid(c, input.fluid().value(), input.amount(), Math.max(1000, input.amount() * 2),
                    tank[0] + dx, tank[1] + dy, tank[2], tank[3], left, top, scale);
            }
        }
        if (recipe.getCatalyst().isPresent()) {
            m = marks.get(i++);
            item(c, choice(recipe.getCatalyst().get(), 1), m.x(), m.y(), left, top, scale, catalyst(0));
        }
        if (layout.mold() >= 0 && MoldItem.forMode(layout.mold()) != null) {
            m = marks.get(i++);
            item(c, new ItemStack(MoldItem.forMode(layout.mold())), m.x(), m.y(), left, top, scale, catalyst(0));
        }
        m = marks.get(i++);
        item(c, recipe.getOutput(), m.x(), m.y(), left, top, scale);
        for (var extra : recipe.getExtraResults()) {
            m = marks.get(i++);
            item(c, extra.create(), m.x(), m.y(), left, top, scale, chance(extra.chance()));
            chanceBadge(c, m.x(), m.y(), extra.chance());
        }
        if (recipe.getFluidOutputTemplate().isPresent()) {
            var output = recipe.getFluidOutputTemplate().get();
            m = marks.get(i);
            fluid(c, output.fluid().value(), output.amount(), Math.max(1000, output.amount()), m.x(), m.y(), 16, 16, left, top, scale);
        }
        int[] gauge = {12 + dx, 22 + dy, 7, 48};
        info(c, gauge[0], gauge[1], gauge[2], gauge[3], left, top, scale, recipe.getEnergyPerTick());
        var window = layout.window();
        if (window != null) {
            c.tipZone(screen(left, window.x() + dx, scale), screen(top, window.y() + dy, scale),
                extent(window.w(), scale), extent(window.h(), scale), RecipePage.time(recipe.getProcessTime()));
        }
    }

    private void drawProcess(Ctx c, int left, int top, float scale) {
        ProcessRecipe recipe = processHolder().value();
        var layout = process.layout();
        var def = ProcessMachines.get(view.machineType()).def();
        int dx = process.dx(), dy = process.dy();
        for (int i = 0; i < recipe.items().size() && i < layout.inputs.size(); i++) {
            var p = layout.inputs.get(i);
            var input = recipe.items().get(i);
            item(c, choice(input.ingredient(), input.count()), p.x() + dx, p.y() + dy, left, top, scale);
        }
        for (int i = 0; i < recipe.results().size() && i < layout.outputs.size(); i++) {
            var p = layout.outputs.get(i);
            var output = recipe.results().get(i);
            Component tip = output.chance() < 1 ? chance(output.chance()) : null;
            item(c, output.create(), p.x() + dx, p.y() + dy, left, top, scale, tip);
        }
        if (layout.catalyst != null && recipe.catalyst().isPresent()) {
            var p = layout.catalyst;
            var catalyst = recipe.catalyst().get();
            item(c, choice(catalyst.ingredient(), 1), p.x() + dx, p.y() + dy, left, top, scale, catalyst(catalyst.damage()));
        }
        int in = def.fluidsIn().size();
        for (int i = 0; i < recipe.fluids().size() && i < in && i < layout.tanks.size(); i++) {
            var input = recipe.fluids().get(i);
            var fluids = input.ingredient().fluids();
            var box = layout.tanks.get(i);
            if (fluids.isEmpty()) continue;
            Fluid selected = fluids.get((int) (System.currentTimeMillis() / 1000 % fluids.size())).value();
            fluid(c, selected, input.amount(), Math.max(def.fluidsIn().get(i), input.amount()),
                box.x() + dx, box.y() + dy, box.w(), box.h(), left, top, scale);
        }
        for (int i = 0; i < recipe.fluidResults().size() && in + i < layout.tanks.size(); i++) {
            var output = recipe.fluidResults().get(i);
            var box = layout.tanks.get(in + i);
            fluid(c, output.fluid(), output.amount(), Math.max(def.fluidsOut().get(i), output.amount()), box.x() + dx, box.y() + dy, box.w(), box.h(), left, top, scale);
        }
        info(c, layout.energyX + dx, ProcessLayout.TANK_Y + dy, ProcessLayout.ENERGY_W, ProcessLayout.TANK_H, left, top, scale, recipe.energyPerTick());
        c.tipZone(screen(left, layout.arrow.x() + dx, scale), screen(top, layout.arrow.y() + dy, scale), extent(layout.progressW, scale), extent(layout.progressH, scale), RecipePage.time(recipe.time()));
    }

    private static Component catalyst(int damage) {
        var tip = Component.translatable("jei.iuce.catalyst_hint").withStyle(ChatFormatting.AQUA);
        if (damage > 0) tip.append("\n").append(Component.translatable("jei.iuce.catalyst_wear", damage).withStyle(ChatFormatting.GRAY));
        return tip;
    }

    private static Component chance(float chance) {
        return Component.translatable("jei.iuce.chance", Math.round(chance * 100)).withStyle(ChatFormatting.YELLOW);
    }

    private static void chanceBadge(Ctx c, int x, int y, float chance) {
        String percent = Math.round(chance * 100) + "%";
        c.g.pose().pushMatrix();
        c.g.pose().translate(x + 17, y - 1);
        c.g.pose().scale(.5f, .5f);
        c.g.text(c.font, percent, -c.font.width(percent), 0, 0xFFFFE040, true);
        c.g.pose().popMatrix();
    }

    private void info(Ctx c, int x, int y, int w, int h, int left, int top, float scale, long rate) {
        c.tipZone(screen(left, x, scale), screen(top, y, scale), extent(w, scale), extent(h, scale),
            Component.translatable("guide.iuce.recipe.cost", view.energy(), String.format(java.util.Locale.ROOT, "%.1f", view.time() / 20f), rate));
    }
}
