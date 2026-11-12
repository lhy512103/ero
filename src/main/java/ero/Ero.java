package ero;

import energy.EUCapabilityRegistry;
import init.ModFluids;
import init.ModRegistries;
import network.MachineNetwork;
import network.performance.PerformanceGovernor;
import network.performance.PerformanceMonitor;
import recipe.ModRecipes;
import recipe.cache.MachineRecipeCache;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(Ero.MOD_ID)
public class Ero {
    public static final String MOD_ID = "iuce";

    private static MachineNetwork machineNetwork;

    public Ero(ModContainer container) {
        IEventBus modEventBus = container.getEventBus();
        ModRegistries.registerAll(modEventBus);
        if (Boolean.getBoolean("iuce.regressionTests")) {
            try {
                Class.forName("regression.RegressionTests").getMethod("register", IEventBus.class).invoke(null, modEventBus);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Regression test source set is missing", error);
            }
        }

        // 注册流体
        ModFluids.register(modEventBus);

        // 注册能量能力
        modEventBus.addListener(EUCapabilityRegistry::registerAll);

        // 初始化机器网络
        machineNetwork = new MachineNetwork();

        // 初始化性能监控系统
        PerformanceMonitor.getInstance().register();
        PerformanceGovernor.getInstance().initialize();

        // 注册世界tick事件
        NeoForge.EVENT_BUS.addListener(Ero::onLevelTick);

        // 注册数据包同步事件 - 请求将自定义配方类型同步到客户端
        NeoForge.EVENT_BUS.addListener(Ero::onDatapackSync);

        // 首次进入世界时赠送一本指南
        NeoForge.EVENT_BUS.addListener(Ero::onPlayerLogin);
    }

    public static MachineNetwork getMachineNetwork() {
        return machineNetwork;
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        // 服务器冻结时（无玩家/手动降速）跳过网络能量传输处理，
        // 避免在 tick 被故意放缓时做无意义的网络计算
        if (!event.getLevel().tickRateManager().runsNormally()) {
            return;
        }

        if (machineNetwork != null) {
            machineNetwork.tick(event.getLevel());
        }
    }

    private static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        var data = player.getPersistentData();
        var persisted = data.getCompoundOrEmpty(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (persisted.getBooleanOr("iuce_guide_given", false)) return;
        persisted.putBoolean("iuce_guide_given", true);
        data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
        player.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(item.ModItems.GUIDE.get()));
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // 请求将自定义配方类型同步到客户端
        event.sendRecipes(ModRecipes.MACHINE_TYPE.get());
        event.sendRecipes(ModRecipes.ERO_CRAFTING_TYPE.get());
        event.sendRecipes(ModRecipes.STANDARD_CRAFTING_TYPE.get());
        event.sendRecipes(ModRecipes.OPTIONAL_CRAFTING_TYPE.get());
    }
}
