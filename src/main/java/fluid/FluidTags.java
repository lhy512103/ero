package fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/** The fluid tags defined in {@code data/iuce/tags/fluid}, as constants machines can test against. */
public final class FluidTags {

    /** Gases the boiler can burn and the vent stack lights. */
    public static final TagKey<Fluid> FLAMMABLE_GASES = create("flammable_gases");

    private static TagKey<Fluid> create(String name) {
        return TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("iuce", name));
    }

    private FluidTags() { }
}
