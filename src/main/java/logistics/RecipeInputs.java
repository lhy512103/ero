package logistics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import recipe.MachineRecipe;
import recipe.ModRecipes;
import recipe.RecipeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Answers "does this machine have any use for this item" for logistics interfaces. Answers are
 * cached per recipe set, so asking is a map lookup; the cache is dropped when recipes are reloaded.
 */
public final class RecipeInputs {
    private RecipeInputs() {}

    private static Object recipesIdentity;
    private static final Map<String, List<MachineRecipe>> BY_TYPE = new HashMap<>();
    private static final Map<String, Boolean> ANSWERS = new HashMap<>();

    private static boolean refresh(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        Object identity = server.recipeAccess().recipeMap();
        if (identity != recipesIdentity) {
            recipesIdentity = identity;
            BY_TYPE.clear();
            ANSWERS.clear();
        }
        return true;
    }

    private static List<MachineRecipe> recipes(ServerLevel level, String machineType) {
        return BY_TYPE.computeIfAbsent(machineType, type -> {
            List<MachineRecipe> list = new ArrayList<>();
            for (var holder : level.recipeAccess().recipeMap().byType(ModRecipes.MACHINE_TYPE.get())) {
                if (holder.value().matchesMachine(type)) list.add(holder.value());
            }
            return list;
        });
    }

    private static boolean cached(Level level, String key, ItemResource resource, Predicate<ItemStack> compute) {
        if (!refresh(level)) return false;
        return ANSWERS.computeIfAbsent(key + "|" + BuiltInRegistries.ITEM.getKey(resource.getItem()), k -> compute.test(resource.toStack(1)));
    }

    /** True when some recipe of the machine type takes the item as its first input. */
    public static boolean isInput(Level level, String machineType, ItemResource resource) {
        return cached(level, "in:" + machineType, resource, stack -> {
            for (MachineRecipe recipe : recipes((ServerLevel) level, machineType)) if (recipe.getInput().test(stack)) return true;
            return false;
        });
    }

    /** True when some recipe of the machine type takes the item as its second input. */
    public static boolean isSecondInput(Level level, String machineType, ItemResource resource) {
        return cached(level, "in2:" + machineType, resource, stack -> {
            for (MachineRecipe recipe : recipes((ServerLevel) level, machineType)) {
                if (recipe.getInput2().isPresent() && recipe.getInput2().get().test(stack)) return true;
            }
            return false;
        });
    }

    /** True when some two-input recipe of the machine type combines the two items, in either order. */
    public static boolean isPair(Level level, String machineType, ItemResource present, ItemResource candidate) {
        String key = "pair:" + machineType + "|" + BuiltInRegistries.ITEM.getKey(present.getItem());
        return cached(level, key, candidate, stack -> {
            ItemStack other = present.toStack(1);
            for (MachineRecipe recipe : recipes((ServerLevel) level, machineType)) {
                if (recipe.getInput2().isEmpty()) continue;
                var first = recipe.getInput();
                var second = recipe.getInput2().get();
                if ((first.test(other) && second.test(stack)) || (first.test(stack) && second.test(other))) return true;
            }
            return false;
        });
    }

    /** True when the planter can grow the item without a data recipe (vanilla and modded crops, saplings, flowers). */
    public static boolean isCrop(Level level, net.minecraft.core.BlockPos pos, ItemResource resource) {
        return cached(level, "crop", resource,
            stack -> machine.planter.CropRegistry.resolve((ServerLevel) level, pos, stack).isPresent());
    }

    /** True when a furnace, blast furnace or smoker recipe exists for the item. */
    public static boolean isCookable(Level level, ItemResource resource) {
        return cached(level, "cook", resource, stack -> RecipeHelper.findCookingRecipe(level, stack) != null);
    }
}
