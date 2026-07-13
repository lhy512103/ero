package recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public class MachineRecipeInput implements RecipeInput {

    private final ItemStack input;
    private final ItemStack input2;
    private final String machineType;

    public MachineRecipeInput(String machineType, ItemStack input) {
        this(machineType, input, ItemStack.EMPTY);
    }

    public MachineRecipeInput(String machineType, ItemStack input, ItemStack input2) {
        this.machineType = machineType;
        this.input = input;
        this.input2 = input2;
    }

    public String getMachineType() {
        return machineType;
    }

    public ItemStack getInput2() {
        return input2;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index == 0) return input;
        if (index == 1) return input2;
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty();
    }
}
