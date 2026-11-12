package multiblock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import recipe.MachineRecipe;
import recipe.RecipeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a large machine makes of one input: the small machines' recipes applied one after the
 * other, with nothing in between ever leaving the machine. Steps that have no recipe for the
 * current item are skipped, so a plant that crushes, washes and smelts also takes dust directly.
 */
public final class ProcessChain {
    private ProcessChain() {}

    /** {@code count} is per batch; by-products that only appear by chance have a fractional count. */
    public record Output(ItemResource item, double count) {}

    /**
     * @param inputCount items of the input one batch uses
     * @param energy     EU one batch costs (half of what the small machines would use)
     */
    public record Chain(int inputCount, List<Output> outputs, long energy) {}

    private static Object recipesIdentity;
    private static final Map<String, Optional<Chain>> CACHE = new HashMap<>();

    public static Optional<Chain> get(ServerLevel level, String[] steps, Item input) {
        Object identity = level.recipeAccess().recipeMap();
        if (identity != recipesIdentity) {
            recipesIdentity = identity;
            CACHE.clear();
        }
        String key = String.join(">", steps) + "|" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input);
        return CACHE.computeIfAbsent(key, k -> compute(level, steps, input));
    }

    private static Optional<Chain> compute(ServerLevel level, String[] steps, Item input) {
        ItemStack current = new ItemStack(input);
        long count = 1, batch = 1, energy = 0;
        boolean any = false;
        Map<Item, Double> extras = new LinkedHashMap<>();
        for (String step : steps) {
            int needed;
            long cost;
            ItemStack result;
            List<MachineRecipe.ExtraResult> byproducts;
            MachineRecipe recipe = RecipeHelper.findMachineRecipe(level, step, current);
            if (recipe != null && !recipe.hasDualInput()) {
                needed = recipe.getInputCount();
                cost = recipe.getEnergyCost();
                result = recipe.getOutput();
                byproducts = recipe.getExtraResults();
            } else if (any && step.equals("electric_furnace")) {
                // plain furnace recipes finish a chain, but never start one
                var cooking = RecipeHelper.findCookingRecipe(level, current);
                if (cooking == null) continue;
                needed = 1;
                cost = 480;
                result = cooking.assemble(new SingleRecipeInput(current));
                byproducts = List.of();
            } else {
                continue;
            }
            if (result.isEmpty() || needed <= 0) continue;
            if (count % needed != 0) {
                // not a whole number of operations: work on a larger batch
                final int factor = needed;
                batch *= factor;
                count *= factor;
                energy *= factor;
                extras.replaceAll((item, amount) -> amount * factor);
            }
            long operations = count / needed;
            energy += cost * operations;
            for (MachineRecipe.ExtraResult extra : byproducts) {
                ItemStack stack = extra.create();
                extras.merge(stack.getItem(), (double) extra.chance() * stack.getCount() * operations, Double::sum);
            }
            current = result.copyWithCount(1);
            count = result.getCount() * operations;
            any = true;
        }
        if (!any || current.getItem() == input || batch > 64) return Optional.empty();
        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output(ItemResource.of(current), count));
        extras.forEach((item, amount) -> outputs.add(new Output(ItemResource.of(new ItemStack(item)), amount)));
        return Optional.of(new Chain((int) batch, List.copyOf(outputs), Math.max(1, energy / 2)));
    }
}
