package compat.jei;

import ero.Ero;
import block.ModBlocks;
import machine.electric_furnace.screen.ElectricFurnaceScreen;
import machine.induction_furnace.screen.InductionFurnaceScreen;
import machine.macerator.screen.MaceratorScreen;
import machine.planter.screen.PlanterScreen;
import machine.compressor.screen.CompressorScreen;
import machine.extractor.screen.ExtractorScreen;
import machine.electrolyzer.screen.ElectrolyzerScreen;
import machine.ore_washer.screen.OreWasherScreen;
import machine.workbench.screen.IndustrialWorkbenchScreen;
import recipe.CraftingRecipe;
import recipe.MachineRecipe;
import recipe.ModRecipes;
import recipe.OptionalCraftingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@JeiPlugin
public class EroJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(EroJeiPlugin.class);

    /** 每台机器的独立 JEI 配方类型 */
    private static final Map<String, MachineTypeEntry> MACHINE_TYPES = new LinkedHashMap<>();

    static {
        registerMachineType("macerator", "打粉机", ModBlocks.MACERATOR);
        registerMachineType("compressor", "压缩机", ModBlocks.COMPRESSOR);
        registerMachineType("metal_former_rolling", "金属成型机 · 辊压", ModBlocks.METAL_FORMER);
        registerMachineType("metal_former_cutting", "金属成型机 · 切割", ModBlocks.METAL_FORMER);
        registerMachineType("metal_former_extruding", "金属成型机 · 挤压", ModBlocks.METAL_FORMER);
        registerMachineType("extractor", "提取机", ModBlocks.EXTRACTOR);
        registerMachineType("electric_furnace", "电炉", ModBlocks.ELECTRIC_FURNACE);
        registerMachineType("electrolyzer", "电解机", ModBlocks.ELECTROLYZER);
        registerMachineType("ore_washer", "洗矿机", ModBlocks.ORE_WASHER);
        registerMachineType("alloy_furnace", "合金炉", ModBlocks.INDUCTION_FURNACE);
        registerMachineType("planter", "种植机", ModBlocks.PLANTER);
    }

    // 工业合成配方类型
    public static final Supplier<IRecipeHolderType<CraftingRecipe>> CRAFTING_RECIPE_TYPE =
        IRecipeHolderType.createDeferred(ModRecipes.ERO_CRAFTING_TYPE);

    // 可选合成配方类型
    public static final Supplier<IRecipeHolderType<OptionalCraftingRecipe>> OPTIONAL_CRAFTING_RECIPE_TYPE =
        IRecipeHolderType.createDeferred(ModRecipes.OPTIONAL_CRAFTING_TYPE);

    /** 缓存的 JEI 运行时实例 */
    private static IJeiRuntime jeiRuntime;

    @SuppressWarnings("unchecked")
    private static void registerMachineType(String machineType, String displayName, Supplier<? extends net.minecraft.world.level.ItemLike> iconBlock) {
        IRecipeType<RecipeHolder<MachineRecipe>> recipeType = IRecipeType.create(
            Identifier.parse(Ero.MOD_ID + ":machine_" + machineType),
            (Class<? extends RecipeHolder<MachineRecipe>>) (Class<?>) RecipeHolder.class
        );
        MACHINE_TYPES.put(machineType, new MachineTypeEntry(recipeType, displayName, iconBlock));
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.parse(Ero.MOD_ID + ":jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (Map.Entry<String, MachineTypeEntry> entry : MACHINE_TYPES.entrySet()) {
            MachineTypeEntry mt = entry.getValue();
            registration.addRecipeCategories(
                new MachineRecipeCategory(registration.getJeiHelpers(), mt.recipeType, mt.displayName, mt.iconBlock.get())
            );
        }

        // 合成分类放在最后注册，addRecipeCategories 每次调用会覆盖之前的结果
        registration.addRecipeCategories(
            new EroCraftingRecipeCategory(registration.getJeiHelpers(), CRAFTING_RECIPE_TYPE.get(), "工业工作台", ModBlocks.INDUSTRIAL_WORKBENCH.get()),
            new OptionalCraftingRecipeCategory(registration.getJeiHelpers(), OPTIONAL_CRAFTING_RECIPE_TYPE.get(), "可选合成", ModBlocks.INDUSTRIAL_WORKBENCH.get())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // MC 26.1 客户端的 level.recipeAccess() 返回 ClientRecipeContainer（不是 RecipeManager），
        // 因此需要从集成服务器获取 RecipeManager
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.server.MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            LOGGER.warn("[Ero JEI] 无法获取集成服务器，跳过配方注册");
            return;
        }

        net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();

        // 获取所有机器配方
        Collection<RecipeHolder<MachineRecipe>> allMachineRecipes = recipeManager.recipeMap().byType(ModRecipes.MACHINE_TYPE.get());
        LOGGER.info("[Ero JEI] 找到 {} 个机器配方", allMachineRecipes.size());

        // 注册机器配方到对应的JEI类型
        for (Map.Entry<String, MachineTypeEntry> entry : MACHINE_TYPES.entrySet()) {
            String machineType = entry.getKey();
            MachineTypeEntry mt = entry.getValue();

            List<RecipeHolder<MachineRecipe>> recipes = allMachineRecipes.stream()
                .filter(holder -> holder.value().matchesMachine(machineType))
                .toList();

            if (machineType.equals("planter")) recipes = withAutomaticCrops(server, recipes);

            LOGGER.info("[Ero JEI] 机器类型 '{}': {} 个配方", machineType, recipes.size());
            registration.addRecipes(mt.recipeType, recipes);
        }

        // 注册工业合成配方
        List<RecipeHolder<CraftingRecipe>> craftingRecipes = List.copyOf(recipeManager.recipeMap().byType(ModRecipes.ERO_CRAFTING_TYPE.get()));
        LOGGER.info("[Ero JEI] 工业合成配方: {} 个", craftingRecipes.size());
        registration.addRecipes(CRAFTING_RECIPE_TYPE.get(), craftingRecipes);

        // 注册可选合成配方
        List<RecipeHolder<OptionalCraftingRecipe>> optionalRecipes = List.copyOf(recipeManager.recipeMap().byType(ModRecipes.OPTIONAL_CRAFTING_TYPE.get()));
        LOGGER.info("[Ero JEI] 可选合成配方: {} 个", optionalRecipes.size());
        registration.addRecipes(OPTIONAL_CRAFTING_RECIPE_TYPE.get(), optionalRecipes);
    }

    /**
     * Adds every crop the planter recognises automatically (vanilla and modded) to its JEI page,
     * skipping seeds that already have a data recipe. The crop list is computed on the server thread
     * because it queries block loot tables.
     */
    private static List<RecipeHolder<MachineRecipe>> withAutomaticCrops(net.minecraft.server.MinecraftServer server,
                                                                       List<RecipeHolder<MachineRecipe>> dataRecipes) {
        List<RecipeHolder<MachineRecipe>> all = new java.util.ArrayList<>(dataRecipes);
        try {
            var level = server.overworld();
            List<machine.planter.CropRegistry.Entry> crops = server.submit(
                () -> machine.planter.CropRegistry.listAll(level, net.minecraft.core.BlockPos.ZERO)).join();
            int added = 0;
            for (machine.planter.CropRegistry.Entry crop : crops) {
                net.minecraft.world.item.ItemStack seed = crop.seed();
                if (dataRecipes.stream().anyMatch(holder -> holder.value().getInput().test(seed))) continue;
                var profile = crop.profile();
                Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(seed.getItem());
                Identifier id = Identifier.fromNamespaceAndPath(Ero.MOD_ID, "planter_auto/" + itemId.getNamespace() + "/" + itemId.getPath());
                MachineRecipe recipe = new MachineRecipe("planter", net.minecraft.world.item.crafting.Ingredient.of(seed.getItem()),
                    profile.result(), profile.energyPerTick() * profile.ticks(), profile.ticks());
                all.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), recipe));
                added++;
            }
            LOGGER.info("[Ero JEI] 种植机自动识别作物: {} 种", added);
        } catch (RuntimeException error) {
            LOGGER.warn("[Ero JEI] 无法列出种植机自动识别的作物", error);
        }
        return all;
    }

    /** Opens the JEI recipe view for an item (used by the built-in guide). */
    public static boolean showRecipes(net.minecraft.world.item.ItemStack stack, boolean uses) {
        if (jeiRuntime == null || stack.isEmpty()) return false;
        var focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(
            uses ? mezz.jei.api.recipe.RecipeIngredientRole.INPUT : mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
            mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack);
        jeiRuntime.getRecipesGui().show(focus);
        return true;
    }

    public static boolean searchHasFocus() {
        return jeiRuntime != null && jeiRuntime.getIngredientListOverlay().hasKeyboardFocus();
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime rt) {
        jeiRuntime = rt;

        // 强制 JEI 识别到 Minecraft 已显示模组名，避免 JEI 为物品额外添加重复的模组名行
        forceModNameOverride(rt);
    }

    /**
     * NeoForge 1.21.2+ 仅对模组物品显示模组名（原版物品如苹果不显示），
     * 而 JEI 的 {@code detectModNameTooltipFormatting()} 通过检测苹果 tooltip 是否包含
     * "Minecraft" 来判断 Minecraft 是否已显示模组名。检测失败后 {@code cachedOverride} 被缓存为空，
     * 导致 {@code isModNameFormatOverrideActive()} 返回 false，JEI 会为物品额外添加一次模组名，
     * 与 Minecraft/NeoForge 已添加的模组名重复。
     * <p>
     * 这里通过反射将 {@code ModIdFormatConfig.cachedOverride} 设置为非空格式字符串，
     * 使 {@code isModNameFormatOverrideActive()} 返回 true，从而让 JEI 跳过为物品添加模组名。
     */
    private void forceModNameOverride(IJeiRuntime rt) {
        try {
            IModIdHelper modIdHelper = rt.getJeiHelpers().getModIdHelper();
            // ModIdHelper.modIdFormattingConfig (private final)
            Field configField = modIdHelper.getClass().getDeclaredField("modIdFormattingConfig");
            configField.setAccessible(true);
            Object config = configField.get(modIdHelper);
            // ModIdFormatConfig.cachedOverride (private)
            Field overrideField = config.getClass().getDeclaredField("cachedOverride");
            overrideField.setAccessible(true);
            // §9§o = 蓝色斜体，匹配 JEI 默认 modNameFormat；%MODNAME% 占位符会被替换为实际模组名
            overrideField.set(config, "§9§o%MODNAME%");
        } catch (Exception e) {
            LOGGER.error("[Ero JEI] 强制设置模组名覆盖失败，JEI 中可能仍会重复显示模组名", e);
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Map.Entry<String, MachineTypeEntry> entry : MACHINE_TYPES.entrySet()) {
            MachineTypeEntry mt = entry.getValue();
            registration.addCraftingStation(mt.recipeType, mt.iconBlock.get());
        }

        // 工业合成
        registration.addCraftingStation(CRAFTING_RECIPE_TYPE.get(), ModBlocks.INDUSTRIAL_WORKBENCH.get());
        registration.addCraftingStation(mezz.jei.api.constants.RecipeTypes.CRAFTING, ModBlocks.INDUSTRIAL_WORKBENCH.get());
        registration.addCraftingStation(mezz.jei.api.constants.RecipeTypes.SMELTING, ModBlocks.ELECTRIC_FURNACE.get());
        registration.addCraftingStation(mezz.jei.api.constants.RecipeTypes.BLASTING, ModBlocks.ELECTRIC_FURNACE.get());
        registration.addCraftingStation(mezz.jei.api.constants.RecipeTypes.SMOKING, ModBlocks.ELECTRIC_FURNACE.get());

        // 可选合成
        registration.addCraftingStation(OPTIONAL_CRAFTING_RECIPE_TYPE.get(), ModBlocks.INDUSTRIAL_WORKBENCH.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 打粉机 — 进度箭头 (79, 36, 16x22)
        registration.addRecipeClickArea(MaceratorScreen.class,
            79, 36, 16, 22,
            MACHINE_TYPES.get("macerator").recipeType);

        // 电炉 — 进度箭头 (79, 36, 16x22)
        registration.addRecipeClickArea(ElectricFurnaceScreen.class,
            79, 36, 16, 22,
            MACHINE_TYPES.get("electric_furnace").recipeType,
            mezz.jei.api.constants.RecipeTypes.SMELTING,
            mezz.jei.api.constants.RecipeTypes.BLASTING,
            mezz.jei.api.constants.RecipeTypes.SMOKING);

        // 合金炉 — 进度箭头 (80, 36, 16x22)
        registration.addRecipeClickArea(InductionFurnaceScreen.class,
            80, 36, 16, 22,
            MACHINE_TYPES.get("alloy_furnace").recipeType);

        // 种植机 — 能量进度条 (63, 35, 13x20)
        registration.addRecipeClickArea(PlanterScreen.class,
            63, 35, 13, 20,
            MACHINE_TYPES.get("planter").recipeType);

        // 压缩机 — 进度箭头 (79, 36, 16x22)
        registration.addRecipeClickArea(CompressorScreen.class,
            79, 36, 16, 22,
            MACHINE_TYPES.get("compressor").recipeType);

        // 金属成型机 — 进度箭头 (79, 36, 16x22)，三种模式的配方一起显示
        registration.addRecipeClickArea(machine.metal_former.screen.MetalFormerScreen.class,
            79, 36, 16, 22,
            MACHINE_TYPES.get("metal_former_rolling").recipeType,
            MACHINE_TYPES.get("metal_former_cutting").recipeType,
            MACHINE_TYPES.get("metal_former_extruding").recipeType);

        // 提取机 — 进度箭头 (79, 36, 16x22)
        registration.addRecipeClickArea(ExtractorScreen.class,
            79, 36, 16, 22,
            MACHINE_TYPES.get("extractor").recipeType);

        // 电解机 — 进度箭头 (80, 38, 52x17)
        registration.addRecipeClickArea(ElectrolyzerScreen.class,
            80, 38, 52, 17,
            MACHINE_TYPES.get("electrolyzer").recipeType);

        // 洗矿机 — 进度条 (74, 39, 16x17)
        registration.addRecipeClickArea(OreWasherScreen.class,
            74, 39, 16, 17,
            MACHINE_TYPES.get("ore_washer").recipeType);

        // 工业工作台 — 箭头 (90, 32, 22x15)
        registration.addRecipeClickArea(IndustrialWorkbenchScreen.class,
            90, 32, 22, 15,
            CRAFTING_RECIPE_TYPE.get(), mezz.jei.api.constants.RecipeTypes.CRAFTING);
    }

    /** 机器类型条目 */
    private record MachineTypeEntry(
        IRecipeType<RecipeHolder<MachineRecipe>> recipeType,
        String displayName,
        Supplier<? extends net.minecraft.world.level.ItemLike> iconBlock
    ) {}
}
