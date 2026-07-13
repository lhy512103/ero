package recipe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import recipe.cache.MachineRecipeCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeHelper {

    /**
     * 获取 RecipeManager
     * MC 26.1 客户端的 level.recipeAccess() 返回 ClientRecipeContainer（不是 RecipeManager），
     * 因此需要从集成服务器获取
     */
    public static RecipeManager getRecipeManager(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.recipeAccess();
        }
        // 客户端：尝试从集成服务器获取
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.server.MinecraftServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            return server.getRecipeManager();
        }
        return null;
    }

    /**
     * 查找匹配的机器配方（使用缓存，O(1) 查找）
     *
     * @param level      世界
     * @param machineType 机器类型（如 "macerator", "compressor" 等）
     * @param input      输入物品
     * @return 匹配的配方，如果没有找到则返回 null
     */
    public static MachineRecipe findMachineRecipe(Level level, String machineType, ItemStack input) {
        if (level == null || input.isEmpty()) return null;
        return MachineRecipeCache.get(machineType).findRecipe(level, input);
    }

    /**
     * 查找匹配的双输入机器配方（使用缓存，O(1) 查找）
     *
     * @param level      世界
     * @param machineType 机器类型（如 "induction_furnace" 等）
     * @param input      第一个输入物品
     * @param input2     第二个输入物品
     * @return 匹配的配方，如果没有找到则返回 null
     */
    public static MachineRecipe findMachineRecipe(Level level, String machineType, ItemStack input, ItemStack input2) {
        if (level == null || input.isEmpty()) return null;
        return MachineRecipeCache.get(machineType).findRecipe(level, input, input2);
    }

    /**
     * 查找匹配的合成配方
     *
     * @param level  世界
     * @param inputs 输入物品列表
     * @return 匹配的配方，如果没有找到则返回 null
     */
    public static CraftingRecipe findCraftingRecipe(Level level, List<ItemStack> inputs) {
        if (level == null || inputs.isEmpty()) return null;

        RecipeManager recipeManager = getRecipeManager(level);
        if (recipeManager == null) return null;

        // 创建 CraftingInput (无序合成，单行排列)
        List<ItemStack> copyList = new ArrayList<>();
        for (ItemStack stack : inputs) {
            copyList.add(stack.copy());
        }
        CraftingInput craftingInput = CraftingInput.of(inputs.size(), 1, copyList);

        Optional<RecipeHolder<CraftingRecipe>> result = recipeManager.getRecipeFor(
            ModRecipes.ERO_CRAFTING_TYPE.get(), craftingInput, level
        );

        return result.map(RecipeHolder::value).orElse(null);
    }

    /**
     * 查找匹配的原版熔炼配方
     *
     * @param level  世界
     * @param input  输入物品
     * @return 匹配的熔炼配方，如果没有找到则返回 null
     */
    public static SmeltingRecipe findSmeltingRecipe(Level level, ItemStack input) {
        if (level == null || input.isEmpty()) return null;

        RecipeManager recipeManager = getRecipeManager(level);
        if (recipeManager == null) return null;

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);

        Optional<RecipeHolder<SmeltingRecipe>> result = recipeManager.getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.SMELTING, recipeInput, level
        );

        return result.map(RecipeHolder::value).orElse(null);
    }
}
