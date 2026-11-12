package logistics;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * One kind of item in a very large amount. The amount is a plain number, so moving one item or
 * a few thousand costs the same.
 */
public final class SiloInventory extends SnapshotJournal<SiloInventory.Contents> implements ResourceHandler<ItemResource> {
    public record Contents(ItemResource resource, long amount) {}

    private final long capacity;
    private final Runnable onChanged;
    private ItemResource resource = ItemResource.EMPTY;
    private long amount;

    public SiloInventory(long capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
    }

    public ItemResource getResource() { return resource; }
    public long getAmount() { return amount; }
    public long getCapacity() { return capacity; }

    /** Overwrites the contents; used when loading. */
    public void set(ItemResource resource, long amount) {
        long clamped = Math.max(0, Math.min(capacity, amount));
        this.resource = clamped == 0 ? ItemResource.EMPTY : resource;
        this.amount = clamped;
    }

    @Override public int size() { return 1; }
    @Override public ItemResource getResource(int index) { return resource; }
    @Override public long getAmountAsLong(int index) { return amount; }
    @Override public long getCapacityAsLong(int index, ItemResource resource) { return capacity; }

    @Override
    public boolean isValid(int index, ItemResource candidate) {
        return index == 0 && !candidate.isEmpty() && (resource.isEmpty() || resource.equals(candidate));
    }

    @Override
    public int insert(int index, ItemResource candidate, int count, TransactionContext transaction) {
        if (count <= 0 || !isValid(index, candidate)) return 0;
        long moved = Math.min(count, capacity - amount);
        if (moved <= 0) return 0;
        updateSnapshots(transaction);
        resource = candidate;
        amount += moved;
        return (int) moved;
    }

    @Override
    public int extract(int index, ItemResource candidate, int count, TransactionContext transaction) {
        if (index != 0 || count <= 0 || resource.isEmpty() || !resource.equals(candidate)) return 0;
        long moved = Math.min(count, amount);
        if (moved <= 0) return 0;
        updateSnapshots(transaction);
        amount -= moved;
        if (amount == 0) resource = ItemResource.EMPTY;
        return (int) moved;
    }

    @Override
    protected Contents createSnapshot() { return new Contents(resource, amount); }

    @Override
    protected void revertToSnapshot(Contents snapshot) {
        resource = snapshot.resource();
        amount = snapshot.amount();
    }

    @Override
    protected void onRootCommit(Contents originalState) { onChanged.run(); }
}
