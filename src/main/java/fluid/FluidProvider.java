package fluid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A machine with a single internal fluid tank. The amount-based methods are the simple API used
 * between IUCE machines and by the fluid upgrades; the tank itself is also exposed to other mods
 * as a standard fluid handler.
 */
public interface FluidProvider {

    /** The machine's tank. */
    MachineTank getTank();

    /** The fluid this machine works with; amount-based transfers always move this fluid. */
    default Fluid getTankFluid() { return Fluids.WATER; }

    /** Removes up to {@code maxAmount} mB and returns how much was removed. */
    default int extractFluid(int maxAmount) { return getTank().drain(maxAmount); }

    /** Adds up to {@code maxAmount} mB and returns how much was accepted. */
    default int insertFluid(int maxAmount) { return getTank().fill(getTankFluid(), maxAmount); }

    default int getStoredFluid() { return getTank().getAmount(); }

    default int getMaxStoredFluid() { return getTank().getCapacity(); }

    /** An item representing the stored fluid, for display. */
    default ItemStack getFluidItem() {
        if (getTank().isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(getTank().getResource().getFluid().getBucket());
    }

    /** Adds exactly one bucket if it fits; returns whether it did. */
    default boolean addBucket() {
        if (getTank().getSpace() < 1000) return false;
        return getTank().fill(getTankFluid(), 1000) == 1000;
    }
}
