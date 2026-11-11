package compat.jei;

import recipe.CraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public class EroCraftingRecipeCategory extends AbstractRecipeCategory<RecipeHolder<CraftingRecipe>> {

    private final IDrawableStatic slotBg;
    private final IDrawableAnimated arrow;

    public EroCraftingRecipeCategory(IJeiHelpers helpers, IRecipeHolderType<CraftingRecipe> recipeType, String title, ItemLike iconItem) {
        super(recipeType, Component.literal(title), helpers.getGuiHelper().createDrawableItemLike(iconItem), 116, 54);
        this.slotBg = helpers.getGuiHelper().getSlotDrawable();
        this.arrow = helpers.getGuiHelper().createAnimatedRecipeArrow(200);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CraftingRecipe> holder, IFocusGroup focuses) {
        CraftingRecipe recipe = holder.value();
        List<Ingredient> inputs = recipe.getInputs();

        if (recipe.isShaped()) {
            boolean[] empty = recipe.getEmptySlots();
            int ingredient = 0;
            for (int i = 0; i < empty.length; i++) {
                if (empty[i]) continue;
                int x = 2 + (i % recipe.getGridWidth()) * 18;
                int y = 2 + (i / recipe.getGridWidth()) * 18;
                builder.addInputSlot(x, y).add(inputs.get(ingredient++));
            }
        } else {
            for (int i = 0; i < inputs.size(); i++) {
                builder.addInputSlot(2 + (i % 3) * 18, 2 + (i / 3) * 18).add(inputs.get(i));
            }
        }

        builder.addOutputSlot(95, 20).add(recipe.getOutput());
    }

    @Override
    public void draw(RecipeHolder<CraftingRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                slotBg.draw(graphics, 1 + col * 18, 1 + row * 18);
            }
        }
        arrow.draw(graphics, 64, 19);
        slotBg.draw(graphics, 94, 19);
    }
}
