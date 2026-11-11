package init;

import ero.Ero;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 流体注册类
 */
public class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Ero.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Ero.MOD_ID);

    // 冷却液（比水更稠、不可无限复制）
    public static final DeferredHolder<FluidType, FluidType> COOLANT_TYPE = FLUID_TYPES.register("coolant",
        () -> new FluidType(FluidType.Properties.create()
            .density(1200)
            .viscosity(1500)
            .canConvertToSource(false)));

    public static final DeferredHolder<Fluid, FlowingFluid> COOLANT = FLUIDS.register("coolant",
        () -> new BaseFlowingFluid.Source(coolantProperties()));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_COOLANT = FLUIDS.register("flowing_coolant",
        () -> new BaseFlowingFluid.Flowing(coolantProperties()));

    private static BaseFlowingFluid.Properties coolantProperties() {
        return new BaseFlowingFluid.Properties(COOLANT_TYPE::value, COOLANT::value, FLOWING_COOLANT::value)
            .bucket(() -> item.ModItems.COOLANT_BUCKET.get())
            .block(() -> block.ModBlocks.COOLANT_BLOCK.get())
            .tickRate(15);
    }

    public static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
    }
}
