package network;

import blockentity.MachineBlockEntity;
import blockentity.cable.CableBlockEntity;
import energy.EUCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

/** One server-side transfer path for adjacent machines and cables. No persisted topology. */
public final class EnergyTransferManager {
    private static final Direction[] DIRECTIONS = Direction.values();
    // Values contain positions only, never levels/entities/capabilities, so closed worlds can be collected.
    private static final Map<Level, State> STATES = new WeakHashMap<>();
    private static final class State {
        final Set<BlockPos> machines = new LinkedHashSet<>();
        final Map<BlockPos, List<Route>> routes = new HashMap<>();
    }
    private record Path(BlockPos pos, long limit, Path parent) {}
    private record Route(BlockPos target, Direction side, Path path) {}
    private record Step(BlockPos pos, Path path) {}
    private EnergyTransferManager() {}

    public static void register(MachineBlockEntity machine) {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide()) return;
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        if (state.machines.add(machine.getBlockPos().immutable())) state.routes.clear();
    }
    public static void remove(MachineBlockEntity machine) {
        Level level = machine.getLevel();
        State state = STATES.get(level);
        if (state != null) {
            state.machines.remove(machine.getBlockPos());
            state.routes.clear();
        }
    }
    public static void invalidate(Level level) {
        State state = STATES.get(level);
        if (state != null) state.routes.clear();
    }

    private static BlockEntity loadedEntity(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null;
    }

    private static List<Route> findRoutes(Level level, BlockPos source) {
        List<Route> routes = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<Step> queue = new ArrayDeque<>();
        queue.add(new Step(source, null));
        visited.add(source);
        while (!queue.isEmpty()) {
            Step step = queue.removeFirst();
            BlockEntity current = loadedEntity(level, step.pos());
            for (Direction direction : DIRECTIONS) {
                if (current instanceof CableBlockEntity cable && !cable.isSideEnabled(direction)) continue;
                BlockPos next = step.pos().relative(direction);
                BlockEntity entity = loadedEntity(level, next);
                if (entity instanceof CableBlockEntity cable) {
                    if (!cable.isSideEnabled(direction.getOpposite()) || !visited.add(next)) continue;
                    queue.addLast(new Step(next, new Path(next, cable.getVoltage().getMaxVoltage(), step.path())));
                } else if (entity instanceof MachineBlockEntity && !next.equals(source)) {
                    // Machines are endpoints, not conductors: a disabled wire cannot bypass through a machine.
                    routes.add(new Route(next, direction.getOpposite(), step.path()));
                }
            }
        }
        return List.copyOf(routes);
    }

    public static void tick(Level level) {
        if (level.isClientSide() || !level.tickRateManager().runsNormally()) return;
        State state = STATES.get(level);
        if (state == null) return;
        if (state.machines.removeIf(pos -> !(loadedEntity(level, pos) instanceof MachineBlockEntity))) state.routes.clear();
        Map<BlockPos, Long> sent = new HashMap<>();
        Map<BlockPos, Long> received = new HashMap<>();
        Map<BlockPos, Long> cableUsed = new HashMap<>();
        List<BlockPos> sources = new ArrayList<>(state.machines);
        if (sources.isEmpty()) return;
        Collections.rotate(sources, (int) Math.floorMod(level.getGameTime(), sources.size()));
        // Generators feed consumers first, then storage; storage only feeds consumers (no charge loops).
        for (int pass = 0; pass < 3; pass++) {
            for (BlockPos sourcePos : sources) {
                if (!(loadedEntity(level, sourcePos) instanceof MachineBlockEntity sourceMachine)) continue;
                EUCapability source = sourceMachine.getEUCapability(null);
                if (source == null || !source.canExtract() || source.getEnergyStored() <= 0) continue;
                boolean storageSource = source.canReceive();
                if (storageSource != (pass == 2)) continue;
                List<Route> routes = state.routes.computeIfAbsent(sourcePos, pos -> findRoutes(level, pos));
                for (Route route : routes) {
                    if (!(loadedEntity(level, route.target()) instanceof MachineBlockEntity targetMachine)) continue;
                    EUCapability target = targetMachine.getEUCapability(route.side());
                    if (target == null || !target.canReceive() || target.canExtract() != (pass == 1)) continue;
                    // No voltage gate: IUCE has no transformers, and the amount below is already capped by
                    // the source rate, the target rate and every cable on the path.
                    long amount = Math.min(source.getMaxTransferRate() - sent.getOrDefault(sourcePos, 0L),
                        target.getMaxTransferRate() - received.getOrDefault(route.target(), 0L));
                    for (Path path = route.path(); path != null && amount > 0; path = path.parent()) {
                        if (!(loadedEntity(level, path.pos()) instanceof CableBlockEntity)) { amount = 0; break; }
                        amount = Math.min(amount, path.limit() - cableUsed.getOrDefault(path.pos(), 0L));
                    }
                    if (amount <= 0) continue;
                    amount = Math.min(source.extractEnergy(amount, true), target.receiveEnergy(amount, true));
                    if (amount <= 0) continue;
                    long extracted = source.extractEnergy(amount, false);
                    long accepted = target.receiveEnergy(extracted, false);
                    if (accepted != extracted) throw new IllegalStateException("EU transfer simulation changed during server tick");
                    sent.merge(sourcePos, accepted, Long::sum);
                    received.merge(route.target(), accepted, Long::sum);
                    for (Path path = route.path(); path != null; path = path.parent()) cableUsed.merge(path.pos(), accepted, Long::sum);
                    sourceMachine.setChanged();
                    targetMachine.setChanged();
                }
            }
        }
    }
}
