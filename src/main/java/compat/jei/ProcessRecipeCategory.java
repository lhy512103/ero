package compat.jei;

import client.recipe.ProcessRecipePage;
import client.recipe.RecipePage;
import machine.process.ProcessLayout;
import machine.process.ProcessLayout.Box;
import machine.process.ProcessLayout.Pos;
import machine.process.ProcessMachineDef;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import recipe.ProcessRecipe;

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
    private final ProcessMachineDef def;
    private final ProcessLayout layout;
    private final ProcessRecipePage page;

    public ProcessRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType, ProcessMachineDef def,
                                 Component title, ItemLike icon, Collection<RecipeHolder<ProcessRecipe>> recipes) {
        this(helpers, recipeType, def, title, icon, new ProcessRecipePage(def, recipes));
    }

    private ProcessRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType, ProcessMachineDef def,
                                  Component title, ItemLike icon, ProcessRecipePage page) {
        super(recipeType, title, helpers.getGuiHelper().createDrawableItemLike(icon), page.width(), page.height());
        this.def = def;
        this.layout = page.layout();
        this.page = page;
    }

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

    private int dx() { return page.dx(); }

    private int dy() { return page.dy(); }

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
        page.draw(holder, graphics);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<ProcessRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
        ProcessRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        if (RecipePage.over(mouseX, mouseY, layout.energyX + dx - 1, ProcessLayout.TANK_Y + dy - 1, ProcessLayout.ENERGY_W + 2, ProcessLayout.TANK_H + 2)) {
            JeiPage.energy(tooltip, recipe.energy(), recipe.energyPerTick());
        } else if (RecipePage.over(mouseX, mouseY, layout.arrow.x() + dx, layout.arrow.y() + dy, layout.progressW, layout.progressH)) {
            tooltip.add(RecipePage.time(recipe.time()));
            if (releases(recipe)) tooltip.add(Component.translatable("jei.iuce.process.released").withStyle(ChatFormatting.GRAY));
        }
    }

}
