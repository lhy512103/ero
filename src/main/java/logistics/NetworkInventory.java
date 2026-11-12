package logistics;

import blockentity.LogisticsPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything stored in a pipe network, seen as one container. A logistics interface offers this
 * view on its own block, so anything attached to the interface (a hopper, another mod's pipe, an
 * Applied Energistics storage bus) reads and uses the stock of all the network's chests and silos.
 * Machines are not part of it: only what sits in storage.
 */
public final class NetworkInventory implements ResourceHandler<ItemResource> {
    private final LogisticsPortBlockEntity port;
    private final List<ResourceHandler<ItemResource>> parts = new ArrayList<>();
    private long resolvedAt = -1;
    private int resolvedVersion = -1;

    public NetworkInventory(LogisticsPortBlockEntity port) { this.port = port; }

    /** The storage behind the network's interfaces; looked up once per tick, and again when a network changed. */
    private List<ResourceHandler<ItemResource>> parts() {
        if (!(port.getLevel() instanceof ServerLevel level)) return List.of();
        if (level.getGameTime() == resolvedAt && LogisticsManager.version() == resolvedVersion) return parts;
        resolvedAt = level.getGameTime();
        resolvedVersion = LogisticsManager.version();
        parts.clear();
        for (BlockPos pos : LogisticsManager.networkOf(level, port.getBlockPos())) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof LogisticsPortBlockEntity member) || !member.isStorage()) continue;
            if (member.getChannel() != port.getChannel()) continue;
            // an interface facing another interface would make the view contain itself
            if (level.getBlockEntity(member.targetPos()) instanceof LogisticsPortBlockEntity) continue;
            ResourceHandler<ItemResource> handler = member.handler();
            if (handler != null) parts.add(handler);
        }
        return parts;
    }

    private interface Call<R> { R on(ResourceHandler<ItemResource> handler, int index); }

    private <R> R at(int index, R fallback, Call<R> call) {
        if (index < 0) return fallback;
        for (ResourceHandler<ItemResource> handler : parts()) {
            int size = handler.size();
            if (index < size) return call.on(handler, index);
            index -= size;
        }
        return fallback;
    }

    @Override
    public int size() {
        int total = 0;
        for (ResourceHandler<ItemResource> handler : parts()) total += handler.size();
        return total;
    }

    @Override public ItemResource getResource(int index) { return at(index, ItemResource.EMPTY, ResourceHandler::getResource); }
    @Override public long getAmountAsLong(int index) { return at(index, 0L, ResourceHandler::getAmountAsLong); }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) { return at(index, 0L, (handler, i) -> handler.getCapacityAsLong(i, resource)); }

    @Override
    public boolean isValid(int index, ItemResource resource) { return at(index, false, (handler, i) -> handler.isValid(i, resource)); }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return at(index, 0, (handler, i) -> handler.insert(i, resource, amount, transaction));
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return at(index, 0, (handler, i) -> handler.extract(i, resource, amount, transaction));
    }
}
