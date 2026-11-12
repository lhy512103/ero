package blockentity;

import block.LogisticsPortBlock;
import logistics.LogisticsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;

/**
 * Logistics interface: joins the block it faces to the pipe network behind it. On a machine it
 * hands the machine's products to the network and asks for what the machine's recipes can use; on
 * a chest, silo or any other container it makes that container the network's storage.
 * The interface holds no items itself; {@link LogisticsManager} moves them.
 */
public class LogisticsPortBlockEntity extends BlockEntity {
    /** Both directions. */
    public static final int AUTO = 0;
    /** Only takes items in from the network. */
    public static final int RECEIVE = 1;
    /** Only hands items to the network. */
    public static final int SUPPLY = 2;

    private int mode = AUTO;
    @Nullable private Item filter;

    public LogisticsPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_PORT.get(), pos, state);
    }

    public int getMode() { return mode; }
    @Nullable public Item getFilter() { return filter; }

    public void configure(int mode, @Nullable Item filter) {
        this.mode = mode < AUTO || mode > SUPPLY ? AUTO : mode;
        this.filter = filter;
        setChanged();
    }

    public Direction facing() { return getBlockState().getValue(LogisticsPortBlock.FACING); }
    public BlockPos targetPos() { return worldPosition.relative(facing()); }

    /** The inventory of the block this interface faces, as pipes and hoppers see it. */
    @Nullable
    public ResourceHandler<ItemResource> handler() {
        return level == null ? null : level.getCapability(Capabilities.Item.BLOCK, targetPos(), facing().getOpposite());
    }

    public boolean passes(ItemResource resource) { return filter == null || resource.getItem() == filter; }
    public boolean supplies() { return mode != RECEIVE; }
    public boolean receives() { return mode != SUPPLY; }

    /** True when the block behind is plain storage rather than something that asks for specific items. */
    public boolean isStorage() {
        BlockEntity target = level == null ? null : level.getBlockEntity(targetPos());
        return !(target instanceof MachineBlockEntity) && !(target instanceof FactoryHubBlockEntity) && !(target instanceof HatchBlockEntity);
    }

    /**
     * Puts items into the block behind the interface and returns how many it took.
     *
     * @param leftovers false: only take what the block asks for (machines, the factory hub);
     *                  true: take products nobody asked for (storage only)
     */
    public int accept(ItemResource resource, int amount, TransactionContext transaction, boolean leftovers) {
        if (level == null || !receives() || !passes(resource)) return 0;
        BlockEntity target = level.getBlockEntity(targetPos());
        if (target instanceof MachineBlockEntity machine) {
            if (leftovers) return 0;
            int slot = machine.requestSlot(resource);
            return slot < 0 ? 0 : machine.getItemHandler().insert(slot, resource, amount, transaction);
        }
        ResourceHandler<ItemResource> handler = handler();
        if (handler == null) return 0;
        // an input hatch asks for what its machine can process; an output hatch takes nothing
        if (target instanceof HatchBlockEntity hatch) {
            return leftovers || !hatch.wants(resource) ? 0 : handler.insert(resource, amount, transaction);
        }
        if (target instanceof FactoryHubBlockEntity hub) {
            return leftovers || !hub.accepts(resource.getItem()) ? 0 : handler.insert(resource, amount, transaction);
        }
        return leftovers ? handler.insert(resource, amount, transaction) : 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) LogisticsManager.register(level, worldPosition);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Mode", mode);
        if (filter != null) output.store("Filter", BuiltInRegistries.ITEM.byNameCodec(), filter);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int saved = input.getIntOr("Mode", AUTO);
        mode = saved < AUTO || saved > SUPPLY ? AUTO : saved;
        filter = input.read("Filter", BuiltInRegistries.ITEM.byNameCodec()).orElse(null);
    }
}
