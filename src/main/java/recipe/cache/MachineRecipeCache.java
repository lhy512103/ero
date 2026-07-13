package recipe.cache;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import recipe.MachineRecipe;
import recipe.ModRecipes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机器配方缓存系统（参考 Mekanism InputRecipeCache 设计）
 * <p>
 * 核心思路：按 machineType + 输入 Ingredient 建立索引，实现 O(1) 查找。
 * 使用惰性初始化，仅在首次查询时构建缓存；配方重载时自动失效。
 * <p>
 * 对于无法快速索引的复杂 Ingredient（如 NBT 匹配），放入 complexRecipes 回退集线性遍历。
 */
public class MachineRecipeCache {

    private static final Map<String, MachineRecipeCache> CACHES = new LinkedHashMap<>();
    private static boolean globalInitialized;

    private final String machineType;

    // 主索引：Item → 配方列表（快速路径）
    private final Map<Item, List<RecipeHolder<MachineRecipe>>> itemIndex = new HashMap<>();
    // 回退集：无法通过 Item 快速索引的配方（如 tag/NBT 匹配）
    private final Set<RecipeHolder<MachineRecipe>> complexRecipes = new LinkedHashSet<>();

    private boolean initialized;

    private MachineRecipeCache(String machineType) {
        this.machineType = machineType;
    }

    /**
     * 获取指定机器类型的配方缓存实例
     */
    public static MachineRecipeCache get(String machineType) {
        return CACHES.computeIfAbsent(machineType, MachineRecipeCache::new);
    }

    /**
     * 清除所有缓存（配方重载时调用）
     */
    public static void clearAll() {
        globalInitialized = false;
        CACHES.values().forEach(MachineRecipeCache::clear);
    }

    /**
     * 清除此缓存
     */
    public void clear() {
        initialized = false;
        itemIndex.clear();
        complexRecipes.clear();
    }

    /**
     * 获取 RecipeManager（从服务端或集成服务器）
     */
    private static RecipeManager getRecipeManager() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            return server.getRecipeManager();
        }
        return null;
    }

    /**
     * 惰性初始化缓存
     */
    private void initCacheIfNeeded() {
        if (initialized) return;
        initialized = true;

        RecipeManager recipeManager = getRecipeManager();
        if (recipeManager == null) return;

        Collection<RecipeHolder<MachineRecipe>> allRecipes =
            recipeManager.recipeMap().byType(ModRecipes.MACHINE_TYPE.get());

        for (RecipeHolder<MachineRecipe> holder : allRecipes) {
            MachineRecipe recipe = holder.value();
            if (!recipe.getMachineType().equals(machineType)) continue;

            // 索引输入1
            if (indexIngredient(recipe.getInput(), holder, itemIndex)) {
                complexRecipes.add(holder);
            }
        }
    }

    /**
     * 将配方索引到指定的 Map 中
     *
     * @return true 如果是复杂 Ingredient（无法快速索引）
     */
    private boolean indexIngredient(Ingredient ingredient, RecipeHolder<MachineRecipe> holder,
                                     Map<Item, List<RecipeHolder<MachineRecipe>>> index) {
        // MC 26.1: items() 返回 Stream<Holder<Item>>
        List<Item> items = ingredient.items().map(Holder::value).distinct().collect(Collectors.toList());
        if (items.isEmpty()) return true;

        // 如果只涉及一个 Item，可以快速索引
        if (items.size() == 1) {
            index.computeIfAbsent(items.get(0), k -> new ArrayList<>()).add(holder);
            return false;
        }

        // 多个 Item：放入所有涉及的 Item 索引 + 回退集
        for (Item item : items) {
            index.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
        }
        return true;
    }

    /**
     * 查找匹配的单输入配方（O(1) 查找 + 回退遍历）
     *
     * @param level 世界（用于获取配方管理器）
     * @param input 输入物品
     * @return 匹配的配方，无则返回 null
     */
    public MachineRecipe findRecipe(Level level, ItemStack input) {
        if (input.isEmpty()) return null;
        initCacheIfNeeded();

        // 快速路径：按 Item 查找
        List<RecipeHolder<MachineRecipe>> candidates = itemIndex.get(input.getItem());
        if (candidates != null) {
            for (RecipeHolder<MachineRecipe> holder : candidates) {
                if (holder.value().getInput().test(input)) {
                    return holder.value();
                }
            }
        }

        // 回退路径：遍历复杂配方
        for (RecipeHolder<MachineRecipe> holder : complexRecipes) {
            if (holder.value().getInput().test(input)) {
                return holder.value();
            }
        }

        return null;
    }

    /**
     * 查找匹配的双输入配方
     *
     * @param level  世界
     * @param input1 第一个输入
     * @param input2 第二个输入
     * @return 匹配的配方，无则返回 null
     */
    public MachineRecipe findRecipe(Level level, ItemStack input1, ItemStack input2) {
        if (input1.isEmpty() || input2.isEmpty()) return null;
        initCacheIfNeeded();

        // 快速路径：按 input1 的 Item 查找
        List<RecipeHolder<MachineRecipe>> candidates = itemIndex.get(input1.getItem());
        if (candidates != null) {
            for (RecipeHolder<MachineRecipe> holder : candidates) {
                MachineRecipe recipe = holder.value();
                if (recipe.getInput().test(input1) && recipe.getInput2().isPresent()
                        && recipe.getInput2().get().test(input2)) {
                    return recipe;
                }
            }
        }

        // 回退路径
        for (RecipeHolder<MachineRecipe> holder : complexRecipes) {
            MachineRecipe recipe = holder.value();
            if (recipe.getInput().test(input1) && recipe.getInput2().isPresent()
                    && recipe.getInput2().get().test(input2)) {
                return recipe;
            }
        }

        return null;
    }

    /**
     * 检查是否有匹配指定输入的配方（用于 UI 显示等场景）
     */
    public boolean hasRecipeFor(ItemStack input) {
        if (input.isEmpty()) return false;
        initCacheIfNeeded();

        List<RecipeHolder<MachineRecipe>> candidates = itemIndex.get(input.getItem());
        if (candidates != null) {
            for (RecipeHolder<MachineRecipe> holder : candidates) {
                if (holder.value().getInput().test(input)) return true;
            }
        }
        for (RecipeHolder<MachineRecipe> holder : complexRecipes) {
            if (holder.value().getInput().test(input)) return true;
        }
        return false;
    }

    /**
     * 获取指定机器类型的所有配方
     */
    public List<MachineRecipe> getAllRecipes() {
        initCacheIfNeeded();
        List<MachineRecipe> result = new ArrayList<>();
        Set<MachineRecipe> seen = new HashSet<>();

        for (List<RecipeHolder<MachineRecipe>> list : itemIndex.values()) {
            for (RecipeHolder<MachineRecipe> holder : list) {
                if (seen.add(holder.value())) {
                    result.add(holder.value());
                }
            }
        }
        for (RecipeHolder<MachineRecipe> holder : complexRecipes) {
            if (seen.add(holder.value())) {
                result.add(holder.value());
            }
        }
        return result;
    }

    /**
     * 获取缓存统计信息（调试用）
     */
    public String getStats() {
        initCacheIfNeeded();
        return String.format("MachineRecipeCache[%s]: %d items indexed, %d complex",
                machineType, itemIndex.size(), complexRecipes.size());
    }
}
