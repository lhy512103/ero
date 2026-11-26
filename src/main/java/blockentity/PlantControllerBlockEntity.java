package blockentity;

import block.EnergyHatchBlock;
import block.HatchBlock;
import block.ModBlocks;
import block.PlantCasingBlock;
import block.PlantControllerBlock;
import block.PlantDrumBlock;
import block.PlantFrameBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import item.MoldItem;
import multiblock.MultiblockManager;
import multiblock.PlantStructure;
import multiblock.ProcessChain;
import multiblock.StructureScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
public class PlantControllerBlockEntity extends BlockEntity implements machine.utility.UtilitySupplier {
    private String batchId="", stageStatus="";
    private int operations, burnTicks;
    private long batchPaid;
    private final List<BlockPos> fluidIn=new ArrayList<>(),fluidOut=new ArrayList<>(),gasOut=new ArrayList<>(),service=new ArrayList<>();
    private final List<String> screwSegments=new ArrayList<>();
    private String industrial(){return PlantStructure.industrialId(getBlockState());}
    public boolean extensible(){return isMill() || getBlockState().is(ModBlocks.TWIN_SCREW_EXTRUDER.get()) || getBlockState().is(ModBlocks.COKE_OVEN_BATTERY.get());}
    public int minimumLength(){return isMill()?9:getBlockState().is(ModBlocks.TWIN_SCREW_EXTRUDER.get())?13:17;}
    public int maximumLength(){return isMill()?MILL_MAX:33;}
    /** Length step of the size control: one block for the mill, one section of four for the others. */
    public int lengthStep(){return isMill()?1:4;}
    /** Above this many stored blocks the controller can only be broken while sneaking. */
    public static final int STORAGE_WARNING = 1000;
    private final multiblock.MaterialStorage materials = new multiblock.MaterialStorage(this::setChanged);
    @Nullable private BlockState materialsFor;
    @Override public BlockPos position(){return worldPosition;}
    @Override public int shares(machine.utility.Utility u){return getBlockState().is(ModBlocks.WASTE_HEAT_POWER_PLANT.get()) && u==machine.utility.Utility.STEAM ? 64:0;}
    @Override public void setOccupied(machine.utility.Utility u,int occupied){}
    @Override public boolean ready(){return formed && burnTicks>0;}
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
    private boolean blocked, starved, clientWorking, lastSyncedWorking;

    public PlantControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLANT_CONTROLLER.get(), pos, state);
    }

    public boolean isFormed() { return formed || (level != null && level.isClientSide() && getBlockState().getValue(PlantControllerBlock.FORMED)); }
    public boolean isWorking() { return (level != null && level.isClientSide()) ? clientWorking : !pending.isEmpty() || formed && (!batchId.isEmpty() || burnTicks>0); }
    public int getProgress() { return progress; }
    public int getParallel() { return parallel; }
    public int getLength() { return length; }
    public ItemResource getMold() { return moldSlot.getResource(0); }
    public ItemStacksResourceHandler moldSlot() { return moldSlot; }
    public boolean isScanning() { return scan != null; }
    public boolean isBlocked() { return blocked; }
    public boolean isStarved() { return starved; }
    public void markStructureDirty() { structureDirty = true; }

    /** Changes target length while immediately restoring visibility of all parts of the previous length. */
    public void resize(int nextLength) {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        setFormedParts(state, false);
        formed = false;
        scan = null;
        scanParts.clear();
        level.setBlock(worldPosition, state.setValue(PlantControllerBlock.FORMED, false).setValue(PlantControllerBlock.LENGTH, nextLength), Block.UPDATE_ALL);
        structureDirty = true;
    }

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
        return PlantStructure.length(controller);
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
        setFormedParts(state, false);
        if (level != null && !getMold().isEmpty())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), swapMold(ItemStack.EMPTY));
    }

    // ------------------------------------------------------------------ structure

    private void beginScan() {
        structureDirty = false;
        MultiblockManager.register(level, worldPosition, box());
        for (BlockPos pos : inputHatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) hatch.setController(null);
        fluidIn.clear();fluidOut.clear();gasOut.clear();service.clear();screwSegments.clear();
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
        else if (state.is(ModBlocks.ENERGY_HATCH.get()) || state.is(ModBlocks.ENERGY_HATCH_HV.get()) || state.is(ModBlocks.GENERATION_HATCH.get())) energyHatches.add(pos);
        else if(state.is(ModBlocks.FLUID_INPUT_HATCH.get())) fluidIn.add(pos);
        else if(state.is(ModBlocks.FLUID_OUTPUT_HATCH.get())) fluidOut.add(pos);
        else if(state.is(ModBlocks.GAS_OUTPUT_HATCH.get())) gasOut.add(pos);
        else if(state.is(ModBlocks.MAINTENANCE_HATCH.get())) service.add(pos);
        else if(scanParts.get(pos)==PlantStructure.Part.SEGMENT) screwSegments.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().replace("_segment",""));
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
        if(scanFailure==null && screwSegments.contains("side_feed") && (screwSegments.indexOf("melting")<0 || screwSegments.indexOf("side_feed")<screwSegments.indexOf("melting"))) scanFailure=Component.translatable("message.iuce.plant.segment_order");
        if(scanFailure==null && getBlockState().is(ModBlocks.CARBIDE_FURNACE_PLANT.get()) && energyHatches.size()>4) scanFailure=Component.translatable("message.iuce.plant.too_many_hatches");
        formed = scanFailure == null;
        if (formed) {
            for (BlockPos pos : inputHatches) if (level.getBlockEntity(pos) instanceof HatchBlockEntity hatch) hatch.setController(worldPosition);
            int tier = Integer.MAX_VALUE;
            for (BlockPos pos : energyHatches) {
                if (level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch) tier = Math.min(tier, hatch.parallel());
            }
            // a longer mill runs more at once: up to twice as many at full length
            double stretch = isMill() ? 1 + (length - MILL_MIN) / (double) (MILL_MAX - MILL_MIN) : 1;
            if(industrial()!=null && extensible()) stretch=Math.min(4,1+(length-minimumLength())/4);
            parallel = (int) (tier * stretch);
        } else {
            problem = scanFailure;
        }
        BlockState state = getBlockState();
        if (state.getValue(PlantControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(PlantControllerBlock.FORMED, formed), Block.UPDATE_ALL);
            state = getBlockState();
        }
        setFormedParts(state, formed);
        structureDirty = false;
    }

    private void setFormedParts(BlockState controllerState, boolean value) {
        if (level == null || level.isClientSide()) return;
        boolean hide = value && PlantStructure.formedModel(controllerState) != null;
        Direction facing = controllerState.getValue(PlantControllerBlock.FACING);
        java.util.Set<BlockPos> own = new java.util.HashSet<>();
        for (PlantStructure.Cell cell : PlantStructure.cells(worldPosition, facing, controllerState)) {
            own.add(cell.pos());
            setHidden(cell.pos(), hide);
        }
        // Parts left over from a longer (or differently laid out) structure must not stay hidden:
        // sweep the largest box this controller can occupy and show anything that is no longer ours.
        BoundingBox box = PlantStructure.bounds(worldPosition, facing, controllerState);
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (own.contains(pos) || pos.equals(worldPosition)) continue;
            if (MultiblockManager.hiddenByOther(level, worldPosition, pos)) continue;
            setHidden(pos.immutable(), false);
        }
    }

    private void setHidden(BlockPos pos, boolean hide) {
        if (!level.isLoaded(pos)) return;
        BlockState s = level.getBlockState(pos);
        // every part block names its hide flag "formed"
        if (s.getBlock().getStateDefinition().getProperty("formed") instanceof net.minecraft.world.level.block.state.properties.BooleanProperty prop
            && !(s.getBlock() instanceof PlantControllerBlock) && s.getValue(prop) != hide) {
            level.setBlock(pos, s.setValue(prop, hide), Block.UPDATE_ALL);
        }
    }

    /** Whether this controller, while formed with an assembled model, keeps the block at {@code pos} hidden. */
    public boolean hides(BlockPos pos) {
        BlockState state = getBlockState();
        if (!formed || PlantStructure.formedModel(state) == null) return false;
        return layout().stream().anyMatch(cell -> cell.pos().equals(pos));
    }

    /** Creative-mode one-click placement: places every missing part (casings, girders, drums, hatches) and forms immediately. */
    public int assembleCreative() {
        if (level == null || level.isClientSide()) return 0;
        Direction facing = facing();
        List<PlantStructure.Cell> cells = layout();
        int placed = 0;
        for (PlantStructure.Cell cell : cells) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos)) continue;
            BlockState existing = level.getBlockState(pos);
            if (cell.part().accepts(existing, facing) || !existing.canBeReplaced()) continue;
            level.setBlock(pos, cell.part().sample(facing), Block.UPDATE_ALL);
            placed++;
        }
        // Update connected edges/joints after all parts and hatches are in place
        for (PlantStructure.Cell cell : cells) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos)) continue;
            BlockState cur = level.getBlockState(pos);
            BlockState joined = cur;
            if (cur.is(ModBlocks.PLANT_CASING.get())) joined = ModBlocks.PLANT_CASING.get().joinedState(level, pos);
            else if (cur.getBlock() instanceof block.ConnectedStructureBlock csb) joined = csb.joinedState(level, pos);
            else if (cur.getBlock() instanceof PlantDrumBlock drum) joined = drum.joinedState(cur, level, pos);
            else if (cur.getBlock() instanceof PlantFrameBlock frame) joined = frame.joinedState(cur, level, pos);
            if (!joined.equals(cur)) level.setBlock(pos, joined, Block.UPDATE_ALL);
        }
        structureDirty = true;
        if (level instanceof ServerLevel) {
            beginScan();
            if (scan != null && scan.step(level, Integer.MAX_VALUE, this::visit)) {
                finishScan();
            }
        }
        return placed;
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

    // ------------------------------------------------------------------ material storage, building and dismantling

    /** Every block the structure is made of, with how many it needs, in first-seen order. */
    public Map<net.minecraft.world.item.Item, Integer> needs() {
        Map<net.minecraft.world.item.Item, Integer> needs = new java.util.LinkedHashMap<>();
        for (PlantStructure.Cell cell : layout()) needs.merge(cell.part().sample(facing()).getBlock().asItem(), 1, Integer::sum);
        needs.remove(net.minecraft.world.item.Items.AIR);
        return needs;
    }

    /** The material store, laid out for the structure's current size. */
    public multiblock.MaterialStorage materials() {
        if (materialsFor != getBlockState()) {
            materialsFor = getBlockState();
            materials.configure(needs());
        }
        return materials;
    }

    /** Per block kind: {needed, fitted, wrong}, over the loaded part of the structure. */
    public Map<net.minecraft.world.item.Item, int[]> progressByKind() {
        Map<net.minecraft.world.item.Item, int[]> counts = new java.util.LinkedHashMap<>();
        if (level == null) return counts;
        for (PlantStructure.Cell cell : layout()) {
            var item = cell.part().sample(facing()).getBlock().asItem();
            int[] c = counts.computeIfAbsent(item, k -> new int[3]);
            c[0]++;
            if (!level.isLoaded(cell.pos())) continue;
            BlockState there = level.getBlockState(cell.pos());
            if (cell.part().accepts(there, facing())) c[1]++;
            else if (!there.canBeReplaced()) c[2]++;
        }
        return counts;
    }

    /**
     * Places every missing part it has blocks for, taking them from the material store first and the
     * player's inventory after; creative players need none. Returns {placed, still missing}.
     */
    public int[] build(net.minecraft.world.entity.player.Player player) {
        if (level == null || level.isClientSide()) return new int[]{0, 0};
        Direction facing = facing();
        var store = materials();
        boolean free = player.hasInfiniteMaterials();
        int placed = 0, missing = 0;
        for (PlantStructure.Cell cell : layout()) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos)) continue;
            BlockState existing = level.getBlockState(pos);
            if (cell.part().accepts(existing, facing) || !existing.canBeReplaced()) continue;
            BlockState part = cell.part().sample(facing);
            var item = part.getBlock().asItem();
            if (!free && store.take(item, 1) == 0 && !takeFromInventory(player, item)) { missing++; continue; }
            level.setBlock(pos, part, Block.UPDATE_ALL);
            placed++;
        }
        if (placed > 0) joinParts();
        structureDirty = true;
        return new int[]{placed, missing};
    }

    private static boolean takeFromInventory(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) { stack.shrink(1); inventory.setChanged(); return true; }
        }
        return false;
    }

    /** Updates the connected edges of every placed part after building. */
    private void joinParts() {
        for (PlantStructure.Cell cell : layout()) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos)) continue;
            BlockState cur = level.getBlockState(pos);
            BlockState joined = cur;
            if (cur.is(ModBlocks.PLANT_CASING.get())) joined = ModBlocks.PLANT_CASING.get().joinedState(level, pos);
            else if (cur.getBlock() instanceof block.ConnectedStructureBlock csb) joined = csb.joinedState(level, pos);
            else if (cur.getBlock() instanceof PlantDrumBlock drum) joined = drum.joinedState(cur, level, pos);
            else if (cur.getBlock() instanceof PlantFrameBlock frame) joined = frame.joinedState(cur, level, pos);
            if (!joined.equals(cur)) level.setBlock(pos, joined, Block.UPDATE_ALL);
        }
    }

    /**
     * Takes down every part of the structure (the controller stays). Each block goes to the player's
     * inventory, then to the material store, and is dropped when neither has room; the contents of
     * hatches drop as they do when broken. Returns {to inventory, to store, dropped}.
     */
    public int[] dismantle(net.minecraft.world.entity.player.Player player) {
        int[] moved = new int[3];
        if (level == null || level.isClientSide()) return moved;
        Direction facing = facing();
        var store = materials();
        for (PlantStructure.Cell cell : layout()) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos)) continue;
            BlockState there = level.getBlockState(pos);
            if (!cell.part().accepts(there, facing) || there.getBlock() instanceof PlantControllerBlock) continue;
            ItemStack block = new ItemStack(there.getBlock().asItem());
            level.removeBlock(pos, false);
            if (block.isEmpty()) continue;
            if (player.getInventory().add(block)) moved[0]++;
            else if (store.add(block.getItem(), 1) == 1) moved[1]++;
            else { Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), block); moved[2]++; }
        }
        structureDirty = true;
        return moved;
    }

    /** Puts up to {@code amount} of the player's blocks of this kind into the store. */
    public int deposit(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item, int amount) {
        var store = materials();
        int moved = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && moved < amount; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item)) continue;
            int put = store.add(item, Math.min(stack.getCount(), amount - moved));
            if (put == 0) break;
            stack.shrink(put);
            moved += put;
        }
        if (moved > 0) inventory.setChanged();
        return moved;
    }

    /** Gives the player up to {@code amount} stored blocks of this kind; what does not fit stays. */
    public int withdraw(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item, int amount) {
        var store = materials();
        int moved = 0;
        while (moved < amount) {
            int batch = Math.min(item.getDefaultMaxStackSize(), Math.min(amount - moved, store.count(item)));
            if (batch <= 0) break;
            ItemStack stack = new ItemStack(item, batch);
            player.getInventory().add(stack);
            int given = batch - stack.getCount();
            if (given <= 0) break;
            store.take(item, given);
            moved += given;
        }
        return moved;
    }

    /** Steps the machine's length by {@code steps} size steps, within its range. */
    public void stepLength(int steps) {
        if (!extensible()) return;
        int current = targetLength(getBlockState());
        int next = Math.clamp(current + steps * lengthStep(), minimumLength(), maximumLength());
        if (next != current) resize(next);
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
        if(formed && industrial()!=null) return getBlockState().is(ModBlocks.WASTE_HEAT_POWER_PLANT.get()) || multiblock.PlantBatch.all(industrial()).stream().anyMatch(b->b.inputs().keySet().stream().anyMatch(k->multiblock.PlantBatch.matches(resource.toStack(1),k)));
        return formed && level instanceof ServerLevel server && ProcessChain.get(server, steps(), resource.getItem()).isPresent();
    }

    public Component describe() {
        if (scan != null) return Component.translatable("message.iuce.plant.scanning");
        if (!formed) return Component.translatable("message.iuce.plant.unformed", problem);
        if(!stageStatus.isEmpty()) return Component.translatable(stageStatus);
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
        if (industrial()!=null) { industrialTick(server);syncWorking();return; }
        if (pending.isEmpty()) {
            if (idleTicks++ % 10 == 0) tryStart(server);
            syncWorking();
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
        syncWorking();
    }

    private List<PlantTankEntity> tankPorts(List<BlockPos> positions) {
        return positions.stream().map(level::getBlockEntity).filter(PlantTankEntity.class::isInstance).map(PlantTankEntity.class::cast).toList();
    }

    private void industrialTick(ServerLevel server) {
        if(getBlockState().is(ModBlocks.WASTE_HEAT_POWER_PLANT.get())) { generatePower(server);return; }
        if(batchId.isEmpty() && idleTicks++%10!=0)return;
        var ports=new multiblock.PlantBatch.Ports(inventories(inputHatches),inventories(outputHatches),inventories(service),tankPorts(fluidIn),tankPorts(fluidOut),tankPorts(gasOut));
        var recipes=multiblock.PlantBatch.all(industrial());
        multiblock.PlantBatch selected=null;
        if(batchId.isEmpty()){
            stageStatus="";
            for(var recipe:recipes){
                if(!recipe.transfer(ports,1,false))continue;
                if(!screwSegments.containsAll(recipe.segments())){stageStatus="message.iuce.plant.missing_segments";continue;}
                int low=1,high=parallel;
                while(low<high){
                    int mid=(low+high+1)/2;
                    if(recipe.transfer(ports,mid,false))low=mid;else high=mid-1;
                }
                operations=low;batchId=recipe.id();selected=recipe;progress=0;batchPaid=0;break;
            }
        }else for(var recipe:recipes)if(recipe.id().equals(batchId)){selected=recipe;break;}
        if(selected==null){batchId="";return;}
        if(!screwSegments.containsAll(selected.segments())){stageStatus="message.iuce.plant.missing_segments";return;}
        if(!selected.transfer(ports,operations,false)){stageStatus="message.iuce.plant.batch_stalled";return;}
        long total=selected.energy()*operations;
        long due=total*Math.min(progress+1,selected.time())/selected.time();
        long limit=0;
        for(var pos:energyHatches)if(level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch)limit+=hatch.storage().getVoltage().getMaxVoltage();
        batchPaid+=drawEnergy(Math.min(limit,Math.max(0,due-batchPaid)));
        if(batchPaid>=due)progress=Math.min(selected.time(),progress+1);
        stageStatus=batchPaid<due?"message.iuce.plant.starved":"message.iuce.plant.processing";
        if(progress==selected.time() && selected.transfer(ports,operations,true)){batchId="";progress=0;batchPaid=0;stageStatus="";}
        setChanged();
    }

    private void generatePower(ServerLevel server){
        if(burnTicks==0){
            if(idleTicks++%10!=0)return;
            try(var tx=Transaction.openRoot()){
                for(var hatch:tankPorts(fluidIn))for(var tank:hatch.getTanks()){
                    var gas=tank.getResource();
                    if(burnTicks==0 && !gas.isEmpty() && gas.getFluid().is(fluid.FluidTags.FLAMMABLE_GASES) && tank.getAmount()>=250 && tank.extract(0,gas,250,tx)==250)burnTicks=100;
                }
                for(var inv:inventories(inputHatches))for(int i=0;i<inv.size() && burnTicks==0;i++){
                    var fuel=inv.getResource(i);
                    int duration=fuel.toStack(1).getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING,server.fuelValues());
                    if(duration>0 && inv.extract(i,fuel,1,tx)==1)burnTicks=duration;
                }
                if(burnTicks>0)tx.commit();
            }
        }
        if(burnTicks>0){
            for(var pos:energyHatches)if(level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch){
                var storage=hatch.storage();
                storage.setEnergy(Math.min(storage.getMaxEnergyStored(),storage.getEnergyStored()+512));hatch.setChanged();
            }
            burnTicks--;setChanged();
        }
    }

    private void syncWorking() {
        boolean working = isWorking();
        if (working != lastSyncedWorking && level != null && !level.isClientSide()) {
            lastSyncedWorking = working;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** EU stored and capacity summed over the bound energy hatches, for the control panel. */
    public long[] hatchEnergy() {
        long stored = 0, capacity = 0;
        if (level != null) for (BlockPos pos : energyHatches) {
            if (level.getBlockEntity(pos) instanceof EnergyHatchBlockEntity hatch) {
                stored += hatch.storage().getEnergyStored();
                capacity += hatch.storage().getMaxEnergyStored();
            }
        }
        return new long[]{stored, capacity};
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Working", isWorking());
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("IndustrialBatch",batchId);output.putInt("Operations",operations);output.putLong("BatchPaid",batchPaid);output.putInt("BurnTicks",burnTicks);
        output.putInt("Progress", progress);
        output.putLong("EnergyPerTick", energyPerTick);
        output.putLong("Paid", paid);
        output.store("Pending", Pending.CODEC.listOf(), pending);
        output.store("Mold", ItemResource.OPTIONAL_CODEC, getMold());
        var stored = materials.contents();
        if (!stored.isEmpty()) output.store("Materials", multiblock.MaterialStorage.CODEC, stored);
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        var stored = materials.contents();
        if (stored.isEmpty()) return;
        CompoundTag tag = new CompoundTag();
        multiblock.MaterialStorage.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stored).result()
            .ifPresent(nbt -> tag.put("IuceMaterials", nbt));
        components.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var data = components.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) return;
        var nbt = data.copyTag().get("IuceMaterials");
        if (nbt != null) multiblock.MaterialStorage.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, nbt).result().ifPresent(m -> {
            materials.restore(m);
            materialsFor = null;
        });
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        batchId=input.getStringOr("IndustrialBatch","");operations=input.getIntOr("Operations",0);batchPaid=input.getLongOr("BatchPaid",0);burnTicks=input.getIntOr("BurnTicks",0);
        progress = input.getIntOr("Progress", 0);
        energyPerTick = input.getLong("EnergyPerTick").orElse(0L);
        paid = input.getLong("Paid").orElse(0L);
        pending.clear();
        input.read("Pending", Pending.CODEC.listOf()).ifPresent(pending::addAll);
        ItemResource mold = input.read("Mold", ItemResource.OPTIONAL_CODEC).orElse(ItemResource.EMPTY);
        moldSlot.set(0, mold, mold.isEmpty() ? 0 : 1);
        materials.restore(input.read("Materials", multiblock.MaterialStorage.CODEC).orElse(Map.of()));
        materialsFor = null;
        clientWorking = input.getBooleanOr("Working", !pending.isEmpty());
        lastSyncedWorking = !pending.isEmpty();
        structureDirty = true;
    }
}
