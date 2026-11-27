package recipe.cache;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import recipe.ModRecipes;
import recipe.ProcessRecipe;
import recipe.RecipeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Process recipes grouped by machine id. Rebuilt whenever the recipe manager hands out a new
 * recipe map (that is, after every reload), so it never needs to be cleared by hand.
 */
public final class ProcessRecipeCache {
    private static Object recipeMapIdentity;
    private static Map<String, List<RecipeHolder<ProcessRecipe>>> byMachine = Map.of();

    private ProcessRecipeCache() {}

    /** All recipes of {@code machine}, in load order; empty when the level has no recipes yet. */
    public static List<RecipeHolder<ProcessRecipe>> recipes(Level level, String machine) {
        RecipeManager manager = level == null ? null : RecipeHelper.getRecipeManager(level);
        if (manager == null) return List.of();
        return index(manager).getOrDefault(machine, List.of());
    }

    private static synchronized Map<String, List<RecipeHolder<ProcessRecipe>>> index(RecipeManager manager) {
        Object identity = manager.recipeMap();
        if (identity != recipeMapIdentity) {
            Map<String, List<RecipeHolder<ProcessRecipe>>> map = new HashMap<>();
            for (RecipeHolder<ProcessRecipe> holder : manager.recipeMap().byType(ModRecipes.PROCESS_TYPE.get())) {
                map.computeIfAbsent(holder.value().machine(), k -> new ArrayList<>()).add(holder);
            }
            byMachine = map;
            recipeMapIdentity = identity;
        }
        return byMachine;
    }
}
