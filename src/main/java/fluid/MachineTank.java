package fluid;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Predicate;

/**
 * A machine's internal fluid tank: one slot, a fixed capacity in millibuckets and an optional
 * filter on what it accepts. It is a standard NeoForge fluid handler, so it can be exposed to
 * pipes and buckets from other mods as-is.
 * <p>
 * The simple {@link #fill}/{@link #drain} helpers are for the machine's own logic; outside
 * callers go through the transactional handler methods.
 * </p>
 */
public class MachineTank extends FluidStacksResourceHandler {

    private final Predicate<FluidResource> filter;
    private final Runnable onChanged;

    public MachineTank(int capacity, Predicate<FluidResource> filter, Runnable onChanged) {
        super(1, capacity);
        this.filter = filter;
        this.onChanged = onChanged;
    }

    /** A tank that only accepts the given fluid. */
    public static MachineTank of(Fluid fluid, int capacity, Runnable onChanged) {
        return new MachineTank(capacity, resource -> resource.getFluid() == fluid, onChanged);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return filter.test(resource);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChanged.run();
    }

    public FluidResource getResource() { return getResource(0); }

    public int getAmount() { return getAmountAsInt(0); }

    public int getCapacity() { return capacity; }

    public int getSpace() { return capacity - getAmount(); }

    public boolean isEmpty() { return getAmount() <= 0; }

    /** Adds up to {@code amount} mB of {@code fluid}; returns how much was accepted. */
    public int fill(Fluid fluid, int amount) {
        if (amount <= 0) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = insert(0, FluidResource.of(fluid), amount, tx);
            tx.commit();
            return inserted;
        }
    }

    /** Removes up to {@code amount} mB of whatever the tank holds; returns how much was removed. */
    public int drain(int amount) {
        FluidResource resource = getResource();
        if (amount <= 0 || resource.isEmpty()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = extract(0, resource, amount, tx);
            tx.commit();
            return extracted;
        }
    }

    /** Overwrites the contents, clamped to the capacity. Used when loading and in tests. */
    public void setAmount(Fluid fluid, int amount) {
        int clamped = Math.max(0, Math.min(capacity, amount));
        set(0, clamped == 0 ? FluidResource.EMPTY : FluidResource.of(fluid), clamped);
    }

    public void save(ValueOutput output, String key) {
        serialize(output.child(key));
    }

    /**
     * Loads the tank. Worlds saved before tanks existed stored a plain millibucket count under
     * {@code legacyKey}; when the tank entry is missing, that count is converted using
     * {@code legacyFluid} and {@code legacyScale} (e.g. 1000 for values that were whole buckets).
     */
    public void load(ValueInput input, String key, String legacyKey, Fluid legacyFluid, int legacyScale) {
        var child = input.child(key);
        if (child.isPresent()) {
            deserialize(child.get());
        } else {
            setAmount(legacyFluid, input.getIntOr(legacyKey, 0) * legacyScale);
        }
    }
}
