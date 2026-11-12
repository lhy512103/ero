package blockentity;

import block.ModBlocks;
import block.PlantControllerBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import item.MoldItem;
import multiblock.MultiblockManager;
import multiblock.ProcessChain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller of a large machine: a hollow shell of plant casing, 5 wide and 5 high, with the
 * controller in the middle of the front face one block above the floor. Hatches may replace any
 * casing block.
 * <ul>
 * <li>Ore processing plant: 5 long; crushes, washes and smelts.</li>
 * <li>Rolling mill: 9 to 17 long; the mold put into the controller decides between plates, wires
 * and rods, and every extra block of length adds to the parallel count (up to double).</li>
 * </ul>
 * The controller is the only part that does any work. Running sixteen operations side by side is
 * one batch whose amounts are multiplied by sixteen, not sixteen batches. The structure is checked
 * when the controller is placed or loaded and again only after a block inside its box changed.
 */
public class PlantControllerBlockEntity extends BlockEntity {
    /** Ticks one batch takes at full power. */
    public static final int DURATION = 100;
    public static final int MILL_MIN = 9, MILL_MAX = 17;
    private static final String[] ORE_STEPS = {"macerator", "ore_washer", "electric_furnace"};
    private static final String[][] MILL_STEPS = {
        {"metal_former_rolling"}, {"metal_former_rolling", "metal_former_cutting"}, {"metal_former_extruding"}};
    private static final String[] NO_STEPS = {};

    private record Pending(ItemResource item, int count) {
        static final Codec<Pending> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemResource.CODEC.fieldOf("item").forGetter(Pending::item),
            Codec.INT.fieldOf("count").forGetter(Pending::count)).apply(instance, Pending::new));
    }

    private boolean structureDirty = true;
    private boolean formed;
    private int length, parallel;
    private Component problem = Component.translatable("message.iuce.plant.unchecked");
    private final List<BlockPos> energyHatches = new ArrayList<>(), inputHatches = new ArrayList<>(), outputHatches = new ArrayList<>();

    private ItemResource mold = ItemResource.EMPTY;
    private final List<Pending> pending = new ArrayList<>();
    private int progress, idleTicks;
    private long energyPerTick, paid;
    private boolean blocked, starved;

    public PlantControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLANT_CONTROLLER.get(), pos, state);
    }

    public boolean isFormed() { return formed; }
    public boolean isWorking() { return !pending.isEmpty(); }
    public int getProgress() { return progress; }
    public int getParallel() { return parallel; }
    public int getLength() { return length; }
    public ItemResource getMold() { return mold; }
    public void markStructureDirty() { structureDirty = true; }

    public boolean isMill() { return getBlockState().is(ModBlocks.MILL_CONTROLLER.get()); }

    /** Swaps the mold of a rolling mill and returns the previous one (possibly empty). */
    public ItemStack swapMold(ItemStack newMold) {
        ItemStack old = mold.isEmpty() ? ItemStack.EMPTY : mold.toStack(1);
        mold = newMold.isEmpty() ? ItemResource.EMPTY : ItemResource.of(newMold.copyWithCount(1));
        setChanged();
        return old;
    }

    private String[] steps() {
        if (!isMill()) return ORE_STEPS;
        return mold.getItem() instanceof MoldItem item && item.mode() < MILL_STEPS.length ? MILL_STEPS[item.mode()] : NO_STEPS;
    }

    // ------------------------------------------------------------------ geometry, shared with the client's projection

    public static BlockPos at(BlockPos controller, Direction facing, int depth, int across, int up) {
        Direction back = facing.getOpposite();
        return controller.relative(back, depth).relative(back.getClockWise(), across).above(up);
    }

    public static boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.PLANT_CASING.get()) || state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get())
            || state.is(ModBlocks.ITEM_INPUT_HATCH.get()) || state.is(ModBlocks.ITEM_OUTPUT_HATCH.get());
    }

    /**
     * Length of the structure a controller at {@code pos} belongs to. A mill ends at the first
     * wall found behind its minimum length; when there is none yet, the minimum length is assumed.
     */
    public static int detectLength(BlockGetter level, BlockPos pos, Direction facing, boolean mill) {
        if (!mill) return 5;
        for (int depth = MILL_MIN - 1; depth < MILL_MAX; depth++) {
            if (isShellBlock(level.getBlockState(at(pos, facing, depth, 0, 1)))) return depth + 1;
        }
        return MILL_MIN;
    }

    /** Every position of the shell, the controller's own excluded. */
    public static List<BlockPos> shell(BlockPos pos, Direction facing, int length) {
        List<BlockPos> positions = new ArrayList<>();
        for (int depth = 0; depth < length; depth++) {
            for (int across = -2; across <= 2; across++) {
                for (int up = -1; up <= 3; up++) {
                    boolean shell = depth == 0 || depth == length - 1 || Math.abs(across) == 2 || up == -1 || up == 3;
                    if (shell && !(depth == 0 && across == 0 && up == 0)) positions.add(at(pos, facing, depth, across, up));
                }
            }
        }
        return positions;
    }

    private Direction facing() { return getBlockState().getValue(PlantControllerBlock.FACING); }

    /** The largest box the structure can occupy, so that lengthening a mill is noticed too. */
    public BoundingBox box() {
        int longest = isMill() ? MILL_MAX : 5;
        return BoundingBox.fromCorners(at(worldPosition, facing(), 0, -2, -1), at(worldPosition, facing(), longest - 1, 2, 3));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) MultiblockManager.register(level, worldPosition, box());
    }

    @Override
    public void setRemoved() {
        if (level != null) MultiblockManager.unregister(level, worldPosition);
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !mold.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), mold.toStack(1));
            mold = ItemResource.EMPTY;
        }
    }

    // ------------------------------------------------------------------ structure

    private void validate() {
        structureDirty = false;
        MultiblockManager.register(level, worldPosition, box());
        for (BlockPos pos : inputHatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) hatch.setController(null);
        energyHatches.clear();
        inputHatches.clear();
        outputHatches.clear();
        Component failure = check();
        formed = failure == null;
        if (!formed) problem = failure;
        if (formed) {
            for (BlockPos pos : inputHatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) hatch.setController(worldPosition);
            int tier = 0;
            for (BlockPos pos : energyHatches) {
                if (level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch) tier = Math.max(tier, hatch.parallel());
            }
            // a longer mill runs more at once: up to twice as many at full length
            double stretch = isMill() ? 1 + (length - MILL_MIN) / (double) (MILL_MAX - MILL_MIN) : 1;
            parallel = (int) (tier * stretch);
        }
        BlockState state = getBlockState();
        if (state.getValue(PlantControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(PlantControllerBlock.FORMED, formed), Block.UPDATE_ALL);
        }
    }

    /** Returns what is wrong, or null when the structure is complete. */
    private Component check() {
        length = detectLength(level, worldPosition, facing(), isMill());
        for (BlockPos pos : shell(worldPosition, facing(), length)) {
            if (!level.isLoaded(pos)) return Component.translatable("message.iuce.plant.unloaded");
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.PLANT_CASING.get())) continue;
            if (state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get())) energyHatches.add(pos);
            else if (state.is(ModBlocks.ITEM_INPUT_HATCH.get())) inputHatches.add(pos);
            else if (state.is(ModBlocks.ITEM_OUTPUT_HATCH.get())) outputHatches.add(pos);
            else return Component.translatable("message.iuce.plant.wrong_block", pos.toShortString());
        }
        if (energyHatches.isEmpty()) return Component.translatable("message.iuce.plant.no_energy_hatch");
        if (inputHatches.isEmpty()) return Component.translatable("message.iuce.plant.no_input_hatch");
        if (outputHatches.isEmpty()) return Component.translatable("message.iuce.plant.no_output_hatch");
        return null;
    }

    /**
     * Puts casing from the stack into every free position of the shell and returns how many were
     * placed; the stack is not changed, the caller takes the blocks.
     */
    public int fill(ItemStack casing) {
        if (level == null || !casing.is(ModBlocks.PLANT_CASING.get().asItem())) return 0;
        int placed = 0;
        int target = detectLength(level, worldPosition, facing(), isMill());
        for (BlockPos pos : shell(worldPosition, facing(), target)) {
            if (placed >= casing.getCount()) break;
            if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced()) continue;
            level.setBlock(pos, ModBlocks.PLANT_CASING.get().defaultBlockState(), Block.UPDATE_ALL);
            placed++;
        }
        structureDirty = true;
        return placed;
    }

    // ------------------------------------------------------------------ work

    /** True when the machine has a process for the item. */
    public boolean canProcess(ItemResource resource) {
        return formed && level instanceof ServerLevel server && ProcessChain.get(server, steps(), resource.getItem()).isPresent();
    }

    public Component describe() {
        if (!formed) return Component.translatable("message.iuce.plant.unformed", problem);
        if (isMill() && mold.isEmpty()) return Component.translatable("message.iuce.plant.no_mold");
        if (pending.isEmpty()) return Component.translatable("message.iuce.plant.idle", parallel);
        if (blocked) return Component.translatable("message.iuce.plant.blocked");
        return Component.translatable(starved ? "message.iuce.plant.starved" : "message.iuce.plant.working", progress * 100 / DURATION);
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (structureDirty) validate();
        if (!formed) return;
        if (pending.isEmpty()) {
            if (idleTicks++ % 10 == 0) tryStart(server);
            return;
        }
        if (progress < DURATION) {
            paid += drawEnergy(energyPerTick - paid);
            starved = paid < energyPerTick;
            if (starved) return;
            paid = 0;
            if (++progress < DURATION) return;
        } else if (idleTicks++ % 10 != 0) {
            return;   // finished but the output hatches were full: look again now and then
        }
        blocked = !deliver(true);
        if (blocked) return;
        pending.clear();
        progress = 0;
        idleTicks = 1;
        setChanged();
        tryStart(server);
    }

    private long drawEnergy(long wanted) {
        long drawn = 0;
        for (BlockPos pos : energyHatches) {
            if (drawn >= wanted) break;
            if (level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch) drawn += hatch.storage().removeEnergy(wanted - drawn);
        }
        return drawn;
    }

    private List<ItemStacksResourceHandler> inventories(List<BlockPos> hatches) {
        List<ItemStacksResourceHandler> list = new ArrayList<>(hatches.size());
        for (BlockPos pos : hatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) list.add(hatch.getInventory());
        return list;
    }

    private void tryStart(ServerLevel server) {
        String[] steps = steps();
        if (steps.length == 0 || parallel <= 0) return;
        List<ItemStacksResourceHandler> inputs = inventories(inputHatches);
        for (ItemStacksResourceHandler inventory : inputs) {
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (inventory.getAmountAsInt(slot) <= 0) continue;
                ItemResource resource = inventory.getResource(slot);
                var chain = ProcessChain.get(server, steps, resource.getItem());
                if (chain.isEmpty()) continue;
                long available = 0;
                for (ItemStacksResourceHandler other : inputs) {
                    for (int i = 0; i < other.size(); i++) if (other.getResource(i).equals(resource)) available += other.getAmountAsInt(i);
                }
                int batches = (int) Math.min(parallel, available / chain.get().inputCount());
                if (batches <= 0) continue;

                // many at once is the same work as one: only the amounts are multiplied
                List<Pending> products = new ArrayList<>();
                for (ProcessChain.Output output : chain.get().outputs()) {
                    double total = output.count() * batches;
                    int count = (int) Math.floor(total);
                    if (server.getRandom().nextDouble() < total - count) count++;
                    if (count > 0) products.add(new Pending(output.item(), count));
                }
                pending.addAll(products);
                if (!deliver(false)) {
                    pending.clear();
                    return;   // no room for the products: do not take the raw materials
                }
                int needed = batches * chain.get().inputCount();
                try (Transaction transaction = Transaction.openRoot()) {
                    for (ItemStacksResourceHandler other : inputs) {
                        if (needed > 0) needed -= other.extract(resource, needed, transaction);
                    }
                    if (needed > 0) {
                        pending.clear();
                        return;
                    }
                    transaction.commit();
                }
                energyPerTick = Math.max(1, chain.get().energy() * batches / DURATION);
                progress = 0;
                paid = 0;
                blocked = false;
                setChanged();
                return;
            }
        }
    }

    /** Puts every pending product into the output hatches; all or nothing. */
    private boolean deliver(boolean commit) {
        List<ItemStacksResourceHandler> outputs = inventories(outputHatches);
        try (Transaction transaction = Transaction.openRoot()) {
            for (Pending product : pending) {
                if (!insert(outputs, product, transaction)) return false;
            }
            if (commit) transaction.commit();
            return true;
        }
    }

    private static boolean insert(List<ItemStacksResourceHandler> outputs, Pending product, TransactionContext transaction) {
        int left = product.count();
        for (ItemStacksResourceHandler inventory : outputs) {
            if (left > 0) left -= inventory.insert(product.item(), left, transaction);
        }
        return left == 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
        output.putLong("EnergyPerTick", energyPerTick);
        output.putLong("Paid", paid);
        output.store("Pending", Pending.CODEC.listOf(), pending);
        output.store("Mold", ItemResource.OPTIONAL_CODEC, mold);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
        energyPerTick = input.getLong("EnergyPerTick").orElse(0L);
        paid = input.getLong("Paid").orElse(0L);
        pending.clear();
        input.read("Pending", Pending.CODEC.listOf()).ifPresent(pending::addAll);
        mold = input.read("Mold", ItemResource.OPTIONAL_CODEC).orElse(ItemResource.EMPTY);
        structureDirty = true;
    }
}
