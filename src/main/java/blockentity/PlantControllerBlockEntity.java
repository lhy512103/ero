package blockentity;

import block.ModBlocks;
import block.PlantControllerBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import item.MoldItem;
import multiblock.MultiblockManager;
import multiblock.PlantStructure;
import multiblock.ProcessChain;
import multiblock.StructureScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller of a large machine. The ore plant crushes, washes and smelts; the mill's mold
 * selects its process, and the chosen mill length determines its parallel count.
 * The controller is the only part that does any work. Running sixteen operations side by side is
 * one batch whose amounts are multiplied by sixteen, not sixteen batches. The structure is checked
 * when the controller is placed or loaded and again only after a block inside its box changed; the
 * check looks at no more than {@link StructureScan#BUDGET} blocks per tick.
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
    @Nullable private StructureScan scan;
    private final Map<BlockPos, PlantStructure.Part> scanParts = new HashMap<>();
    @Nullable private BlockState layoutState;
    private List<PlantStructure.Cell> layout = List.of();
    @Nullable private Component scanFailure;
    private boolean formed;
    private int length, parallel;
    private Component problem = Component.translatable("message.iuce.plant.unchecked");
    private final List<BlockPos> energyHatches = new ArrayList<>(), inputHatches = new ArrayList<>(), outputHatches = new ArrayList<>();

    private final ItemStacksResourceHandler moldSlot = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) { setChanged(); }
    };
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
    public ItemResource getMold() { return moldSlot.getResource(0); }
    public ItemStacksResourceHandler moldSlot() { return moldSlot; }
    public boolean isScanning() { return scan != null; }
    public boolean isBlocked() { return blocked; }
    public boolean isStarved() { return starved; }
    public void markStructureDirty() { structureDirty = true; }

    public boolean isMill() { return getBlockState().is(ModBlocks.MILL_CONTROLLER.get()); }

    /** Swaps the mold of a rolling mill and returns the previous one (possibly empty). */
    public ItemStack swapMold(ItemStack newMold) {
        ItemStack old = getMold().toStack(1);
        moldSlot.set(0, ItemResource.of(newMold), newMold.isEmpty() ? 0 : 1);
        return old;
    }

    private String[] steps() {
        if (!isMill()) return ORE_STEPS;
        return getMold().getItem() instanceof MoldItem item && item.mode() < MILL_STEPS.length ? MILL_STEPS[item.mode()] : NO_STEPS;
    }

    // ------------------------------------------------------------------ geometry, shared with the client's projection

    /** The length the structure of this controller is meant to have: fixed for the ore plant, chosen for the mill. */
    public static int targetLength(BlockState controller) {
        return controller.is(ModBlocks.MILL_CONTROLLER.get()) ? controller.getValue(PlantControllerBlock.LENGTH)
            : controller.getValue(PlantControllerBlock.LEGACY) ? 5 : PlantStructure.ORE_LENGTH;
    }

    private Direction facing() { return getBlockState().getValue(PlantControllerBlock.FACING); }

    private List<PlantStructure.Cell> layout() {
        BlockState state = getBlockState();
        if (state != layoutState) {
            layoutState = state;
            layout = PlantStructure.cells(worldPosition, facing(), state);
        }
        return layout;
    }

    public boolean canFillCasing(BlockPos pos) {
        return layout().stream().anyMatch(cell -> cell.pos().equals(pos) && cell.part().canFillWithCasing());
    }

    /** The largest box the structure can occupy, so that lengthening a mill is noticed too. */
    public BoundingBox box() {
        return PlantStructure.bounds(worldPosition, facing(), getBlockState());
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
        if (level != null && !getMold().isEmpty())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), swapMold(ItemStack.EMPTY));
    }

    // ------------------------------------------------------------------ structure

    private void beginScan() {
        structureDirty = false;
        MultiblockManager.register(level, worldPosition, box());
        for (BlockPos pos : inputHatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) hatch.setController(null);
        energyHatches.clear();
        inputHatches.clear();
        outputHatches.clear();
        length = targetLength(getBlockState());
        scanFailure = null;
        scanParts.clear();
        for (PlantStructure.Cell cell : layout()) scanParts.put(cell.pos(), cell.part());
        scan = new StructureScan(layout().stream().map(PlantStructure.Cell::pos).toList());
    }

    private boolean visit(BlockPos pos, @Nullable BlockState state) {
        if (state == null) scanFailure = Component.translatable("message.iuce.plant.unloaded");
        else if (!scanParts.get(pos).accepts(state, facing())) {
            scanFailure = Component.translatable("message.iuce.plant.wrong_part", pos.toShortString(), scanParts.get(pos).sample(facing()).getBlock().getName());
        }
        else if (state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get())) energyHatches.add(pos);
        else if (state.is(ModBlocks.ITEM_INPUT_HATCH.get())) inputHatches.add(pos);
        else if (state.is(ModBlocks.ITEM_OUTPUT_HATCH.get())) outputHatches.add(pos);
        return scanFailure == null;
    }

    private void finishScan() {
        scan = null;
        scanParts.clear();
        if (scanFailure == null) {
            if (energyHatches.isEmpty()) scanFailure = Component.translatable("message.iuce.plant.no_energy_hatch");
            else if (inputHatches.isEmpty()) scanFailure = Component.translatable("message.iuce.plant.no_input_hatch");
            else if (outputHatches.isEmpty()) scanFailure = Component.translatable("message.iuce.plant.no_output_hatch");
        }
        formed = scanFailure == null;
        if (formed) {
            for (BlockPos pos : inputHatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) hatch.setController(worldPosition);
            int tier = 0;
            for (BlockPos pos : energyHatches) {
                if (level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch) tier = Math.max(tier, hatch.parallel());
            }
            // a longer mill runs more at once: up to twice as many at full length
            double stretch = isMill() ? 1 + (length - MILL_MIN) / (double) (MILL_MAX - MILL_MIN) : 1;
            parallel = (int) (tier * stretch);
        } else {
            problem = scanFailure;
        }
        BlockState state = getBlockState();
        if (state.getValue(PlantControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(PlantControllerBlock.FORMED, formed), Block.UPDATE_ALL);
        }
    }

    /** Positions of the shell that are still free, in building order. */
    public List<BlockPos> missing() {
        List<BlockPos> free = new ArrayList<>();
        if (level == null) return free;
        for (PlantStructure.Cell cell : layout()) {
            BlockPos pos = cell.pos();
            if (cell.part().canFillWithCasing() && level.isLoaded(pos) && level.getBlockState(pos).canBeReplaced()) free.add(pos);
        }
        return free;
    }

    /**
     * Puts casing from the stack into free positions of the shell and returns how many were
     * placed; the stack is not changed, the caller takes the blocks.
     */
    public int fill(ItemStack casing) {
        if (level == null || !casing.is(ModBlocks.PLANT_CASING.get().asItem())) return 0;
        int placed = 0;
        for (BlockPos pos : missing()) {
            if (placed >= casing.getCount()) break;
            level.setBlock(pos, ModBlocks.PLANT_CASING.get().joinedState(level, pos), Block.UPDATE_ALL);
            placed++;
        }
        structureDirty = true;
        return placed;
    }

    /** Bill of materials for the blueprint terminal: size, what the shell needs and what is already there. */
    public Component summary() {
        int target = targetLength(getBlockState());
        int fitted = 0, wrong = 0;
        Map<Block, Integer> needed = new java.util.LinkedHashMap<>();
        for (PlantStructure.Cell cell : layout()) {
            if (!level.isLoaded(cell.pos())) continue;
            BlockState state = level.getBlockState(cell.pos());
            if (cell.part().accepts(state, facing())) fitted++;
            else {
                needed.merge(cell.part().sample(facing()).getBlock(), 1, Integer::sum);
                if (!state.canBeReplaced()) wrong++;
            }
        }
        MutableComponent text = Component.translatable("message.iuce.blueprint.title", getBlockState().getBlock().getName(), target, layout().size());
        text.append("\n").append(Component.translatable("message.iuce.blueprint.parts", fitted, wrong));
        needed.forEach((block, count) -> text.append("\n").append(Component.translatable("message.iuce.blueprint.needed", block.getName(), count)));
        double stretch = isMill() ? 1 + (target - MILL_MIN) / (double) (MILL_MAX - MILL_MIN) : 1;
        text.append("\n").append(Component.translatable("message.iuce.blueprint.parallel", (int) (16 * stretch), (int) (64 * stretch)));
        if (isMill()) text.append("\n").append(Component.translatable("message.iuce.blueprint.resize", MILL_MIN, MILL_MAX));
        return text;
    }

    // ------------------------------------------------------------------ work

    /** True when the machine has a process for the item. */
    public boolean canProcess(ItemResource resource) {
        return formed && level instanceof ServerLevel server && ProcessChain.get(server, steps(), resource.getItem()).isPresent();
    }

    public Component describe() {
        if (scan != null) return Component.translatable("message.iuce.plant.scanning");
        if (!formed) return Component.translatable("message.iuce.plant.unformed", problem);
        if (isMill() && getMold().isEmpty()) return Component.translatable("message.iuce.plant.no_mold");
        if (pending.isEmpty()) return Component.translatable("message.iuce.plant.idle", parallel);
        if (blocked) return Component.translatable("message.iuce.plant.blocked");
        return Component.translatable(starved ? "message.iuce.plant.starved" : "message.iuce.plant.working", progress * 100 / DURATION);
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (structureDirty) beginScan();
        if (scan != null) {
            if (!scan.step(level, StructureScan.BUDGET, this::visit)) return;   // a large structure takes several ticks
            finishScan();
        }
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
        output.store("Mold", ItemResource.OPTIONAL_CODEC, getMold());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
        energyPerTick = input.getLong("EnergyPerTick").orElse(0L);
        paid = input.getLong("Paid").orElse(0L);
        pending.clear();
        input.read("Pending", Pending.CODEC.listOf()).ifPresent(pending::addAll);
        ItemResource mold = input.read("Mold", ItemResource.OPTIONAL_CODEC).orElse(ItemResource.EMPTY);
        moldSlot.set(0, mold, mold.isEmpty() ? 0 : 1);
        structureDirty = true;
    }
}
