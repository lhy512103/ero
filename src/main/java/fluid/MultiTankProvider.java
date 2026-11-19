package fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;

/** A machine with several tanks: some it fills from outside, some it fills itself with products. */
public interface MultiTankProvider {

    /** Input tanks first, then output tanks. */
    List<MachineTank> getTanks();

    int getInputTankCount();

    /** The view pipes and other mods see: inputs can be filled, outputs drained. */
    ResourceHandler<FluidResource> getFluidHandler();
}
