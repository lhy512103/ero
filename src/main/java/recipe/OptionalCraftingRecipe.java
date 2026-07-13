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
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 可选合成配方 - 适配工业工作台。
 * 必选材料必须全部存在，可选材料可以存在或不存在。
 */
public class OptionalCraftingRecipe implements net.minecraft.world.item.crafting.CraftingRecipe {

    private final List<Ingredient> requiredInputs;
    private final List<Ingredient> optionalInputs;
    private final ItemStackTemplate outputTemplate;

    public OptionalCraftingRecipe(List<Ingredient> requiredInputs, List<Ingredient> optionalInputs,
                                  ItemStackTemplate outputTemplate) {
        this.requiredInputs = requiredInputs;
        this.optionalInputs = optionalInputs;
        this.outputTemplate = outputTemplate;
    }

    // ── 查询 ──

    public List<Ingredient> getRequiredInputs() { return requiredInputs; }
    public List<Ingredient> getOptionalInputs() { return optionalInputs; }
    public ItemStack getOutput() { return outputTemplate.create(); }
    public ItemStackTemplate getOutputTemplate() { return outputTemplate; }

    /** 所有输入（必选 + 可选），用于 JEI 显示 */
    public List<Ingredient> getAllInputs() {
        var all = new ArrayList<>(requiredInputs);
        all.addAll(optionalInputs);
        return all;
    }

    // ── Recipe 接口 ──

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean[] used = new boolean[input.size()];
        boolean[] requiredMatched = new boolean[requiredInputs.size()];

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            boolean matched = false;
            // 先匹配必选
            for (int j = 0; j < requiredInputs.size(); j++) {
                if (!requiredMatched[j] && requiredInputs.get(j).test(stack)) {
                    requiredMatched[j] = used[i] = true;
                    matched = true;
                    break;
                }
            }
            // 再匹配可选
            if (!matched) {
                for (Ingredient optional : optionalInputs) {
                    if (optional.test(stack)) {
                        used[i] = true;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) return false;
        }

        for (boolean b : requiredMatched) {
            if (!b) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) { return getOutput(); }

    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    public String group() { return ""; }

    @Override
    public CraftingBookCategory category() { return CraftingBookCategory.MISC; }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<net.minecraft.world.item.crafting.CraftingRecipe> getSerializer() {
        return (RecipeSerializer<net.minecraft.world.item.crafting.CraftingRecipe>)
            (RecipeSerializer<?>) ModRecipes.OPTIONAL_CRAFTING_SERIALIZER.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeType<net.minecraft.world.item.crafting.CraftingRecipe> getType() {
        return (RecipeType) ModRecipes.OPTIONAL_CRAFTING_TYPE.get();
    }

    // ── 配方书 / JEI ──

    @Override
    public PlacementInfo placementInfo() { return PlacementInfo.create(getAllInputs()); }

    @Override
    public List<RecipeDisplay> display() {
        var displays = getAllInputs().stream()
            .map(Ingredient::display)
            .toList();
        var result = new SlotDisplay.ItemStackSlotDisplay(outputTemplate);
        var station = SlotDisplay.Empty.INSTANCE;
        return List.of(new ShapelessCraftingRecipeDisplay(displays, result, station));
    }
}
