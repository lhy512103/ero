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
import net.neoforged.neoforge.transfer.fluid.FluidResource;
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
 * or removed. Every {@link #INTERVAL} ticks each network moves items and fluids directly between
 * the blocks behind its interfaces; nothing travels through the pipes and nothing is dropped
 * into the world.
 * <ul>
 * <li>What a machine produced goes to a machine that can use it, otherwise into storage.</li>
 * <li>What sits in storage goes to a machine that can use it.</li>
 * <li>Fluid goes from pumps and tanks to machines with a tank for it; what a pump has left goes into tanks.</li>
 * <li>Interfaces only deal with interfaces on the same channel.</li>
 * <li>A network that had nothing to move rests: it waits twice as long after every empty round,
 * up to {@link #MAX_REST} rounds, and is back at full pace as soon as something moves.</li>
 * </ul>
 */
public final class LogisticsManager {
    private LogisticsManager() {}

    /** Ticks between two rounds of a network. */
    public static final int INTERVAL = 4;
    /** Items one interface may send per tick (tier I). */
    public static final int RATE = 64;
    /** Millibuckets one interface may send per tick. */
    public static final int FLUID_RATE = 1000;
    /** Longest rest of an idle network, in rounds (3.2 seconds). */
    public static final int MAX_REST = 16;
    private static final int MAX_PIPES = 50_000;

    private static final class Network {
        final List<BlockPos> ports;
        int rest, skip;

        final boolean fluid;
        Network(List<BlockPos> ports, boolean fluid) { this.ports = ports; this.fluid = fluid; }
    }

    private static final class State {
        final Set<BlockPos> ports = new HashSet<>();
        List<Network> networks = List.of();
        final Map<BlockPos,Network> waterPipes = new java.util.HashMap<>();
        final Set<BlockPos> dedicated = new HashSet<>();
        boolean dirty = true;
    }

    private static final Map<Level, State> STATES = new WeakHashMap<>();
    private static int version;

    /** Changes whenever any network may have changed; lets callers drop what they cached about a network. */
    public static int version() { return version; }

    public static void register(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        State state = STATES.computeIfAbsent(level, l -> new State());
        state.ports.add(pos.immutable());
        state.dirty = true;
        version++;
    }

    /** Call whenever a pipe or interface appears, disappears or turns. */
    public static void invalidate(Level level) {
        version++;
        State state = STATES.get(level);
        if (state != null) state.dirty = true;
    }

    /** Ends every network's rest, e.g. after an interface was reconfigured. */
    public static void wake(Level level) {
        State state = STATES.get(level);
        if (state == null) return;
        for (Network network : state.networks) {
            network.rest = 0;
            network.skip = 0;
        }
    }

    public static void tick(Level level) {
        if (level instanceof ServerLevel server && server.getGameTime() % INTERVAL == 0) cycle(server, false);
    }

    /** Number of networks that join at least two interfaces; for tests and diagnostics. */
    public static int networkCount(ServerLevel level) {
        State state = STATES.get(level);
        if (state == null) return 0;
        if (state.dirty) rebuild(level, state);
        return state.networks.size();
    }

    /** Number of networks currently resting; for tests and diagnostics. */
    public static int restingCount(ServerLevel level) {
        State state = STATES.get(level);
        if (state == null) return 0;
        int resting = 0;
        for (Network network : state.networks) if (network.skip > 0) resting++;
        return resting;
    }

    /** The interfaces sharing a network with the one at {@code port}; empty when it is not joined to any other. */
    public static List<BlockPos> networkOf(ServerLevel level, BlockPos port) {
        State state = STATES.get(level);
        if (state == null) return List.of();
        if (state.dirty) rebuild(level, state);
        for (Network network : state.networks) if (!network.fluid && network.ports.contains(port)) return network.ports;
        return List.of();
    }

    /** One round for every network of the level, resting or not. */
    public static void runCycle(ServerLevel level) { cycle(level, true); }

    /** One round as the game runs it: resting networks are skipped. */
    public static void runDueCycle(ServerLevel level) { cycle(level, false); }

    private static void cycle(ServerLevel level, boolean force) {
        State state = STATES.get(level);
        if (state == null || state.ports.isEmpty()) return;
        if (state.dirty) rebuild(level, state);
        for (Network network : state.networks) {
            if (!force && network.skip > 0) {
                network.skip--;
                continue;
            }
            if (process(level, network, state) > 0) {
                network.rest = 0;
                network.skip = 0;
            } else {
                network.rest = Math.min(MAX_REST, network.rest == 0 ? 1 : network.rest * 2);
                network.skip = network.rest;
            }
        }
    }

    private static void rebuild(ServerLevel level, State state) {
        state.dirty = false;
        for (Iterator<BlockPos> iterator = state.ports.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof LogisticsPortBlock)) iterator.remove();
        }
        List<Network> networks = new ArrayList<>();
        state.waterPipes.clear();
        state.dedicated.clear();
        for (boolean fluid : new boolean[]{false,true}) {
        Set<BlockPos> joined = new HashSet<>();
        for (BlockPos start : state.ports) {
            if (!level.isLoaded(start) || !joined.add(start)) continue;
            List<BlockPos> members = new ArrayList<>();
            members.add(start);
            Set<BlockPos> pipes = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            // an interface joins the network through the pipe behind it
            BlockPos behind = start.relative(level.getBlockState(start).getValue(LogisticsPortBlock.FACING).getOpposite());
            Direction face = level.getBlockState(start).getValue(LogisticsPortBlock.FACING);
            for (Direction side : Direction.values()) {
                if (side == face || (!fluid && !start.relative(side).equals(behind))) continue;
                BlockPos adjacent=start.relative(side);
                if (!level.isLoaded(adjacent)) continue;
                var candidate=level.getBlockState(adjacent);
                if (candidate.getBlock() instanceof PneumaticPipeBlock && (candidate.getBlock() instanceof block.FluidPipeBlock)==fluid
                        && block.FluidPipeBlock.open(candidate,side.getOpposite()) && pipes.add(adjacent)) queue.add(adjacent);
            }
            while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
                BlockPos current = queue.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!level.isLoaded(next)) continue;
                    BlockState nextState = level.getBlockState(next);
                    if (!block.FluidPipeBlock.open(level.getBlockState(current),direction) || !block.FluidPipeBlock.open(nextState,direction.getOpposite())) continue;
                    if (nextState.getBlock() instanceof PneumaticPipeBlock && (nextState.getBlock() instanceof block.FluidPipeBlock)==fluid) {
                        if (pipes.add(next)) queue.add(next);
                    } else if (nextState.getBlock() instanceof LogisticsPortBlock
                            && (fluid ? nextState.getValue(LogisticsPortBlock.FACING) != direction.getOpposite() : nextState.getValue(LogisticsPortBlock.FACING) == direction) && joined.add(next)) {
                        members.add(next);
                    }
                }
            }
            Network net=new Network(members,fluid);
            if (members.size() > 1) networks.add(net);
            if(fluid && !pipes.isEmpty()) { state.dedicated.addAll(members); for(BlockPos pipe:pipes) state.waterPipes.put(pipe,net); }
        }
        }
        state.networks = networks;
    }

    /** Moves what there is to move and returns how much that was (items plus millibuckets). */
    public static boolean hasWater(Level level, BlockPos pipe) {
        State state=STATES.get(level);
        if (!(level instanceof ServerLevel server) || state==null) return false;
        if(state.dirty) rebuild(server,state);
        Network supply=state.waterPipes.get(pipe);
        if(supply==null) return false;
        for(BlockPos pos:supply.ports) if(level.getBlockEntity(pos) instanceof LogisticsPortBlockEntity port && port.supplies()) {
            var tanks=port.fluidHandler();
            if(tanks!=null) for(int i=0;i<tanks.size();i++) if(tanks.getAmountAsLong(i)>0 && tanks.getResource(i).getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)) return true;
        }
        return false;
    }
    private static long process(ServerLevel level, Network group, State state) {
        List<BlockPos> network=group.ports;
        List<LogisticsPortBlockEntity> ports = new ArrayList<>(network.size());
        for (BlockPos pos : network) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof LogisticsPortBlockEntity port) ports.add(port);
        }
        if (ports.size() < 2) return 0;
        long total = 0;
        for (LogisticsPortBlockEntity source : ports) {
            if (!source.supplies()) continue;
            if (group.fluid || !state.dedicated.contains(source.getBlockPos())) total += fluids(source, ports, group.fluid ? Set.of() : state.dedicated);
            if(group.fluid) continue;
            ResourceHandler<ItemResource> handler = source.handler();
            if (handler == null) continue;
            boolean storage = source.isStorage();
            int budget = (source.getBlockState().is(block.ModBlocks.LOGISTICS_PORT_II.get()) ? 4096 : RATE) * INTERVAL;
            for (int index = 0; index < handler.size() && budget > 0; index++) {
                ItemResource resource = handler.getResource(index);
                long stored = handler.getAmountAsLong(index);
                if (resource.isEmpty() || stored <= 0 || !source.passes(resource)) continue;
                int offered = (int) Math.min(stored, budget);
                // machines that can use it come first; what a machine made and nobody needs goes into storage
                for (int pass = 0; pass < (storage ? 1 : 2) && offered > 0; pass++) {
                    for (LogisticsPortBlockEntity target : ports) {
                        if (target == source || offered <= 0 || target.getChannel() != source.getChannel()) continue;
                        int moved = transfer(handler, index, resource, offered, target, pass == 1);
                        offered -= moved;
                        budget -= moved;
                        total += moved;
                    }
                }
            }
        }
        return total;
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

    /**
     * Pumps and tanks feed machines; a pump's surplus goes into tanks. Machines with several tanks
     * hand their products to machines that use them first and to tanks otherwise. Single-tank
     * machines are never drained.
     */
    private static long fluids(LogisticsPortBlockEntity source, List<LogisticsPortBlockEntity> ports, Set<BlockPos> separate) {
        int role = source.fluidRole();
        boolean machine = role == LogisticsPortBlockEntity.FLUID_MACHINE;
        if (role != LogisticsPortBlockEntity.FLUID_PUMP && role != LogisticsPortBlockEntity.FLUID_TANK && !(machine && source.supplies())) return 0;
        ResourceHandler<FluidResource> from = source.fluidHandler();
        if (from == null) return 0;
        long total = 0;
        int budget = (source.getBlockState().is(block.ModBlocks.LOGISTICS_PORT_II.get()) ? 64000 : FLUID_RATE) * INTERVAL;
        int first = machine && source.getLevel().getBlockEntity(source.targetPos()) instanceof fluid.MultiTankProvider tanks
            ? tanks.getInputTankCount() : 0;   // a machine only gives away what is in its output tanks
        for (int index = first; index < from.size() && budget > 0; index++) {
            FluidResource resource = from.getResource(index);
            long stored = from.getAmountAsLong(index);
            if (resource.isEmpty() || stored <= 0) continue;
            int offered = (int) Math.min(stored, budget);
            boolean surplusToTanks = role == LogisticsPortBlockEntity.FLUID_PUMP || machine;
            for (int pass = 0; pass < (surplusToTanks ? 2 : 1) && offered > 0; pass++) {
                for (LogisticsPortBlockEntity target : ports) {
                    if (target == source || offered <= 0 || separate.contains(target.getBlockPos()) || !target.receives() || target.getChannel() != source.getChannel()) continue;
                    int targetRole = target.fluidRole();
                    boolean wanted = pass == 0
                        ? targetRole == LogisticsPortBlockEntity.FLUID_CONSUMER || targetRole == LogisticsPortBlockEntity.FLUID_MACHINE
                        : targetRole == LogisticsPortBlockEntity.FLUID_TANK;
                    if (!wanted) continue;
                    ResourceHandler<FluidResource> to = target.fluidHandler();
                    if (to == null) continue;
                    int room;
                    try (Transaction probe = Transaction.openRoot()) {
                        room = to.insert(resource, offered, probe);
                    }
                    if (room <= 0) continue;
                    try (Transaction transaction = Transaction.openRoot()) {
                        int taken = from.extract(index, resource, room, transaction);
                        if (taken <= 0 || to.insert(resource, taken, transaction) != taken) continue;
                        transaction.commit();
                        offered -= taken;
                        budget -= taken;
                        total += taken;
                    }
                }
            }
        }
        return total;
    }
}
