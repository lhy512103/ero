package machine.process;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import java.util.function.Supplier;

public final class PlasticComponents {
    public static final Supplier<DataComponentType<Unit>> DRIED = ProcessCards.REGISTER.register("dried",
        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).build());
    public static final Supplier<DataComponentType<Identifier>> SOURCE = ProcessCards.REGISTER.register("sprue_source",
        () -> DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).build());
    public static final Supplier<DataComponentType<Identifier>> PAIL_FLUID = ProcessCards.REGISTER.register("pail_fluid",
        () -> DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).build());
    public static void initialize() {}
}
