package fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

/**
 * The outside view of a machine with several tanks: pipes may fill the input tanks and drain the
 * output tanks, never the other way round. A fluid that already sits in one input tank is never
 * put into a second one, so each fluid a recipe needs keeps a tank of its own.
 */
public class MachineTanksHandler implements ResourceHandler<FluidResource> {
    private final List<MachineTank> tanks;
    private final int inputs;

    /** @param inputs how many of {@code tanks}, counted from the start, are input tanks */
    public MachineTanksHandler(List<MachineTank> tanks, int inputs) {
        this.tanks = tanks;
        this.inputs = inputs;
    }

    @Override public int size() { return tanks.size(); }
    @Override public FluidResource getResource(int index) { return tanks.get(index).getResource(); }
    @Override public long getAmountAsLong(int index) { return tanks.get(index).getAmount(); }
    @Override public long getCapacityAsLong(int index, FluidResource resource) { return tanks.get(index).getCapacity(); }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return index < inputs && tanks.get(index).isValid(0, resource);
    }

    /** True when {@code resource} may go into input tank {@code index}. */
    public boolean accepts(int index, FluidResource resource) {
        if (index >= inputs || !tanks.get(index).isValid(0, resource)) return false;
        MachineTank tank = tanks.get(index);
        if (!tank.isEmpty()) return tank.getResource().equals(resource);
        for (int other = 0; other < inputs; other++) {
            if (other != index && !tanks.get(other).isEmpty() && tanks.get(other).getResource().equals(resource)) return false;
        }
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return accepts(index, resource) ? tanks.get(index).insert(0, resource, amount, transaction) : 0;
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        // fill a tank that already holds this fluid before starting a new one
        int inserted = 0;
        for (int pass = 0; pass < 2 && inserted < amount; pass++) {
            for (int index = 0; index < inputs && inserted < amount; index++) {
                if ((pass == 0) == tanks.get(index).isEmpty()) continue;
                inserted += insert(index, resource, amount - inserted, transaction);
            }
        }
        return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return index < inputs ? 0 : tanks.get(index).extract(0, resource, amount, transaction);
    }
}
