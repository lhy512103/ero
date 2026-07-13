package init;
import block.ModBlockItems;
import block.ModBlocks;
import blockentity.ModBlockEntities;
import item.ModItems;
import recipe.ModRecipes;
import net.neoforged.bus.api.IEventBus;

public class ModRegistries {
    public static void registerAll(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockItems.BLOCK_ITEMS.register(modEventBus);
        ModCreativeModeTabs.TABS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
    }
}