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

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import java.util.List;
import java.util.Optional;

public class MachineRecipe implements Recipe<MachineRecipeInput> {

    /** 附加产物（如洗矿副产粒），chance 为 0~1 的产出概率 */
    public record ExtraResult(ItemStackTemplate template, float chance) {
        public ItemStack create() {
            return template.create();
        }
    }

    private final String machineType;
    private final Ingredient input;
    private final Optional<Ingredient> input2;
    private final int inputCount;
    private final int input2Count;
    private final ItemStackTemplate outputTemplate;
    private final List<ExtraResult> extraResults;
    private final long energyCost;
    private final int processTime;
    /** Fluid consumed per operation; empty when the recipe needs none. */
    private final Optional<FluidStackTemplate> fluidInput;
    /** Fluid produced per operation; empty when the recipe makes none. */
    private final Optional<FluidStackTemplate> fluidOutput;
    /** Item that must be present but is not consumed (mould, culture, ...). */
    private final Optional<Ingredient> catalyst;

    public MachineRecipe(String machineType, Ingredient input, Optional<Ingredient> input2,
                         int inputCount, int input2Count,
                         ItemStackTemplate outputTemplate, List<ExtraResult> extraResults,
                         long energyCost, int processTime) {
        this(machineType, input, input2, inputCount, input2Count, outputTemplate, extraResults, energyCost, processTime,
            Optional.empty(), Optional.empty(), Optional.empty());
    }

    public MachineRecipe(String machineType, Ingredient input, Optional<Ingredient> input2,
                         int inputCount, int input2Count,
                         ItemStackTemplate outputTemplate, List<ExtraResult> extraResults,
                         long energyCost, int processTime,
                         Optional<FluidStackTemplate> fluidInput, Optional<FluidStackTemplate> fluidOutput, Optional<Ingredient> catalyst) {
        this.machineType = machineType;
        this.input = input;
        this.input2 = input2;
        this.inputCount = inputCount;
        this.input2Count = input2Count;
        this.outputTemplate = outputTemplate;
        this.extraResults = extraResults;
        this.energyCost = energyCost;
        this.processTime = processTime;
        this.fluidInput = fluidInput;
        this.fluidOutput = fluidOutput;
        this.catalyst = catalyst;
    }

    /**
     * Fluid consumed per operation; empty when none. Templates are used because recipes are
     * loaded before fluids can be turned into stacks.
     */
    public Optional<FluidStackTemplate> getFluidInputTemplate() { return fluidInput; }

    public Optional<FluidStackTemplate> getFluidOutputTemplate() { return fluidOutput; }

    /** A fresh stack of the consumed fluid, or an empty stack. Only call while a world is loaded. */
    public FluidStack getFluidInput() { return fluidInput.map(FluidStackTemplate::create).orElse(FluidStack.EMPTY); }

    /** A fresh stack of the produced fluid, or an empty stack. Only call while a world is loaded. */
    public FluidStack getFluidOutput() { return fluidOutput.map(FluidStackTemplate::create).orElse(FluidStack.EMPTY); }

    public Optional<Ingredient> getCatalyst() { return catalyst; }

    /**
     * Millibuckets of {@code fluid} one operation consumes: 0 when the recipe needs no fluid,
     * -1 when it needs a different fluid (so a machine with a {@code fluid} tank cannot run it).
     */
    public int fluidInputAmount(Fluid fluid) {
        if (fluidInput.isEmpty()) return 0;
        return fluidInput.get().fluid().value() == fluid ? fluidInput.get().amount() : -1;
    }

    public MachineRecipe(String machineType, Ingredient input, Optional<Ingredient> input2,
                         int inputCount, int input2Count,
                         ItemStackTemplate outputTemplate, long energyCost, int processTime) {
        this(machineType, input, input2, inputCount, input2Count, outputTemplate, List.of(), energyCost, processTime);
    }

    // 单输入构造器（向后兼容）
    public MachineRecipe(String machineType, Ingredient input, ItemStack output, long energyCost, int processTime) {
        this(machineType, input, Optional.empty(), 1, 1,
            new ItemStackTemplate(output.getItem(), output.getCount()), energyCost, processTime);
    }

    public MachineRecipe(String machineType, Ingredient input, ItemStackTemplate outputTemplate, long energyCost, int processTime) {
        this(machineType, input, Optional.empty(), 1, 1, outputTemplate, energyCost, processTime);
    }

    public List<ExtraResult> getExtraResults() {
        return extraResults;
    }

    public boolean hasExtraResults() {
        return !extraResults.isEmpty();
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

    /**
     * 每 tick 耗电量。配方 JSON 的 energy 字段表示「单次加工总耗电 (EU)」，
     * 机器加工时按 总耗电 / 加工时间 逐 tick 扣除（向上取整，至少 1 EU/t）。
     */
    public long getEnergyPerTick() {
        int time = Math.max(1, getProcessTime());
        return Math.max(1L, (energyCost + time - 1) / time);
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
