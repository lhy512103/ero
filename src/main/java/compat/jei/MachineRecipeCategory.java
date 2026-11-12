package compat.jei;

import recipe.MachineRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

public class MachineRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {

    private final IDrawableStatic slotBg;
    private final IDrawableAnimated arrow;

    public MachineRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<MachineRecipe>> recipeType, String machineName, ItemLike iconItem) {
        super(recipeType, Component.translatable(machineName), helpers.getGuiHelper().createDrawableItemLike(iconItem), 116, 54);
        this.slotBg = helpers.getGuiHelper().getSlotDrawable();
        this.arrow = helpers.getGuiHelper().createAnimatedRecipeArrow(200);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        if (recipe.hasDualInput()) {
            // 双输入配方：两个输入槽并排
            builder.addInputSlot(7, 21).add(recipe.getInput());
            builder.addInputSlot(27, 21).add(recipe.getInput2().get());
            builder.addOutputSlot(96, 21).add(recipe.getOutput());
        } else {
            // 单输入配方
            builder.addInputSlot(7, 21).add(recipe.getInput());
            builder.addOutputSlot(96, 21).add(recipe.getOutput());
        }
    }

    @Override
    public void draw(RecipeHolder<MachineRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        MachineRecipe recipe = holder.value();
        if (recipe.hasDualInput()) {
            // 双输入：两个槽位
            slotBg.draw(graphics, 6, 20);
            slotBg.draw(graphics, 26, 20);
            arrow.draw(graphics, 58, 20);
            slotBg.draw(graphics, 95, 20);
        } else {
            // 单输入
            slotBg.draw(graphics, 6, 20);
            arrow.draw(graphics, 48, 20);
            slotBg.draw(graphics, 95, 20);
        }
    }
}
