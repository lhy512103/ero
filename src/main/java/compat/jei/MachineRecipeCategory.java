package compat.jei;

import recipe.MachineRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * One machine's JEI page. Every input is shown with the amount one operation consumes, followed by
 * the fluid and the catalyst; the right side lists the product, the by-products (with their chance)
 * and the fluid produced, so a recipe can be read off completely, e.g. to encode an AE2 pattern.
 */
public class MachineRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {

    private static final int[][] INPUTS = {{1, 1}, {19, 1}, {37, 1}};
    private static final int[][] OUTPUTS = {{85, 1}, {103, 1}, {121, 1}};

    private final IDrawableAnimated arrow;

    public MachineRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<MachineRecipe>> recipeType, String machineName, ItemLike iconItem) {
        super(recipeType, Component.translatable(machineName), helpers.getGuiHelper().createDrawableItemLike(iconItem), 138, 31);
        this.arrow = helpers.getGuiHelper().createAnimatedRecipeArrow(200);
    }

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return ingredient.items().map(holder -> new ItemStack(holder, count)).toList();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        int in = 0;
        builder.addInputSlot(INPUTS[in][0], INPUTS[in][1]).setStandardSlotBackground()
            .addItemStacks(stacks(recipe.getInput(), recipe.getInputCount()));
        in++;
        if (recipe.getInput2().isPresent()) {
            builder.addInputSlot(INPUTS[in][0], INPUTS[in][1]).setStandardSlotBackground()
                .addItemStacks(stacks(recipe.getInput2().get(), recipe.getInput2Count()));
            in++;
        }
        if (recipe.getFluidInputTemplate().isPresent()) {
            var fluid = recipe.getFluidInputTemplate().get();
            builder.addInputSlot(INPUTS[in][0], INPUTS[in][1]).setStandardSlotBackground()
                .add(fluid.fluid().value(), fluid.amount());
            in++;
        }
        if (recipe.getCatalyst().isPresent() && in < INPUTS.length) {
            builder.addInputSlot(INPUTS[in][0], INPUTS[in][1]).setStandardSlotBackground()
                .addItemStacks(stacks(recipe.getCatalyst().get(), 1))
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.iuce.catalyst").withStyle(ChatFormatting.GRAY)));
        }

        int out = 0;
        builder.addOutputSlot(OUTPUTS[out][0], OUTPUTS[out][1]).setStandardSlotBackground().add(recipe.getOutput());
        out++;
        for (MachineRecipe.ExtraResult extra : recipe.getExtraResults()) {
            if (out >= OUTPUTS.length) break;
            String percent = extra.chance() >= 1 ? "100" : String.valueOf(Math.round(extra.chance() * 100));
            builder.addOutputSlot(OUTPUTS[out][0], OUTPUTS[out][1]).setStandardSlotBackground().add(extra.template().create())
                .setOverlay(new ChanceBadge(percent + "%"), 0, 0)
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.iuce.chance", percent).withStyle(ChatFormatting.YELLOW)));
            out++;
        }
        if (recipe.getFluidOutputTemplate().isPresent() && out < OUTPUTS.length) {
            var fluid = recipe.getFluidOutputTemplate().get();
            builder.addOutputSlot(OUTPUTS[out][0], OUTPUTS[out][1]).setStandardSlotBackground()
                .add(fluid.fluid().value(), fluid.amount());
        }
    }

    @Override
    public void draw(RecipeHolder<MachineRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        MachineRecipe recipe = holder.value();
        arrow.draw(graphics, 58, 1);
        String seconds = String.format(java.util.Locale.ROOT, "%.1f", recipe.getProcessTime() / 20.0);
        Component cost = Component.translatable("guide.iuce.recipe.cost", recipe.getEnergyCost(), seconds, recipe.getEnergyPerTick());
        graphics.text(Minecraft.getInstance().font, cost, 1, 22, 0xFF555555, false);
    }

    /** Small yellow percentage in the top-right corner of a by-product slot. */
    private record ChanceBadge(String text) implements mezz.jei.api.gui.drawable.IDrawable {
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
