package compat.jei;

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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import recipe.ProcessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The JEI page of one process machine. Inputs (items, then fluids, then the catalyst) fill a 4×2
 * grid on the left, products a 3×2 grid on the right, with the card, utilities and segments the
 * recipe needs listed underneath. As with the older machines, only certain products are real output
 * slots so pattern encoders do not count chance products.
 * <p>
 * Every fluid slot gets a renderer: without one JEI draws a bare frame and only the tooltip names
 * the fluid, which reads as an input nobody can see. A catalyst is drawn in its own slot and says
 * in its tooltip that it is not consumed, so it is not mistaken for a second, optional ingredient.
 * </p>
 * <p>
 * A recipe that throws its input away (the vent stack) has no products at all. Its page shows a
 * flame where the arrow would be and a line saying the fluid is released, so the empty product
 * side is never left to be guessed at.
 * </p>
 */
public class ProcessRecipeCategory extends AbstractRecipeCategory<RecipeHolder<ProcessRecipe>> {
    private static final int OUTPUT_X = 102, ARROW_X = 76, ARROW_Y = 10;
    /** Capacity of a slot-sized fluid renderer: one bucket, so any recipe amount reads as a level. */
    private static final long SLOT_CAPACITY = 1000;

    private final IJeiHelpers helpers;
    private final IDrawable arrow;
    private final IDrawable flame;

    public ProcessRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType, Component title, ItemLike icon) {
        super(recipeType, title, helpers.getGuiHelper().createDrawableItemLike(icon), 158, 68);
        this.helpers = helpers;
        this.arrow = helpers.getGuiHelper().createAnimatedRecipeArrow(200);
        this.flame = helpers.getGuiHelper().createAnimatedRecipeFlame(300);
    }

    /** True when the recipe consumes its input and produces nothing: a vent or a flare. */
    private static boolean releases(ProcessRecipe recipe) {
        return recipe.results().isEmpty() && recipe.fluidResults().isEmpty();
    }

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return ingredient.items().map(holder -> new ItemStack(holder, count)).toList();
    }

    private static int x(int index, int columns, int left) { return left + (index % columns) * 18; }

    private static int y(int index, int columns) { return 1 + (index / columns) * 18; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProcessRecipe> holder, IFocusGroup focuses) {
        ProcessRecipe recipe = holder.value();
        var gui = helpers.getGuiHelper();
        int in = 0;
        for (ProcessRecipe.ItemInput input : recipe.items()) {
            if (in >= 8) break;
            builder.addInputSlot(x(in, 4, 1), y(in, 4)).setStandardSlotBackground().addItemStacks(stacks(input.ingredient(), input.count()));
            in++;
        }
        for (ProcessRecipe.FluidInput input : recipe.fluids()) {
            if (in >= 8) break;
            // The renderer is what actually paints the fluid; a slot without one shows only a frame.
            var slot = builder.addInputSlot(x(in, 4, 1), y(in, 4)).setStandardSlotBackground()
                .setFluidRenderer(Math.max(SLOT_CAPACITY, input.amount()), true, 16, 16);
            input.ingredient().fluids().forEach(fluid -> slot.add(fluid.value(), input.amount()));
            in++;
        }
        if (recipe.catalyst().isPresent() && in < 8) {
            ProcessRecipe.Catalyst catalyst = recipe.catalyst().get();
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, x(in, 4, 1), y(in, 4)).setStandardSlotBackground()
                .addItemStacks(stacks(catalyst.ingredient(), 1))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.iuce.catalyst_hint").withStyle(ChatFormatting.AQUA));
                    if (catalyst.damage() > 0) tooltip.add(Component.translatable("jei.iuce.catalyst_wear", catalyst.damage()).withStyle(ChatFormatting.GRAY));
                });
        }

        int out = 0;
        for (ProcessRecipe.Output output : recipe.results()) {
            if (out >= 6) break;
            boolean certain = output.chance() >= 1;
            String percent = String.valueOf(Math.round(output.chance() * 100));
            var slot = certain ? builder.addOutputSlot(x(out, 3, OUTPUT_X), y(out, 3))
                : builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x(out, 3, OUTPUT_X), y(out, 3));
            slot.setStandardSlotBackground().add(output.create());
            if (!certain) {
                builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(output.create());
                slot.addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.iuce.chance", percent).withStyle(ChatFormatting.YELLOW)));
            }
            out++;
        }
        for (ProcessRecipe.FluidOutput output : recipe.fluidResults()) {
            if (out >= 6) break;
            builder.addOutputSlot(x(out, 3, OUTPUT_X), y(out, 3)).setStandardSlotBackground()
                .setFluidRenderer(Math.max(SLOT_CAPACITY, output.amount()), true, 16, 16)
                .add(output.fluid(), output.amount());
            out++;
        }
    }

    @Override
    public void draw(RecipeHolder<ProcessRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        ProcessRecipe recipe = holder.value();
        if (releases(recipe)) flame.draw(graphics, ARROW_X + 3, ARROW_Y);
        else arrow.draw(graphics, ARROW_X, ARROW_Y);
        var font = Minecraft.getInstance().font;
        List<Component> lines = new ArrayList<>();
        String seconds = String.format(Locale.ROOT, "%.1f", recipe.time() / 20.0);
        lines.add(Component.translatable("guide.iuce.recipe.cost", recipe.energy(), seconds, recipe.energyPerTick()));
        recipe.card().ifPresent(card -> lines.add(Component.translatable("jei.iuce.process.card",
            machine.process.ProcessCards.label(card))));
        if (!recipe.utilities().isEmpty()) lines.add(Component.translatable("jei.iuce.process.utilities", names(recipe.utilities(), "utility")));
        if (!recipe.segments().isEmpty()) lines.add(Component.translatable("jei.iuce.process.segments", names(recipe.segments(), "segment")));
        if (releases(recipe)) lines.add(Component.translatable("jei.iuce.process.released_line").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < lines.size() && i < 3; i++) graphics.text(font, lines.get(i), 1, 38 + i * 10, 0xFF555555, false);
    }

    private static Component names(List<String> ids, String kind) {
        var text = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) text.append(", ");
            text.append(Component.translatable("jei.iuce." + kind + "." + ids.get(i)));
        }
        return text;
    }
}
