package client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import recipe.CraftingRecipe;
import recipe.MachineRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only index of the recipes the guide can display. It is rebuilt lazily whenever the
 * underlying recipe map changes (world join, datapack reload), so pages always show live data.
 */
final class GuideRecipes {
    private GuideRecipes() {}

    enum Kind { MACHINE, CRAFT, SMELT }

    record Extra(ItemStack stack, float chance) {}

    /**
     * @param gridW for CRAFT: 3 when {@code inputs} is a row-major 3x3 grid, 0 for shapeless
     */
    record View(Kind kind, String machineType, ItemStack station, boolean workbenchOnly, int gridW,
                List<List<ItemStack>> inputs, ItemStack output, List<Extra> extras, long energy, int time, String id) {
        Component stationName() {
            if (kind == Kind.MACHINE) return Component.translatable("guide.iuce.machine." + machineType);
            return station.getHoverName();
        }
    }

    private static final Map<String, String> MACHINE_BLOCKS = Map.ofEntries(
        Map.entry("macerator", "macerator"), Map.entry("compressor", "compressor"),
        Map.entry("extractor", "extractor"), Map.entry("electric_furnace", "electric_furnace"),
        Map.entry("electrolyzer", "electrolyzer"), Map.entry("ore_washer", "ore_washer"),
        Map.entry("alloy_furnace", "induction_furnace"), Map.entry("planter", "planter"),
        Map.entry("metal_former_rolling", "metal_former"), Map.entry("metal_former_cutting", "metal_former"),
        Map.entry("metal_former_extruding", "metal_former"));

    @Nullable private static RecipeMap synced;
    @Nullable private static Object builtFrom;
    private static int version;
    private static Map<Item, List<View>> byOutput = Map.of();
    private static Map<Item, List<View>> byInput = Map.of();
    private static Map<String, List<View>> byMachine = Map.of();

    static void onRecipesReceived(RecipesReceivedEvent event) {
        synced = event.getRecipeMap();
    }

    static void clear() {
        synced = null;
        builtFrom = null;
        byOutput = Map.of();
        byInput = Map.of();
        byMachine = Map.of();
        version++;
    }

    /** Changes whenever the index is rebuilt; blocks use it to know when to re-resolve. */
    static int version() {
        refresh();
        return version;
    }

    static List<View> producing(Item item) {
        refresh();
        return byOutput.getOrDefault(item, List.of());
    }

    static List<View> using(Item item) {
        refresh();
        return byInput.getOrDefault(item, List.of());
    }

    static List<View> ofMachine(String machineType) {
        refresh();
        return byMachine.getOrDefault(machineType, List.of());
    }

    @Nullable
    static RecipeMap current() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) return server.getRecipeManager().recipeMap();
        return synced;
    }

    private static List<ItemStack> options(Ingredient ingredient, int count) {
        return ingredient.items().map(holder -> new ItemStack(holder, count)).toList();
    }

    private static ItemStack block(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("iuce", path)));
    }

    private static void refresh() {
        RecipeMap map = current();
        if (map == builtFrom) return;
        builtFrom = map;
        version++;
        Map<Item, List<View>> outputs = new HashMap<>();
        Map<Item, List<View>> inputs = new HashMap<>();
        Map<String, List<View>> machines = new HashMap<>();
        if (map != null) {
            List<RecipeHolder<?>> holders = new ArrayList<>(map.values());
            holders.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));
            for (RecipeHolder<?> holder : holders) {
                View view = view(holder);
                if (view == null || view.output.isEmpty()) continue;
                outputs.computeIfAbsent(view.output.getItem(), k -> new ArrayList<>()).add(view);
                for (Extra extra : view.extras) outputs.computeIfAbsent(extra.stack.getItem(), k -> new ArrayList<>()).add(view);
                Set<Item> seen = new LinkedHashSet<>();
                for (List<ItemStack> slot : view.inputs) for (ItemStack stack : slot) seen.add(stack.getItem());
                for (Item item : seen) inputs.computeIfAbsent(item, k -> new ArrayList<>()).add(view);
                if (view.kind == Kind.MACHINE) machines.computeIfAbsent(view.machineType, k -> new ArrayList<>()).add(view);
            }
        }
        byOutput = outputs;
        byInput = inputs;
        byMachine = machines;
    }

    @Nullable
    private static View view(RecipeHolder<?> holder) {
        String id = holder.id().identifier().toString();
        if (holder.value() instanceof MachineRecipe recipe) {
            List<List<ItemStack>> in = new ArrayList<>();
            in.add(options(recipe.getInput(), recipe.getInputCount()));
            recipe.getInput2().ifPresent(second -> in.add(options(second, recipe.getInput2Count())));
            List<Extra> extras = new ArrayList<>();
            for (MachineRecipe.ExtraResult extra : recipe.getExtraResults()) extras.add(new Extra(extra.create(), extra.chance()));
            String type = recipe.getMachineType();
            String blockPath = MACHINE_BLOCKS.get(type);
            return new View(Kind.MACHINE, type, blockPath == null ? ItemStack.EMPTY : block(blockPath), false, 0,
                in, recipe.getOutput(), extras, recipe.getEnergyCost(), recipe.getProcessTime(), id);
        }
        if (holder.value() instanceof CraftingRecipe recipe) {
            List<List<ItemStack>> in = new ArrayList<>();
            int gridW = 0;
            if (recipe.isShaped()) {
                gridW = 3;
                for (int i = 0; i < 9; i++) in.add(List.of());
                boolean[] empty = recipe.getEmptySlots();
                int next = 0;
                for (int row = 0; row < recipe.getGridHeight(); row++) {
                    for (int col = 0; col < recipe.getGridWidth(); col++) {
                        if (empty[row * recipe.getGridWidth() + col]) continue;
                        Ingredient ingredient = recipe.getInputs().get(next++);
                        if (row < 3 && col < 3) in.set(row * 3 + col, options(ingredient, 1));
                    }
                }
            } else {
                for (Ingredient ingredient : recipe.getInputs()) in.add(options(ingredient, 1));
            }
            boolean workbench = !recipe.isStandardCrafting();
            return new View(Kind.CRAFT, "", workbench ? block("industrial_workbench") : new ItemStack(Items.CRAFTING_TABLE),
                workbench, gridW, in, recipe.getOutput(), List.of(), 0, 0, id);
        }
        if (holder.value() instanceof AbstractCookingRecipe recipe && recipe.getType() == RecipeType.SMELTING) {
            ItemStack output = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
            List<ItemStack> in = options(recipe.input(), 1);
            boolean ours = "iuce".equals(BuiltInRegistries.ITEM.getKey(output.getItem()).getNamespace())
                || in.stream().anyMatch(stack -> "iuce".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()));
            if (!ours) return null;
            return new View(Kind.SMELT, "", new ItemStack(Items.FURNACE), false, 0, List.of(in), output, List.of(),
                0, recipe.cookingTime(), id);
        }
        return null;
    }
}
