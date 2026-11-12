package blockentity;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The view of a machine's inventory given to hoppers and other mods' pipes: raw materials can
 * only go into the machine's input slots, products can only come out of its output slots, and
 * upgrade slots are not visible at all.
 * <p>
 * Indices 0..inputs-1 map to the input slots, the rest to the output slots.
 * </p>
 */
public final class AutomationItemHandler implements ResourceHandler<ItemResource> {

    private final MachineBlockEntity machine;
    private final ItemStacksResourceHandler inventory;
    private final int[] inputs;
    private final int[] outputs;

    AutomationItemHandler(MachineBlockEntity machine, ItemStacksResourceHandler inventory, int[] inputs, int[] outputs) {
        this.machine = machine;
        this.inventory = inventory;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    private boolean isInput(int index) { return index >= 0 && index < inputs.length; }

    private int slot(int index) {
        return index < inputs.length ? inputs[index] : outputs[index - inputs.length];
    }

    @Override
    public int size() { return inputs.length + outputs.length; }

    @Override
    public ItemResource getResource(int index) { return inventory.getResource(slot(index)); }

    @Override
    public long getAmountAsLong(int index) { return inventory.getAmountAsLong(slot(index)); }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) { return inventory.getCapacityAsLong(slot(index), resource); }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return isInput(index) && inventory.isValid(slot(index), resource) && machine.acceptsAutomationInput(slot(index), resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!isValid(index, resource)) return 0;
        return inventory.insert(slot(index), resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (index < inputs.length || index >= size()) return 0;
        return inventory.extract(slot(index), resource, amount, transaction);
    }
}
