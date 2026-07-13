package compat.jei;

import recipe.OptionalCraftingRecipe;
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

/**
 * 可选合成配方 JEI 分类 - 适配工业工作台
 */
public class OptionalCraftingRecipeCategory extends AbstractRecipeCategory<RecipeHolder<OptionalCraftingRecipe>> {

    private final IDrawableStatic slotBg;
    private final IDrawableAnimated arrow;

    public OptionalCraftingRecipeCategory(IJeiHelpers helpers, IRecipeHolderType<OptionalCraftingRecipe> recipeType,
                                          String title, ItemLike iconItem) {
        super(recipeType, Component.literal(title), helpers.getGuiHelper().createDrawableItemLike(iconItem), 116, 54);
        this.slotBg = helpers.getGuiHelper().getSlotDrawable();
        this.arrow = helpers.getGuiHelper().createAnimatedRecipeArrow(200);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OptionalCraftingRecipe> holder, IFocusGroup focuses) {
        OptionalCraftingRecipe recipe = holder.value();
        List<Ingredient> required = recipe.getRequiredInputs();
        List<Ingredient> optional = recipe.getOptionalInputs();

        int total = required.size() + optional.size();
        int gridW = total == 4 ? 2 : 3;
        int slotIndex = 0;

        for (Ingredient ingredient : required) {
            int x = 2 + (slotIndex % gridW) * 18;
            int y = 2 + (slotIndex / gridW) * 18;
            builder.addInputSlot(x, y).add(ingredient);
            slotIndex++;
        }

        for (Ingredient ingredient : optional) {
            int x = 2 + (slotIndex % gridW) * 18;
            int y = 2 + (slotIndex / gridW) * 18;
            builder.addInputSlot(x, y).add(ingredient);
            slotIndex++;
        }

        builder.addOutputSlot(95, 20).add(recipe.getOutput());
    }

    @Override
    public void draw(RecipeHolder<OptionalCraftingRecipe> holder, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                slotBg.draw(graphics, 1 + col * 18, 1 + row * 18);
            }
        }
        arrow.draw(graphics, 64, 19);
        slotBg.draw(graphics, 94, 19);
    }
}
