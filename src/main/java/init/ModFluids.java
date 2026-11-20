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

import java.util.ArrayList;
import java.util.List;

/**
 * 流体注册类。
 * <p>
 * 冷却液之外，化工流体集中登记在 {@link CHEMICALS} 表里：一项给出 id、颜色和物性，注册时同时得到
 * 静止/流动两种流体和各自的 {@link FluidType}。化工流体没有放置方块，也没有桶，只存在于机器与管道
 * 里，和 IC2/GT 的化工流体一致。颜色只用于界面里的储罐和客户端渲染的着色，不进入物性。
 * </p>
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

    // ------------------------------------------------------------------ 化工流体

    /**
     * 一种化工流体。
     *
     * @param id      注册名，静止流体叫这个名字，流动流体叫 {@code flowing_<id>}
     * @param color   ARGB 颜色，用于界面储罐与客户端着色
     * @param gas     气体（密度为负，管道里的行为与液体一致）
     * @param density 密度，液体默认 1000，气体默认 -10
     * @param boiling 温度，默认 300 K；气体在界面里显示为常温
     */
    public record Chemical(String id, int color, boolean gas, int density, int boiling) {}

    private static Chemical liquid(String id, int color) {
        return new Chemical(id, color, false, 1000, 300);
    }

    private static Chemical gas(String id, int color) {
        return new Chemical(id, color, true, -10, 300);
    }

    /** M1 的全部化工流体，顺序固定，生成的注册名也按这个顺序。 */
    public static final List<Chemical> CHEMICALS = List.of(
        liquid("starch_slurry", 0xFFD8D2C0),
        liquid("liquefied_mash", 0xFFC9A86A),
        liquid("sugar_solution", 0xFFE8D08A),
        liquid("fermented_wash", 0xFF9A7B4F),
        liquid("lactic_broth", 0xFFE4E0B8),
        liquid("ethanol", 0xFFE9F0F2),
        liquid("stillage", 0xFF5E4426),
        gas("ethylene", 0xFFDCE6B8),
        gas("carbon_dioxide", 0xFFB9BCC2),
        liquid("process_water", 0xFF7FB3D5),
        liquid("lactic_acid", 0xFFE3E6BF),
        liquid("pla_oligomer", 0xFFE3CF98),
        liquid("crude_lactide", 0xFFC6AA74),
        liquid("lactide_mother_liquor", 0xFFAAAB81),
        liquid("pla_melt_injection", 0xFFD8BD9E),
        liquid("pla_melt_film", 0xFFADD7C9));

    private static final List<DeferredHolder<FluidType, FluidType>> TYPES = new ArrayList<>();
    private static final List<DeferredHolder<Fluid, ? extends FlowingFluid>> STILL = new ArrayList<>();
    private static final List<DeferredHolder<Fluid, ? extends FlowingFluid>> FLOWING = new ArrayList<>();

    static {
        for (int offset = 0; offset < CHEMICALS.size(); offset++) {
            int index = offset;
            Chemical chemical = CHEMICALS.get(index);
            String id = chemical.id();
            TYPES.add(FLUID_TYPES.register(id, () -> new FluidType(
                FluidType.Properties.create()
                    .density(chemical.gas() ? -10 : 1000)
                    .viscosity(chemical.gas() ? 200 : 1000)
                    .temperature(300)
                    .canConvertToSource(false))));
            STILL.add(FLUIDS.register(id,
                () -> new BaseFlowingFluid.Source(properties(index))));
            FLOWING.add(FLUIDS.register("flowing_" + id,
                () -> new BaseFlowingFluid.Flowing(properties(index))));
        }
    }

    /** 没有方块也没有桶的流体：只用于机器与管道。 */
    private static BaseFlowingFluid.Properties properties(int index) {
        return new BaseFlowingFluid.Properties(TYPES.get(index)::value, STILL.get(index)::value, FLOWING.get(index)::value)
            .tickRate(20);
    }

    private static int indexOf(String id) {
        for (int i = 0; i < CHEMICALS.size(); i++) {
            if (CHEMICALS.get(i).id().equals(id)) return i;
        }
        throw new IllegalArgumentException("unknown chemical " + id);
    }

    /** 化工流体的静止流体。 */
    public static Fluid chemical(String id) {
        return STILL.get(indexOf(id)).get();
    }

    /** 化工流体的流动流体。 */
    public static Fluid flowingChemical(String id) {
        return FLOWING.get(indexOf(id)).get();
    }

    /** 化工流体的颜色；不是化工流体时返回 0。 */
    public static int chemicalColor(Fluid fluid) {
        for (int i = 0; i < CHEMICALS.size(); i++) {
            if (fluid.isSame(STILL.get(i).get()) || fluid.isSame(FLOWING.get(i).get())) return CHEMICALS.get(i).color();
        }
        return 0;
    }

    public static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
    }
}
