package logistics;

import block.LogisticsPortBlock;
import block.PneumaticPipeBlock;
import blockentity.LogisticsPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The pneumatic pipe network. Pipes do no work of their own: the network is a cached list of
 * which logistics interfaces are joined by pipes, rebuilt only after a pipe or interface is placed
 * or removed. Every {@link #INTERVAL} ticks each network moves items directly between the
 * inventories behind its interfaces; nothing travels through the pipes and nothing is dropped
 * into the world.
 * <ul>
 * <li>What a machine produced goes to a machine that can use it, otherwise into storage.</li>
 * <li>What sits in storage goes to a machine that can use it.</li>
 * </ul>
 */
public final class LogisticsManager {
    private LogisticsManager() {}

    /** Ticks between two rounds of a network. */
    public static final int INTERVAL = 4;
    /** Items one interface may send per tick (tier I). */
    public static final int RATE = 64;
    private static final int MAX_PIPES = 50_000;

    private static final class State {
        final Set<BlockPos> ports = new HashSet<>();
        List<List<BlockPos>> networks = List.of();
        boolean dirty = true;
    }

    private static final Map<Level, State> STATES = new WeakHashMap<>();

    public static void register(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        State state = STATES.computeIfAbsent(level, l -> new State());
        state.ports.add(pos.immutable());
        state.dirty = true;
    }

    /** Call whenever a pipe or interface appears, disappears or turns. */
    public static void invalidate(Level level) {
        State state = STATES.get(level);
        if (state != null) state.dirty = true;
    }

    public static void tick(Level level) {
        if (level instanceof ServerLevel server && server.getGameTime() % INTERVAL == 0) runCycle(server);
    }

    /** Number of networks that join at least two interfaces; for tests and diagnostics. */
    public static int networkCount(ServerLevel level) {
        State state = STATES.get(level);
        if (state == null) return 0;
        if (state.dirty) rebuild(level, state);
        return state.networks.size();
    }

    /** One round for every network of the level. */
    public static void runCycle(ServerLevel level) {
        State state = STATES.get(level);
        if (state == null || state.ports.isEmpty()) return;
        if (state.dirty) rebuild(level, state);
        for (List<BlockPos> network : state.networks) process(level, network);
    }

    private static void rebuild(ServerLevel level, State state) {
        state.dirty = false;
        for (Iterator<BlockPos> iterator = state.ports.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof LogisticsPortBlock)) iterator.remove();
        }
        List<List<BlockPos>> networks = new ArrayList<>();
        Set<BlockPos> joined = new HashSet<>();
        for (BlockPos start : state.ports) {
            if (!level.isLoaded(start) || !joined.add(start)) continue;
            List<BlockPos> members = new ArrayList<>();
            members.add(start);
            Set<BlockPos> pipes = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            // an interface joins the network through the pipe behind it
            BlockPos behind = start.relative(level.getBlockState(start).getValue(LogisticsPortBlock.FACING).getOpposite());
            if (level.isLoaded(behind) && level.getBlockState(behind).getBlock() instanceof PneumaticPipeBlock && pipes.add(behind)) queue.add(behind);
            while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
                BlockPos current = queue.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!level.isLoaded(next)) continue;
                    BlockState nextState = level.getBlockState(next);
                    if (nextState.getBlock() instanceof PneumaticPipeBlock) {
                        if (pipes.add(next)) queue.add(next);
                    } else if (nextState.getBlock() instanceof LogisticsPortBlock
                            && nextState.getValue(LogisticsPortBlock.FACING) == direction && joined.add(next)) {
                        members.add(next);
                    }
                }
            }
            if (members.size() > 1) networks.add(members);
        }
        state.networks = networks;
    }

    private static void process(ServerLevel level, List<BlockPos> network) {
        List<LogisticsPortBlockEntity> ports = new ArrayList<>(network.size());
        for (BlockPos pos : network) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof LogisticsPortBlockEntity port) ports.add(port);
        }
        if (ports.size() < 2) return;
        for (LogisticsPortBlockEntity source : ports) {
            if (!source.supplies()) continue;
            ResourceHandler<ItemResource> handler = source.handler();
            if (handler == null) continue;
            boolean storage = source.isStorage();
            int budget = RATE * INTERVAL;
            for (int index = 0; index < handler.size() && budget > 0; index++) {
                ItemResource resource = handler.getResource(index);
                long stored = handler.getAmountAsLong(index);
                if (resource.isEmpty() || stored <= 0 || !source.passes(resource)) continue;
                int offered = (int) Math.min(stored, budget);
                // machines that can use it come first; what a machine made and nobody needs goes into storage
                for (int pass = 0; pass < (storage ? 1 : 2) && offered > 0; pass++) {
                    for (LogisticsPortBlockEntity target : ports) {
                        if (target == source || offered <= 0) continue;
                        int moved = transfer(handler, index, resource, offered, target, pass == 1);
                        offered -= moved;
                        budget -= moved;
                    }
                }
            }
        }
    }

    private static int transfer(ResourceHandler<ItemResource> from, int index, ItemResource resource, int amount,
                                LogisticsPortBlockEntity target, boolean leftovers) {
        int room;
        try (Transaction probe = Transaction.openRoot()) {
            room = target.accept(resource, amount, probe, leftovers);
        }
        if (room <= 0) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = from.extract(index, resource, room, transaction);
            if (taken <= 0 || target.accept(resource, taken, transaction, leftovers) != taken) return 0;
            transaction.commit();
            return taken;
        }
    }
}
