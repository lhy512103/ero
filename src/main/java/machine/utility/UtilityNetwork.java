package machine.utility;

import block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The utility shares of one dimension. There is no controller and nothing to build in a fixed
 * shape: a supplier reaches every machine it touches, and every machine a run of fluid pipes touches.
 * <p>
 * Every 20 game ticks the network wakes up: it drops last round's result, walks each run of pipes
 * once, and hands out the shares in coordinate order. A machine that wanted a share and did not get
 * one reports {@code missing_utility}, and the same layout always produces the same winner.
 * </p>
 * <p>
 * Machines only ask for a share when a recipe of theirs could start, so an idle machine never holds
 * one. A supplier is told how many shares of each utility it carries and charges fuel, water and
 * power for exactly those, so a supplier with nothing attached uses nothing.
 * </p>
 */
public final class UtilityNetwork {

    /** Ticks between two rebuilds. */
    public static final int PERIOD = 20;

    private static final Map<ServerLevel, UtilityNetwork> NETWORKS = new WeakHashMap<>();

    private final ServerLevel level;
    /** Machines that spoke this round: position → shares wanted per utility. */
    private final Map<BlockPos, EnumMap<Utility, Integer>> demand = new HashMap<>();
    private final Map<BlockPos, EnumMap<Utility, Integer>> granted = new HashMap<>();
    private final Map<Utility, Integer> supply = new EnumMap<>(Utility.class);
    /** Supply of each utility that no machine in a machine's own cluster holds this round. */
    private final Map<BlockPos, EnumMap<Utility, Integer>> free = new HashMap<>();
    /** Shares handed out per supplier and utility in the last round. */
    private final Map<BlockPos, EnumMap<Utility, Integer>> occupied = new HashMap<>();
    private long round = Long.MIN_VALUE;
    /** Set when a machine changed its demand, so the next reader sees the new layout at once. */
    private boolean dirty;
    /** Bumped on {@link #clear()}; machines re-report their demand when it moves. */
    private int generation;

    private UtilityNetwork(ServerLevel level) { this.level = level; }

    public static UtilityNetwork of(ServerLevel level) {
        UtilityNetwork network = NETWORKS.get(level);
        if (network == null) {
            network = new UtilityNetwork(level);
            NETWORKS.put(level, network);
        }
        return network;
    }

    // ---- called by machines ----

    /** Records that a machine is here and how many shares of which utility it could use right now. */
    public void setDemand(BlockPos pos, Map<Utility, Integer> wanted) {
        EnumMap<Utility, Integer> before = demand.get(pos);
        if (wanted.isEmpty()) {
            if (before == null) return;
            demand.remove(pos);
        } else {
            EnumMap<Utility, Integer> now = new EnumMap<>(wanted);
            if (now.equals(before)) return;
            demand.put(pos.immutable(), now);
        }
        dirty = true;
    }

    /**
     * Says the supplies changed under the network's feet, e.g. a boiler ran dry; the next reader
     * recounts at once instead of serving last round's answer.
     */
    public void invalidate() { dirty = true; }

    /** A machine that was broken or removed stops asking for shares. */
    public void remove(BlockPos pos) {
        if (demand.remove(pos) != null) dirty = true;
        granted.remove(pos);
    }

    /** Shares of {@code utility} granted to the machine at {@code pos}. */
    public int granted(BlockPos pos, Utility utility) {
        refreshIfStale();
        Map<Utility, Integer> mine = granted.get(pos);
        return mine == null ? 0 : mine.getOrDefault(utility, 0);
    }

    /** How many shares of {@code utility} the suppliers near this machine can give in total. */
    public int available(BlockPos pos, Utility utility) {
        refreshIfStale();
        return supply.getOrDefault(utility, 0);
    }

    /**
     * Shares of {@code utility} the suppliers this machine can reach can still hand out this round.
     * Suppliers that no pipe or machine connects to this machine do not count, so a machine on its
     * own reports 0 rather than the surplus of a neighbour elsewhere in the level.
     */
    public int free(BlockPos pos, Utility utility) {
        refreshIfStale();
        EnumMap<Utility, Integer> mine = free.get(pos);
        return mine == null ? 0 : mine.getOrDefault(utility, 0);
    }

    // ---- the round ----

    /**
     * Called by machines that want shares; cheap once this round has been calculated. A machine that
     * just started asking for something makes the round run again right away, so it does not sit a
     * whole period without an answer.
     */
    public void refreshIfStale() {
        long current = Math.floorDiv(level.getGameTime(), PERIOD);
        if (current == round && !dirty) return;
        round = current;
        dirty = false;
        rebuild();
    }

    /**
     * Changes whenever the network forgets its demand, which is when a machine must report again
     * even though its own demand did not change. Tests clear the network between layouts; a machine
     * that only reported on a change would otherwise never be found again.
     */
    public int generation() { return generation; }

    private void rebuild() {
        granted.clear();
        supply.clear();
        demand.keySet().removeIf(pos -> !level.hasChunkAt(pos));

        List<BlockPos> starts = new ArrayList<>(demand.keySet());
        starts.sort(ORDER);
        Set<BlockPos> walked = new HashSet<>();
        Map<BlockPos, EnumMap<Utility, Integer>> used = new HashMap<>();
        Map<Utility, Integer> totals = new EnumMap<>(Utility.class);
        free.clear();

        for (BlockPos start : starts) {
            if (walked.contains(start)) continue;
            List<UtilitySupplier> suppliers = new ArrayList<>();
            List<BlockPos> machines = walk(start, walked, suppliers);
            if (suppliers.isEmpty()) continue;
            suppliers.sort(Comparator.comparing(UtilitySupplier::position, ORDER));

            EnumMap<Utility, Integer> left = new EnumMap<>(Utility.class);
            for (UtilitySupplier supplier : suppliers) {
                if (!supplier.ready()) continue;
                for (Utility utility : Utility.values()) {
                    int shares = supplier.shares(utility);
                    if (shares <= 0) continue;
                    left.merge(utility, shares, Integer::sum);
                    totals.merge(utility, shares, Integer::sum);
                }
            }

            machines.sort(ORDER);
            for (BlockPos machine : machines) {
                EnumMap<Utility, Integer> wanted = demand.get(machine);
                if (wanted == null) continue;
                for (Map.Entry<Utility, Integer> entry : wanted.entrySet()) {
                    Utility utility = entry.getKey();
                    int have = left.getOrDefault(utility, 0);
                    if (entry.getValue() <= 0 || have <= 0) continue;
                    int given = Math.min(entry.getValue(), have);
                    left.put(utility, have - given);
                    granted.computeIfAbsent(machine, ignored -> new EnumMap<>(Utility.class)).put(utility, given);
                    for (UtilitySupplier supplier : suppliers) {
                        if (given <= 0) break;
                        if (!supplier.ready()) continue;
                        int shares = supplier.shares(utility);
                        if (shares <= 0) continue;
                        EnumMap<Utility, Integer> mine = used.computeIfAbsent(supplier.position(), ignored -> new EnumMap<>(Utility.class));
                        int taken = mine.getOrDefault(utility, 0);
                        int take = Math.min(given, shares - taken);
                        if (take <= 0) continue;
                        mine.put(utility, taken + take);
                        given -= take;
                    }
                }
            }
            // what this cluster's suppliers can still give, so one machine's shortage can name it
            for (BlockPos machine : machines) {
                EnumMap<Utility, Integer> spare = new EnumMap<>(Utility.class);
                for (Utility utility : Utility.values()) {
                    int have = left.getOrDefault(utility, 0);
                    if (have > 0) spare.put(utility, have);
                }
                if (!spare.isEmpty()) free.put(machine, spare);
            }
        }

        supply.putAll(totals);
        // Tell every supplier how many shares it now carries, including one that lost them all.
        Set<BlockPos> told = new HashSet<>(used.keySet());
        told.addAll(occupied.keySet());
        for (BlockPos pos : told) {
            EnumMap<Utility, Integer> now = used.getOrDefault(pos, new EnumMap<>(Utility.class));
            EnumMap<Utility, Integer> before = occupied.getOrDefault(pos, new EnumMap<>(Utility.class));
            if (now.equals(before)) continue;
            occupied.put(pos, now);
            if (level.getBlockEntity(pos) instanceof UtilitySupplier supplier) {
                for (Utility utility : Utility.values()) supplier.setOccupied(utility, now.getOrDefault(utility, 0));
            }
        }
    }

    /** Walks machines and fluid pipes reachable from {@code start}; returns the machines, not the pipes. */
    private List<BlockPos> walk(BlockPos start, Set<BlockPos> walked, List<UtilitySupplier> suppliers) {
        List<BlockPos> machines = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            BlockEntity entity = level.getBlockEntity(current);
            if (entity instanceof UtilitySupplier supplier) suppliers.add(supplier);
            if (entity instanceof UtilityConsumer) machines.add(current);
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!level.hasChunkAt(next) || !block.FluidPipeBlock.open(level.getBlockState(current),direction)
                        || !block.FluidPipeBlock.open(level.getBlockState(next),direction.getOpposite()) || !visited.add(next)) continue;
                if (ModBlocks.isUtilityPipe(level.getBlockState(next))) {
                    walked.add(next);
                    queue.add(next);
                    continue;
                }
                BlockEntity found = level.getBlockEntity(next);
                if (found instanceof UtilitySupplier || found instanceof UtilityConsumer) queue.add(next);
            }
        }
        return machines;
    }

    /** Coordinate order: deterministic, so equal layouts always hand out shares the same way. */
    public static final Comparator<BlockPos> ORDER = Comparator.comparingLong((BlockPos pos) -> pos.getX())
        .thenComparingLong(pos -> pos.getY()).thenComparingLong(pos -> pos.getZ());

    /** Forgets everything; tests use it so one test cannot leak shares into the next. */
    public void clear() {
        demand.clear();
        granted.clear();
        occupied.clear();
        supply.clear();
        free.clear();
        round = Long.MIN_VALUE;
        dirty = false;
        generation++;
    }
}
