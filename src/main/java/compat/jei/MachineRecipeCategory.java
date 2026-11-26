package compat.jei;

import client.gui.MachineScreen;
import client.recipe.MachineRecipePage;
import client.recipe.RecipePage;
import client.gui.MachineScreen.Window;
import client.recipe.RecipePage.Mark;
import item.MoldItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import recipe.MachineRecipe;

import java.util.Collection;
import java.util.List;

/**
 * One older machine's JEI page, drawn like its screen: the machine's picture on the screen's light
 * panel with the slots where the screen has them, the water in the tank the screen draws, the energy
 * gauge draining and the run animated in the machine's own window. The page is cut to the parts it
 * shows; the energy and time of a run are the tooltips of the gauge and the window.
 * <p>
 * Every input is shown with the amount one operation consumes. Inputs and guaranteed outputs are
 * ordinary JEI input / output slots with their real amounts, which is what Applied Energistics reads
 * when the "+" button encodes a processing pattern from this page; by-products that only appear by
 * chance are left out of it. The catalyst and the metal former's mould are crafting-station slots:
 * the machine keeps them across runs.
 * </p>
 */
public class MachineRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {
    private static final int ENERGY_X = 12, ENERGY_Y = 22, ENERGY_W = 7, ENERGY_H = 48;
    private final MachineRecipePage page;
    private final MachineRecipePage.Layout layout;

    public MachineRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<MachineRecipe>> recipeType, String machineType,
                                 String machineName, ItemLike iconItem, Collection<RecipeHolder<MachineRecipe>> recipes) {
        this(helpers, recipeType, machineName, iconItem, new MachineRecipePage(machineType, recipes));
    }

    private MachineRecipeCategory(IJeiHelpers helpers, IRecipeType<RecipeHolder<MachineRecipe>> recipeType,
                                  String machineName, ItemLike iconItem, MachineRecipePage page) {
        super(recipeType, Component.translatable(machineName), helpers.getGuiHelper().createDrawableItemLike(iconItem), page.width(), page.height());
        this.page = page;
        this.layout = page.layout();
    }

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        return ingredient.items().map(holder -> new ItemStack(holder, count)).toList();
    }

    private int dx() { return page.dx(); }
    private int dy() { return page.dy(); }

    @Override
    public boolean needsRecipeBorder() {
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        List<Mark> marks = MachineRecipePage.places(layout, recipe, dx, dy);
        int k = 0;
        Mark m = marks.get(k++);
        builder.addInputSlot(m.x(), m.y()).addItemStacks(stacks(recipe.getInput(), recipe.getInputCount()));
        if (recipe.getInput2().isPresent()) {
            m = marks.get(k++);
            builder.addInputSlot(m.x(), m.y()).addItemStacks(stacks(recipe.getInput2().get(), recipe.getInput2Count()));
        }
        if (recipe.getFluidInputTemplate().isPresent()) {
            var fluid = recipe.getFluidInputTemplate().get();
            int[] tank = layout.fluidIn();
            if (tank != null) {
                // the water in the machine's own tank, half full: the level reads as what one run takes
                builder.addInputSlot(tank[0] + dx, tank[1] + dy)
                    .setFluidRenderer(Math.max(1000, fluid.amount() * 2L), false, tank[2], tank[3])
                    .setOverlay(new TankMarks(tank[2], tank[3]), 0, 0)
                    .add(fluid.fluid().value(), fluid.amount());
            } else {
                // Without the renderer JEI paints only the frame and the fluid appears from nowhere.
                m = marks.get(k++);
                builder.addInputSlot(m.x(), m.y())
                    .setFluidRenderer(Math.max(1000, fluid.amount()), true, 16, 16)
                    .add(fluid.fluid().value(), fluid.amount());
            }
        }
        if (recipe.getCatalyst().isPresent()) station(builder, marks.get(k++), stacks(recipe.getCatalyst().get(), 1));
        if (layout.mold() >= 0) {
            Item mold = MoldItem.forMode(layout.mold());
            if (mold != null) station(builder, marks.get(k++), List.of(new ItemStack(mold)));
        }

        m = marks.get(k++);
        builder.addOutputSlot(m.x(), m.y()).add(recipe.getOutput());
        for (MachineRecipe.ExtraResult extra : recipe.getExtraResults()) {
            m = marks.get(k++);
            String percent = extra.chance() >= 1 ? "100" : String.valueOf(Math.round(extra.chance() * 100));
            boolean certain = extra.chance() >= 1;
            var slot = certain ? builder.addOutputSlot(m.x(), m.y())
                : builder.addSlot(RecipeIngredientRole.RENDER_ONLY, m.x(), m.y());
            if (!certain) builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(extra.template().create());
            slot.add(extra.template().create())
                .setOverlay(new ChanceBadge(percent + "%"), 0, 0)
                .addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("jei.iuce.chance", percent).withStyle(ChatFormatting.YELLOW)));
        }
        if (recipe.getFluidOutputTemplate().isPresent()) {
            var fluid = recipe.getFluidOutputTemplate().get();
            m = marks.get(k);
            builder.addOutputSlot(m.x(), m.y())
                .setFluidRenderer(Math.max(1000, fluid.amount()), true, 16, 16)
                .add(fluid.fluid().value(), fluid.amount());
        }
    }

    /** A slot for something the machine keeps across runs: a catalyst or the mould. */
    private static void station(IRecipeLayoutBuilder builder, Mark m, List<ItemStack> stacks) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, m.x(), m.y())
            .addItemStacks(stacks)
            .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                Component.translatable("jei.iuce.catalyst_hint").withStyle(ChatFormatting.AQUA)));
    }

    @Override
    public void draw(RecipeHolder<MachineRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        page.draw(holder, graphics);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<MachineRecipe> holder, IRecipeSlotsView slots, double mouseX, double mouseY) {
        MachineRecipe recipe = holder.value();
        int dx = dx(), dy = dy();
        Window window = layout.window();
        if (RecipePage.over(mouseX, mouseY, ENERGY_X + dx - 1, ENERGY_Y + dy - 1, ENERGY_W + 2, ENERGY_H + 2)) {
            JeiPage.energy(tooltip, recipe.getEnergyCost(), recipe.getEnergyPerTick());
            if (window == null) tooltip.add(RecipePage.time(recipe.getProcessTime()));
        } else if (window != null && RecipePage.over(mouseX, mouseY, window.x() + dx, window.y() + dy, window.w(), window.h())) {
            tooltip.add(RecipePage.time(recipe.getProcessTime()));
        }
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
