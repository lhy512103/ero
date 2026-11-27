package multiblock;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The material store of a large machine controller: one slot per kind of block its structure is
 * built from, each holding up to what the whole structure needs. Kinds the structure no longer
 * uses (after a resize) keep their slot with no room, so they can only be taken out.
 */
public class MaterialStorage extends ItemStacksResourceHandler {
    private final Runnable changed;
    private List<Item> kinds = new ArrayList<>();
    private int[] caps = new int[0];

    public MaterialStorage(Runnable changed) {
        super(0);
        this.changed = changed;
    }

    /** Lays the slots out for a structure needing {@code needs}; stored blocks are kept. */
    public void configure(Map<Item, Integer> needs) {
        Map<Item, Integer> stored = new LinkedHashMap<>();
        for (int i = 0; i < size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) stored.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        List<Item> order = new ArrayList<>(needs.keySet());
        for (Item item : stored.keySet()) if (!needs.containsKey(item)) order.add(item);
        if (order.equals(kinds) && sameCaps(order, needs) && stacks.size() == order.size()) return;
        NonNullList<ItemStack> next = NonNullList.withSize(order.size(), ItemStack.EMPTY);
        int[] nextCaps = new int[order.size()];
        for (int i = 0; i < order.size(); i++) {
            Item item = order.get(i);
            nextCaps[i] = needs.getOrDefault(item, 0);
            int amount = stored.getOrDefault(item, 0);
            if (amount > 0) next.set(i, new ItemStack(item, amount));
        }
        kinds = order;
        caps = nextCaps;
        setStacks(next);
    }

    /** What is stored, by block kind; for saving and for the dropped controller item. */
    public Map<Item, Integer> contents() {
        Map<Item, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) out.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return out;
    }

    /** Replaces the contents; the slots are laid out again by the next {@link #configure}. */
    public void restore(Map<Item, Integer> contents) {
        List<Item> order = new ArrayList<>(contents.keySet());
        NonNullList<ItemStack> next = NonNullList.withSize(order.size(), ItemStack.EMPTY);
        for (int i = 0; i < order.size(); i++) next.set(i, new ItemStack(order.get(i), contents.get(order.get(i))));
        kinds = order;
        caps = new int[order.size()];
        setStacks(next);
    }

    public static final com.mojang.serialization.Codec<Map<Item, Integer>> CODEC =
        com.mojang.serialization.Codec.unboundedMap(net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec(), com.mojang.serialization.Codec.INT);

    private boolean sameCaps(List<Item> order, Map<Item, Integer> needs) {
        if (caps.length != order.size()) return false;
        for (int i = 0; i < order.size(); i++) if (caps[i] != needs.getOrDefault(order.get(i), 0)) return false;
        return true;
    }

    public Item kind(int index) {
        return index < kinds.size() ? kinds.get(index) : stacks.get(index).getItem();
    }

    /** How many of this slot's block the structure needs: the slot's limit. */
    public int need(int index) { return index < caps.length ? caps[index] : 0; }

    public int indexOf(Item item) {
        for (int i = 0; i < size(); i++) if (kind(i) == item) return i;
        return -1;
    }

    public int count(Item item) {
        int index = indexOf(item);
        return index < 0 ? 0 : getAmountAsInt(index);
    }

    public long total() {
        long total = 0;
        for (int i = 0; i < size(); i++) total += getAmountAsLong(i);
        return total;
    }

    /** Adds up to {@code amount} of {@code item}; returns how many fitted. */
    public int add(Item item, int amount) {
        int index = indexOf(item);
        if (index < 0 || amount <= 0) return 0;
        int have = getAmountAsInt(index);
        int added = Math.min(Math.max(0, need(index) - have), amount);
        if (added > 0) set(index, ItemResource.of(item), have + added);
        return added;
    }

    /** Takes up to {@code amount} of {@code item}; returns how many came out. */
    public int take(Item item, int amount) {
        int index = indexOf(item);
        if (index < 0 || amount <= 0) return 0;
        int have = getAmountAsInt(index);
        int taken = Math.min(have, amount);
        if (taken > 0) set(index, have == taken ? ItemResource.EMPTY : ItemResource.of(item), have - taken);
        return taken;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return index < kinds.size() && resource.getItem() == kinds.get(index);
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return Math.max(need(index), getAmountAsInt(index));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int room = need(index) - getAmountAsInt(index);
        if (room <= 0) return 0;
        return super.insert(index, resource, Math.min(amount, room), transaction);
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) { changed.run(); }
}
