package recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class MachineRecipe implements Recipe<MachineRecipeInput> {

    private final String machineType;
    private final Ingredient input;
    private final Optional<Ingredient> input2;
    private final int inputCount;
    private final int input2Count;
    private final ItemStackTemplate outputTemplate;
    private final long energyCost;
    private final int processTime;

    public MachineRecipe(String machineType, Ingredient input, Optional<Ingredient> input2,
                         int inputCount, int input2Count,
                         ItemStackTemplate outputTemplate, long energyCost, int processTime) {
        this.machineType = machineType;
        this.input = input;
        this.input2 = input2;
        this.inputCount = inputCount;
        this.input2Count = input2Count;
        this.outputTemplate = outputTemplate;
        this.energyCost = energyCost;
        this.processTime = processTime;
    }

    // 单输入构造器（向后兼容）
    public MachineRecipe(String machineType, Ingredient input, ItemStack output, long energyCost, int processTime) {
        this(machineType, input, Optional.empty(), 1, 1,
            new ItemStackTemplate(output.getItem(), output.getCount()), energyCost, processTime);
    }

    public MachineRecipe(String machineType, Ingredient input, ItemStackTemplate outputTemplate, long energyCost, int processTime) {
        this(machineType, input, Optional.empty(), 1, 1, outputTemplate, energyCost, processTime);
    }

    public String getMachineType() {
        return machineType;
    }

    public Ingredient getInput() {
        return input;
    }

    public Optional<Ingredient> getInput2() {
        return input2;
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getInput2Count() {
        return input2Count;
    }

    public boolean hasDualInput() {
        return input2.isPresent();
    }

    public ItemStack getOutput() {
        return outputTemplate.create();
    }

    public ItemStackTemplate getOutputTemplate() {
        return outputTemplate;
    }

    public long getEnergyCost() {
        return energyCost;
    }

    public int getProcessTime() {
        return processTime;
    }

    public boolean matchesMachine(String type) {
        return this.machineType.equals(type);
    }

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        if (!matchesMachine(input.getMachineType())) return false;
        if (!this.input.test(input.getItem(0))) return false;
        if (input2.isPresent()) {
            return this.input2.get().test(input.getItem(1));
        }
        return true;
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input) {
        return getOutput();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<MachineRecipeInput>> getSerializer() {
        return ModRecipes.MACHINE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<MachineRecipeInput>> getType() {
        return ModRecipes.MACHINE_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(input);
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return new RecipeBookCategory();
    }
}
