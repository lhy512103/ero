package recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 通用合成配方，同时支持 {@code ero:crafting}（工业工作台）和 {@code ero:standard_crafting}（原版工作台）。
 * <p>
 * 通过 {@link #isStandardCrafting()} 区分两种模式：
 * <ul>
 *   <li>standard_crafting → 对原版工作台暴露为 {@link RecipeType#CRAFTING}，可正常匹配</li>
 *   <li>crafting → 保持自定义类型，仅工业工作台可用</li>
 * </ul>
 * </p>
 */
public class CraftingRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

    private final List<Ingredient> inputs;       // 非空 Ingredient 列表（用于 PlacementInfo / 序列化）
    private final ItemStackTemplate outputTemplate;
    private final boolean standardCrafting;
    private final boolean shaped;
    private final int gridWidth;                 // 有序配方网格宽度
    private final int gridHeight;                // 有序配方网格高度
    /** 有序配方时长度为 gridWidth*gridHeight，true 表示该位置为空位 */
    private final boolean[] emptySlots;
    /** 有序配方时长度为 gridWidth*gridHeight，保存每个网格位置的 Ingredient（空位为 null） */
    private final Ingredient[] gridIngredients;
    private final Supplier<RecipeSerializer<?>> serializerSupplier;

    /** 工厂方法：无序配方 */
    static CraftingRecipe create(List<Ingredient> inputs, ItemStackTemplate output,
                                 RecipeType<?> type, Supplier<RecipeSerializer<?>> serializerSupplier) {
        return new CraftingRecipe(inputs, output,
            type == ModRecipes.STANDARD_CRAFTING_TYPE.get(), serializerSupplier);
    }

    /** 工厂方法：有序配方（3x3 网格含空位） */
    static CraftingRecipe createShaped(List<Ingredient> gridInputs, boolean[] emptySlots,
                                       int gridWidth, int gridHeight,
                                       ItemStackTemplate output, RecipeType<?> type,
                                       Supplier<RecipeSerializer<?>> serializerSupplier) {
        return new CraftingRecipe(gridInputs, emptySlots, gridWidth, gridHeight,
            output, type == ModRecipes.STANDARD_CRAFTING_TYPE.get(), serializerSupplier);
    }

    /** 无序配方构造 */
    private CraftingRecipe(List<Ingredient> inputs, ItemStackTemplate output,
                           boolean standardCrafting, Supplier<RecipeSerializer<?>> serializerSupplier) {
        this.inputs = inputs;
        this.outputTemplate = output;
        this.standardCrafting = standardCrafting;
        this.shaped = false;
        this.gridWidth = 0;
        this.gridHeight = 0;
        this.emptySlots = null;
        this.gridIngredients = null;
        this.serializerSupplier = serializerSupplier;
    }

    /** 有序配方构造 */
    private CraftingRecipe(List<Ingredient> gridInputs, boolean[] emptySlots,
                           int gridWidth, int gridHeight,
                           ItemStackTemplate output, boolean standardCrafting,
                           Supplier<RecipeSerializer<?>> serializerSupplier) {
        this.emptySlots = emptySlots;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.standardCrafting = standardCrafting;
        this.shaped = true;
        this.outputTemplate = output;
        this.serializerSupplier = serializerSupplier;

        // 构建 gridIngredients（含 null 的完整网格）
        this.gridIngredients = new Ingredient[gridWidth * gridHeight];
        List<Ingredient> nonEmpty = new ArrayList<>();
        int inputIndex = 0;
        for (int i = 0; i < gridWidth * gridHeight; i++) {
            if (emptySlots[i]) {
                gridIngredients[i] = null;
            } else {
                Ingredient ingredient = gridInputs.get(inputIndex++);
                gridIngredients[i] = ingredient;
                nonEmpty.add(ingredient);
            }
        }
        this.inputs = nonEmpty;
    }

    public CraftingRecipe(List<Ingredient> inputs, ItemStack output,
                          RecipeType<CraftingRecipe> recipeType, Supplier<RecipeSerializer<?>> serializerSupplier) {
        this(inputs, new ItemStackTemplate(output.getItem(), output.getCount()),
            recipeType == ModRecipes.STANDARD_CRAFTING_TYPE.get(), serializerSupplier);
    }

    // ── 查询 ──

    public List<Ingredient> getInputs() { return inputs; }
    public ItemStack getOutput() { return outputTemplate.create(); }
    public ItemStackTemplate getOutputTemplate() { return outputTemplate; }
    public boolean isStandardCrafting() { return standardCrafting; }
    public boolean isShaped() { return shaped; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public boolean[] getEmptySlots() { return emptySlots; }

    // ── Recipe 接口 ──

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (shaped) {
            return matchesShaped(input);
        }
        return matchesShapeless(input);
    }

    private boolean matchesShaped(CraftingInput input) {
        // 统计配方非空材料数
        long requiredCount = 0;
        for (boolean empty : emptySlots) {
            if (!empty) requiredCount++;
        }
        if (input.ingredientCount() != requiredCount) return false;

        // 计算输入网格的实际偏移，找到最小包围盒
        int inputMinRow = input.height(), inputMinCol = input.width();
        int inputMaxRow = -1, inputMaxCol = -1;
        for (int r = 0; r < input.height(); r++) {
            for (int c = 0; c < input.width(); c++) {
                if (!input.getItem(c, r).isEmpty()) {
                    inputMinRow = Math.min(inputMinRow, r);
                    inputMinCol = Math.min(inputMinCol, c);
                    inputMaxRow = Math.max(inputMaxRow, r);
                    inputMaxCol = Math.max(inputMaxCol, c);
                }
            }
        }
        if (inputMaxRow < 0) return requiredCount == 0;

        int inputH = inputMaxRow - inputMinRow + 1;
        int inputW = inputMaxCol - inputMinCol + 1;

        // 计算配方的最小包围盒
        int recipeMinRow = gridHeight, recipeMinCol = gridWidth;
        int recipeMaxRow = -1, recipeMaxCol = -1;
        for (int r = 0; r < gridHeight; r++) {
            for (int c = 0; c < gridWidth; c++) {
                if (!emptySlots[r * gridWidth + c]) {
                    recipeMinRow = Math.min(recipeMinRow, r);
                    recipeMinCol = Math.min(recipeMinCol, c);
                    recipeMaxRow = Math.max(recipeMaxRow, r);
                    recipeMaxCol = Math.max(recipeMaxCol, c);
                }
            }
        }

        int recipeH = recipeMaxRow - recipeMinRow + 1;
        int recipeW = recipeMaxCol - recipeMinCol + 1;

        if (inputH != recipeH || inputW != recipeW) return false;

        // 逐位置匹配
        for (int r = 0; r < recipeH; r++) {
            for (int c = 0; c < recipeW; c++) {
                int recipeIdx = (recipeMinRow + r) * gridWidth + (recipeMinCol + c);
                ItemStack actual = input.getItem(inputMinCol + c, inputMinRow + r);
                if (emptySlots[recipeIdx]) {
                    if (!actual.isEmpty()) return false;
                } else {
                    if (!gridIngredients[recipeIdx].test(actual)) return false;
                }
            }
        }
        return true;
    }

    private boolean matchesShapeless(CraftingInput input) {
        if (input.ingredientCount() != inputs.size()) return false;
        boolean[] used = new boolean[inputs.size()];
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            boolean matched = false;
            for (int j = 0; j < inputs.size(); j++) {
                if (!used[j] && inputs.get(j).test(stack)) {
                    used[j] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) { return getOutput(); }

    @Override
    public boolean isSpecial() { return !standardCrafting; }

    @Override
    public boolean showNotification() { return standardCrafting; }

    @Override
    public String group() { return ""; }

    @Override
    public CraftingBookCategory category() { return CraftingBookCategory.MISC; }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<net.minecraft.world.item.crafting.CraftingRecipe> getSerializer() {
        return (RecipeSerializer<net.minecraft.world.item.crafting.CraftingRecipe>)
            (RecipeSerializer<?>) serializerSupplier.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeType<net.minecraft.world.item.crafting.CraftingRecipe> getType() {
        return standardCrafting
            ? (RecipeType) RecipeType.CRAFTING
            : (RecipeType) ModRecipes.ERO_CRAFTING_TYPE.get();
    }

    // ── 配方书 / JEI ──

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.create(inputs); }

    @Override
    public List<RecipeDisplay> display() {
        net.minecraft.world.item.crafting.display.SlotDisplay result =
            new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(outputTemplate);
        net.minecraft.world.item.crafting.display.SlotDisplay station =
            net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE;

        if (shaped) {
            List<net.minecraft.world.item.crafting.display.SlotDisplay> displays = new ArrayList<>();
            for (int i = 0; i < gridWidth * gridHeight; i++) {
                if (emptySlots[i]) {
                    displays.add(net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE);
                } else {
                    displays.add(gridIngredients[i].display());
                }
            }
            return List.of(new net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay(
                gridWidth, gridHeight, displays, result, station));
        }
        List<net.minecraft.world.item.crafting.display.SlotDisplay> displays =
            inputs.stream().map(Ingredient::display).toList();
        return List.of(new net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay(
            displays, result, station));
    }
}
