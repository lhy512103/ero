package init;

import ero.Ero;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 流体注册类
 */
public class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Ero.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Ero.MOD_ID);

    public static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
    }
}
