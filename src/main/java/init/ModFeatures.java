package init;

import ero.Ero;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import world.RubberTreeFeature;

public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Ero.MOD_ID);

    public static final DeferredHolder<Feature<?>, RubberTreeFeature> RUBBER_TREE =
        FEATURES.register("rubber_tree", () -> new RubberTreeFeature(NoneFeatureConfiguration.CODEC));
}
