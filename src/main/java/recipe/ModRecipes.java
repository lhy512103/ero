package recipe;

import ero.Ero;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, Ero.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Ero.MOD_ID);

    // === 机器配方类型 ===
    public static final Supplier<RecipeType<MachineRecipe>> MACHINE_TYPE =
        RECIPE_TYPES.register("machine", () -> RecipeType.simple(net.minecraft.resources.Identifier.parse("ero:machine")));

    public static final Supplier<RecipeSerializer<MachineRecipe>> MACHINE_SERIALIZER =
        RECIPE_SERIALIZERS.register("machine", MachineRecipeSerializer::create);

    // === 工业合成配方类型 ===
    public static final Supplier<RecipeType<CraftingRecipe>> ERO_CRAFTING_TYPE =
        RECIPE_TYPES.register("crafting", () -> RecipeType.simple(net.minecraft.resources.Identifier.parse("ero:crafting")));

    public static final Supplier<RecipeSerializer<CraftingRecipe>> ERO_CRAFTING_SERIALIZER =
        RECIPE_SERIALIZERS.register("crafting", () -> CraftingRecipeSerializer.forType(ERO_CRAFTING_TYPE.get()).create());

    // === 普通合成配方类型 ===
    public static final Supplier<RecipeType<CraftingRecipe>> STANDARD_CRAFTING_TYPE =
        RECIPE_TYPES.register("standard_crafting", () -> RecipeType.simple(net.minecraft.resources.Identifier.parse("ero:standard_crafting")));

    public static final Supplier<RecipeSerializer<CraftingRecipe>> STANDARD_CRAFTING_SERIALIZER =
        RECIPE_SERIALIZERS.register("standard_crafting", () -> CraftingRecipeSerializer.forType(STANDARD_CRAFTING_TYPE.get()).create());

    // === 可选合成配方类型（适配工业工作台）===
    public static final Supplier<RecipeType<OptionalCraftingRecipe>> OPTIONAL_CRAFTING_TYPE =
        RECIPE_TYPES.register("optional_crafting", () -> RecipeType.simple(net.minecraft.resources.Identifier.parse("ero:optional_crafting")));

    public static final Supplier<RecipeSerializer<OptionalCraftingRecipe>> OPTIONAL_CRAFTING_SERIALIZER =
        RECIPE_SERIALIZERS.register("optional_crafting", () -> OptionalCraftingRecipeSerializer.forType(OPTIONAL_CRAFTING_TYPE.get()).create());

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
