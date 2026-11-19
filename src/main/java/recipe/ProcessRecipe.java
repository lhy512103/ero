package recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A recipe for one of the data-driven process machines ({@code iuce:process}). Unlike
 * {@link MachineRecipe} it takes any number of item and fluid inputs and outputs, and can ask for
 * a catalyst, a process card, utilities and extruder segments. The machine does the matching;
 * {@link #matches} is never used.
 *
 * @param machine      id of the process machine that runs it
 * @param items        consumed items; each entry may be spread over several input slots
 * @param fluids       consumed fluids; each entry needs its own input tank
 * @param catalyst     item kept in the catalyst slot, optionally worn down per operation
 * @param card         process card the card slot must hold, with the option levels it must be set to
 * @param utilities    utility ids (steam, cooling water, ...) that must be supplied while running
 * @param segments     extruder segments the machine must have (multiblock extruders only)
 * @param results      produced items, each with a chance
 * @param fluidResults produced fluids
 * @param energy       total EU per operation
 * @param time         ticks per operation before overclocking
 */
public record ProcessRecipe(String machine, List<ItemInput> items, List<FluidInput> fluids,
                            Optional<Catalyst> catalyst, Optional<CardRequirement> card,
                            List<String> utilities, List<String> segments,
                            List<Output> results, List<FluidOutput> fluidResults,
                            long energy, int time) implements Recipe<RecipeInput> {

    public record ItemInput(Ingredient ingredient, int count) {
        public boolean test(ItemStack stack) { return !stack.isEmpty() && ingredient.test(stack); }
    }

    public record FluidInput(FluidIngredient ingredient, int amount) {
        public boolean test(FluidResource resource) {
            return !resource.isEmpty() && ingredient.test(resource.toStack(amount));
        }
    }

    /** @param damage durability taken per operation; 0 = not worn, and ignored for undamageable items */
    public record Catalyst(Ingredient ingredient, int damage) {}

    /** @param options option name → required level (1-4); options not listed may be set to anything */
    public record CardRequirement(String family, Map<String, Integer> options) {}

    public record Output(ItemStackTemplate template, float chance) {
        public ItemStack create() { return template.create(); }
    }

    public record FluidOutput(Fluid fluid, int amount) {
        public FluidStack create() { return new FluidStack(fluid, amount); }
    }

    /** Spread over the run, rounded up; a recipe with no energy (the vent stack) needs no power at all. */
    public long energyPerTick() {
        if (energy <= 0) return 0;
        int ticks = Math.max(1, time);
        return Math.max(1L, (energy + ticks - 1) / ticks);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) { return false; }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().create();
    }

    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }

    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() { return ModRecipes.PROCESS_SERIALIZER.get(); }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() { return ModRecipes.PROCESS_TYPE.get(); }

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }

    @Override
    public List<RecipeDisplay> display() { return List.of(); }

    @Override
    public RecipeBookCategory recipeBookCategory() { return new RecipeBookCategory(); }
}
