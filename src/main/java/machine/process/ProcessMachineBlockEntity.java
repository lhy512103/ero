package machine.process;

import fluid.MachineTank;
import fluid.MachineTanksHandler;
import fluid.MultiTankProvider;
import machine.PoweredMachineBlockEntity;
import machine.utility.Utility;
import machine.utility.UtilityConsumer;
import machine.utility.UtilityNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import recipe.ProcessRecipe;
import recipe.cache.ProcessRecipeCache;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The block entity of every data-driven process machine. The slots and tanks come from the
 * block's {@link ProcessMachineDef}; recipes are {@code iuce:process} recipes for that machine id.
 * <p>
 * A recipe runs when its inputs are present and all of its products fit. Inputs are taken and
 * products added in one step when the timer finishes, so pulling an input out halfway only resets
 * the timer. When nothing can run, {@link #getStatusKey()} names the first thing that is missing.
 * </p>
 */
public class ProcessMachineBlockEntity extends PoweredMachineBlockEntity implements MenuProvider, MultiTankProvider, UtilityConsumer {

    /** Reasons a machine is not running, in the order they are checked. Index = sync id. */
    public static final List<String> REASONS = List.of("", "no_recipe", "no_card", "wrong_card", "no_catalyst",
        "missing_input", "missing_utility", "missing_segment", "output_full", "tank_full", "needs_drying");
    public static final List<String> MOLDS = List.of("crate", "pail", "pail_lid", "brick", "fitting", "housing", "emitter", "lamp_shade",
        "pipe_die", "film_die", "coating_die", "belt_mold", "seal_mold");
    public static String toolItem(int selection) {
        String tool = MOLDS.get(selection);
        return selection < 8 ? tool + "_mold" : tool;
    }
    private int mold;
    public int mold() { return mold; }
    public void nextMold() {
        mold = (mold + 1) % (def.id().equals("single_screw_extruder") ? 2 : MOLDS.size());
        current = null;
        progress = 0;
        setChanged();
    }

    private final ProcessMachineDef def;
    private final List<MachineTank> tanks = new ArrayList<>();
    private final MachineTanksHandler fluidHandler;

    @Nullable private RecipeHolder<ProcessRecipe> current;
    private boolean searchNeeded = true;
    private String reason = "";
    /** Shares of each utility the recipes currently worth running would occupy; empty while running. */
    private final EnumMap<Utility, Integer> wanted = new EnumMap<>(Utility.class);
    /** The last demand reported to the network, so an unchanged one is not reported again. */
    private final EnumMap<Utility, Integer> reported = new EnumMap<>(Utility.class);
    private boolean reprobed;
    private long lastRound = Long.MIN_VALUE;
    /** Network generation this machine last reported into, so a cleared network is told again. */
    private int seenGeneration;
    @Nullable private Utility shortUtility;
    private int freeShares;
    private int neededShares;

    public ProcessMachineBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, ((ProcessMachineBlock) state.getBlock()).def());
    }

    private ProcessMachineBlockEntity(BlockPos pos, BlockState state, ProcessMachineDef def) {
        super(ProcessMachines.BLOCK_ENTITY.get(), pos, state, def.voltage(), def.buffer(),
            def.slotCount(), def.upgradeStart(), def.baseTime());
        this.def = def;
        for (int capacity : def.fluidsIn()) tanks.add(new MachineTank(capacity, this::usesFluid, this::contentsChanged));
        for (int capacity : def.fluidsOut()) tanks.add(new MachineTank(capacity, resource -> true, this::contentsChanged));
        this.fluidHandler = new MachineTanksHandler(tanks, def.fluidsIn().size());
    }

    public ProcessMachineDef def() { return def; }

    @Override public String getMachineName() { return def.id(); }
    @Override public List<MachineTank> getTanks() { return tanks; }
    @Override public int getInputTankCount() { return def.fluidsIn().size(); }
    @Override public ResourceHandler<FluidResource> getFluidHandler() { return fluidHandler; }

    @Override public int[] getInputSlots() { return range(0, def.itemsIn()); }
    @Override public int[] getOutputSlots() { return range(def.outputStart(), def.outputStart() + def.itemsOut()); }

    @Nullable public RecipeHolder<ProcessRecipe> currentRecipe() { return current; }
    /** Why the machine is idle, as an entry of {@link #REASONS}; empty while running or with empty inputs. */
    public String reason() { return reason; }

    private void contentsChanged() {
        searchNeeded = true;
        super.setChanged();
    }

    @Override
    public void setChanged() {
        searchNeeded = true;
        super.setChanged();
    }

    /** Input tanks only take fluids that one of this machine's recipes uses. */
    private boolean usesFluid(FluidResource resource) {
        if (level == null) return false;
        for (RecipeHolder<ProcessRecipe> holder : ProcessRecipeCache.recipes(level, def.id())) {
            for (ProcessRecipe.FluidInput input : holder.value().fluids()) if (input.test(resource)) return true;
        }
        return false;
    }

    /** Pipes may also fill the catalyst slot, so a worn catalyst can be replaced without a player. */
    @Override
    protected int[] getAutomationInputSlots() {
        int[] inputs = getInputSlots();
        if (!def.catalystSlot()) return inputs;
        int[] slots = java.util.Arrays.copyOf(inputs, inputs.length + 1);
        slots[inputs.length] = def.catalystIndex();
        return slots;
    }

    @Override
    public boolean acceptsAutomationInput(int slot, ItemResource resource) {
        if (slot == def.catalystIndex()) return itemHandler.getAmountAsInt(slot) == 0 && isCatalyst(resource.toStack(1));
        if (def.id().equals("compounding_extruder") && (slot == 2) != resource.toStack(1).is(item.ModItems.CALCIUM_CARBONATE.get())) return false;
        return usesItem(resource);
    }

    @Override
    public int requestSlot(ItemResource resource) {
        if (def.id().equals("compounding_extruder") && usesItem(resource))
            return resource.toStack(1).is(item.ModItems.CALCIUM_CARBONATE.get()) ? slotFor(resource, 2) : slotFor(resource, 0, 1);
        if (usesItem(resource)) return slotFor(resource, getInputSlots());
        int catalyst = def.catalystIndex();
        if (catalyst >= 0 && itemHandler.getAmountAsInt(catalyst) == 0 && isCatalyst(resource.toStack(1))) return catalyst;
        return -1;
    }

    private boolean usesItem(ItemResource resource) {
        if (level == null || resource.isEmpty()) return false;
        ItemStack stack = resource.toStack(1);
        for (RecipeHolder<ProcessRecipe> holder : ProcessRecipeCache.recipes(level, def.id())) {
            for (ProcessRecipe.ItemInput input : holder.value().items()) if (input.test(stack)) return true;
        }
        return false;
    }

    /** Whether {@code stack} is the catalyst of one of this machine's recipes. */
    public boolean isCatalyst(ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<ProcessRecipe> holder : ProcessRecipeCache.recipes(level, def.id())) {
            if (holder.value().catalyst().map(catalyst -> catalyst.ingredient().test(stack)).orElse(false)) return true;
        }
        return false;
    }

    // ---- running ----

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        processUpgrades();
        boolean wasWorking = isWorking();

        // The network is asked before the recipe is checked, so one rebuilt this tick still answers
        // with the shares the machine already holds.
        reportUtilityDemand();

        // A shortage of utilities only stalls the run: the work already done is kept, so a steam
        // network that is rebuilt or drains for a moment does not throw away the batch.
        boolean stalled = false;
        if (current != null) {
            String why = check(current.value());
            if (why.equals("missing_utility")) {
                stalled = true;
                reason = why;
            } else if (!why.isEmpty()) {
                current = null;
                progress = 0;
                searchNeeded = true;
            }
        }
        // A machine that asked for a utility is retried once per network round even while its inputs
        // stay the same, so it starts as soon as a share frees up.
        long round = Math.floorDiv(level.getGameTime(), UtilityNetwork.PERIOD);
        if (round != lastRound) {
            lastRound = round;
            reprobed = false;
        }
        if (current == null && (searchNeeded || (reason.equals("missing_utility") && !reprobed))) search();

        if (current != null && !stalled) {
            ProcessRecipe recipe = current.value();
            maxProgress = overclockedTime(recipe.time());
            if (progress < maxProgress && payForTick(overclockedEnergy(recipe.energyPerTick()))) progress++;
            if (progress >= maxProgress) {
                finish(recipe);
                progress = 0;
                current = null;
                searchNeeded = true;
            }
        }

        reportUtilityDemand();
        if (wasWorking != isWorking()) updateRunningState();
        if (current != null && !stalled) emitRelease(current.value());
    }

    /**
     * The vent stack burns what it is throwing away. A recipe with no products is a release, and the
     * left-over is shown at the top of the tower: a gas on the flammable tag goes up as a flare, and
     * anything else as a plume of smoke. Particles come from the server, so every player sees them.
     */
    private void emitRelease(ProcessRecipe recipe) {
        if (!recipe.results().isEmpty() || !recipe.fluidResults().isEmpty()) return;
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 2 != 0) return;
        if (recipe.fluids().isEmpty()) return;
        FluidResource held = tanks.get(0).getResource();
        if (held.isEmpty() || !recipe.fluids().getFirst().test(held)) return;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
        if (held.getFluid().is(fluid.FluidTags.FLAMMABLE_GASES)) {
            server.sendParticles(ParticleTypes.FLAME, x, y + 0.15, z, 4, 0.12, 0.05, 0.12, 0.01);
            server.sendParticles(ParticleTypes.SMOKE, x, y + 0.6, z, 2, 0.1, 0.15, 0.1, 0.01);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.1, z, 1, 0.08, 0.2, 0.08, 0.005);
        } else {
            server.sendParticles(ParticleTypes.CLOUD, x, y + 0.2, z, 2, 0.1, 0.08, 0.1, 0.01);
            server.sendParticles(ParticleTypes.SMOKE, x, y + 0.7, z, 1, 0.08, 0.15, 0.08, 0.005);
        }
    }

    /**
     * Picks the first recipe that can run; otherwise remembers why the closest one cannot. A recipe
     * that only lacks utilities has those written down as this machine's demand, and the search runs
     * a second time once the network has answered, so a machine with a supplier attached starts the
     * same tick it is placed.
     */
    private void search() {
        searchNeeded = false;
        wanted.clear();
        if (scan()) return;
        if (!wanted.isEmpty()) {
            // report the demand, let the network answer, then look again with the answer in hand
            reportUtilityDemand();
            reprobed = true;
            wanted.clear();
            scan();
        }
    }

    /** One pass over the recipes; true when one of them started. Leaves the demand of the closest one. */
    private boolean scan() {
        reason = "";
        int best = 0;
        for (RecipeHolder<ProcessRecipe> holder : ProcessRecipeCache.recipes(level, def.id())) {
            if (def.id().equals("cnc_machining_center") && !holder.id().identifier().getPath().endsWith("/" + MOLDS.get(mold))) continue;
            if (def.id().equals("single_screw_extruder")) {
                String path = holder.id().identifier().getPath();
                if (path.endsWith("/hot_pe_pipe") && mold == 1 || path.endsWith("/hot_drip_tape") && mold == 0) continue;
            }
            String why = check(holder.value());
            if (why.isEmpty()) {
                current = holder;
                return true;
            }
            if (why.equals("missing_utility")) wantedOf(holder.value(), wanted);
            int rank = REASONS.indexOf(why);
            if (rank > best && rank > 1) {
                best = rank;
                reason = why;
            }
        }
        if (reason.isEmpty() && !inputsEmpty()) reason = "no_recipe";
        return false;
    }

    /** Adds the utilities of {@code recipe} to {@code into}, one share each. */
    private static void wantedOf(ProcessRecipe recipe, EnumMap<Utility, Integer> into) {
        for (String id : recipe.utilities()) {
            Utility utility = Utility.of(id);
            if (utility != null) into.merge(utility, 1, Integer::sum);
        }
    }

    /** Everything this machine would occupy right now: what it runs, or what it is waiting to run. */
    private EnumMap<Utility, Integer> currentWanted() {
        EnumMap<Utility, Integer> demand = new EnumMap<>(Utility.class);
        if (current != null) wantedOf(current.value(), demand);
        else demand.putAll(wanted);
        return demand;
    }

    /** Tells the network what this machine would use right now; a running recipe keeps its share. */
    private void reportUtilityDemand() {
        shortUtility = null;
        freeShares = 0;
        neededShares = 0;
        if (!(level instanceof ServerLevel server)) return;
        EnumMap<Utility, Integer> demand = currentWanted();
        UtilityNetwork network = UtilityNetwork.of(server);
        if (!demand.equals(reported) || network.generation() != seenGeneration) {
            network.setDemand(worldPosition, demand);
            reported.clear();
            reported.putAll(demand);
            seenGeneration = network.generation();
        }
        if (demand.isEmpty()) return;
        network.refreshIfStale();
        // remember the first utility that came up short, so the menu can name it and show the numbers
        for (Map.Entry<Utility, Integer> entry : demand.entrySet()) {
            if (network.granted(worldPosition, entry.getKey()) >= entry.getValue()) continue;
            shortUtility = entry.getKey();
            neededShares = entry.getValue();
            freeShares = network.free(worldPosition, entry.getKey());
            break;
        }
    }

    /** The utility this machine is short of, or null when it has everything it asked for. */
    @Nullable public Utility shortUtility() { return shortUtility; }
    /** Shares of {@link #shortUtility()} still free at the suppliers; the machine asked for {@link #neededShares()}. */
    public int freeShares() { return freeShares; }
    public int neededShares() { return neededShares; }

    private boolean inputsEmpty() {
        for (int slot : getInputSlots()) if (itemHandler.getAmountAsInt(slot) > 0) return false;
        for (int i = 0; i < getInputTankCount(); i++) if (!tanks.get(i).isEmpty()) return false;
        return true;
    }

    /** Empty when {@code recipe} can run now, else the first reason it cannot (see {@link #REASONS}). */
    private String check(ProcessRecipe recipe) {
        if (!recipe.segments().isEmpty()) return "missing_segment";   // only multiblock extruders have segments
        int hits = countPresentInputs(recipe);
        int needed = recipe.items().size() + recipe.fluids().size();
        if (hits == 0 && needed > 0) return "no_recipe";                // nothing of this recipe is here
        if (def.id().equals("injection_molder") && itemHandler.getResource(0).toStack(1).is(item.ModItems.PLA_INJECTION_PELLET.get())
                && !itemHandler.getResource(0).toStack(1).has(PlasticComponents.DRIED.get())) return "needs_drying";
        if (def.id().equals("compounding_extruder") && recipe.items().stream().anyMatch(i -> i.test(new ItemStack(item.ModItems.CALCIUM_CARBONATE.get())))
                && !itemHandler.getResource(2).toStack(1).is(item.ModItems.CALCIUM_CARBONATE.get())) return "missing_input";
        if (recipe.card().isPresent()) {
            if (def.cardIndex() < 0 || itemHandler.getAmountAsInt(def.cardIndex()) <= 0) return "no_card";
            if (!ProcessCards.matches(itemHandler.getResource(def.cardIndex()).toStack(1), recipe.card().get())) return "wrong_card";
        } else if (def.cardIndex() >= 0 && itemHandler.getAmountAsInt(def.cardIndex()) > 0) {
            return "wrong_card";
        }
        if (recipe.catalyst().isPresent()) {
            int slot = def.catalystIndex();
            if (slot < 0 || !recipe.catalyst().get().ingredient().test(itemHandler.getResource(slot).toStack(1))) return "no_catalyst";
        }
        if (hits < needed || !itemsAvailable(recipe) || assignTanks(recipe) == null) return "missing_input";
        if (!recipe.utilities().isEmpty() && !utilitiesGranted(recipe)) return "missing_utility";
        try (Transaction simulation = Transaction.openRoot()) {
            String full = insertProducts(recipe, simulation, false);
            if (!full.isEmpty()) return full;
        }
        return "";
    }

    /** Whether the network gave this machine one share of each utility the recipe needs this round. */
    private boolean utilitiesGranted(ProcessRecipe recipe) {
        if (!(level instanceof ServerLevel server)) return false;
        UtilityNetwork network = UtilityNetwork.of(server);
        for (String id : recipe.utilities()) {
            Utility utility = Utility.of(id);
            if (utility == null || network.granted(worldPosition, utility) < 1) return false;
        }
        return true;
    }

    /** How many of the recipe's item and fluid entries have at least something matching in the machine. */
    private int countPresentInputs(ProcessRecipe recipe) {
        int hits = 0;
        for (ProcessRecipe.ItemInput input : recipe.items()) {
            for (int slot : getInputSlots()) {
                if (input.test(itemHandler.getResource(slot).toStack(1))) {
                    hits++;
                    break;
                }
            }
        }
        for (ProcessRecipe.FluidInput input : recipe.fluids()) {
            for (int i = 0; i < getInputTankCount(); i++) {
                if (input.test(tanks.get(i).getResource())) {
                    hits++;
                    break;
                }
            }
        }
        return hits;
    }

    /** Whether every item entry can be paid from the input slots, an item counted only once. */
    private boolean itemsAvailable(ProcessRecipe recipe) {
        int[] slots = getInputSlots();
        int[] left = new int[slots.length];
        for (int i = 0; i < slots.length; i++) left[i] = itemHandler.getAmountAsInt(slots[i]);
        for (ProcessRecipe.ItemInput input : recipe.items()) {
            int needed = input.count();
            for (int i = 0; i < slots.length && needed > 0; i++) {
                if (left[i] <= 0 || !input.test(itemHandler.getResource(slots[i]).toStack(1))) continue;
                int take = Math.min(needed, left[i]);
                left[i] -= take;
                needed -= take;
            }
            if (needed > 0) return false;
        }
        return true;
    }

    /** For each fluid entry, a distinct input tank holding enough of it; null when there is none. */
    @Nullable
    private int[] assignTanks(ProcessRecipe recipe) {
        int[] assigned = new int[recipe.fluids().size()];
        boolean[] used = new boolean[getInputTankCount()];
        for (int f = 0; f < assigned.length; f++) {
            ProcessRecipe.FluidInput input = recipe.fluids().get(f);
            assigned[f] = -1;
            for (int t = 0; t < used.length; t++) {
                MachineTank tank = tanks.get(t);
                if (!used[t] && input.test(tank.getResource()) && tank.getAmount() >= input.amount()) {
                    used[t] = true;
                    assigned[f] = t;
                    break;
                }
            }
            if (assigned[f] < 0) return null;
        }
        return assigned;
    }

    /**
     * Puts the recipe's products into the output slots and tanks inside {@code transaction}.
     * With {@code roll} false every product is assumed to appear, which is how fitting is checked.
     *
     * @return empty on success, else "output_full" or "tank_full"
     */
    private String insertProducts(ProcessRecipe recipe, TransactionContext transaction, boolean roll) {
        ItemStack feed = def.itemsIn() == 0 ? ItemStack.EMPTY : itemHandler.getResource(0).toStack(1);
        for (ProcessRecipe.Output output : recipe.results()) {
            if (roll && output.chance() < 1 && level.getRandom().nextFloat() >= output.chance()) continue;
            ItemStack stack = output.create();
            if (def.id().equals("dehumidifying_dryer")) {
                stack = feed.copyWithCount(stack.getCount());
                stack.set(PlasticComponents.DRIED.get(), net.minecraft.util.Unit.INSTANCE);
            } else if (def.id().equals("injection_molder")) {
                if (stack.is(item.ModItems.SPRUE.get())) stack.set(PlasticComponents.SOURCE.get(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(feed.getItem()));
                stack.copyFrom(net.minecraft.core.component.DataComponents.DYED_COLOR, feed);
            } else if (def.id().equals("plastic_granulator")) {
                var source = feed.get(PlasticComponents.SOURCE.get());
                if (source == null) return "missing_input";
                stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(source));
                stack.copyFrom(net.minecraft.core.component.DataComponents.DYED_COLOR, feed);
            }
            if (def.id().equals("compounding_extruder") || def.id().equals("strand_pelletizer") || def.id().equals("single_screw_extruder")
                    || def.id().equals("cooling_tank") || def.id().equals("haul_off_cutter") || def.id().equals("film_winder") || def.id().equals("injection_molder")) {
                var dyeType = net.minecraft.core.component.DataComponents.DYE;
                for (int slot : getInputSlots()) {
                    var dye = itemHandler.getResource(slot).toStack(1).get(dyeType);
                    if (dye == null) continue;
                    stack.set(dyeType, dye);
                    stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(dye.getTextureDiffuseColor()));
                    if (stack.is(block.ModBlocks.CRATE.get().asItem()) || stack.is(block.ModBlocks.TOY_BRICK.get().asItem()))
                        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(block.ColoredPlasticBlock.COLOR,dye));
                    break;
                }
            }
            ItemResource resource = ItemResource.of(stack);
            int remaining = stack.getCount();
            // stack onto matching products first, then use empty slots
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot : getOutputSlots()) {
                    if ((pass == 0) == (itemHandler.getAmountAsInt(slot) == 0)) continue;
                    remaining -= itemHandler.insert(slot, resource, remaining, transaction);
                    if (remaining <= 0) break;
                }
            }
            if (remaining > 0) return "output_full";
        }
        if (catalystWearsOut(recipe)) {
            // the worn catalyst leaves as spent catalyst through an output slot, so pipes can take it away
            ItemResource spent = ItemResource.of(new ItemStack(item.ModItems.SPENT_CATALYST.get()));
            int remaining = 1;
            for (int slot : getOutputSlots()) {
                remaining -= itemHandler.insert(slot, spent, remaining, transaction);
                if (remaining <= 0) break;
            }
            if (remaining > 0) return "output_full";
        }
        for (ProcessRecipe.FluidOutput output : recipe.fluidResults()) {
            FluidResource resource = FluidResource.of(output.fluid());
            int remaining = output.amount();
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int t = getInputTankCount(); t < tanks.size() && remaining > 0; t++) {
                    if ((pass == 0) == tanks.get(t).isEmpty()) continue;
                    remaining -= tanks.get(t).insert(0, resource, remaining, transaction);
                }
            }
            if (remaining > 0) return "tank_full";
        }
        return "";
    }

    private void finish(ProcessRecipe recipe) {
        int[] assigned = assignTanks(recipe);
        if (assigned == null || !itemsAvailable(recipe)) return;
        try (Transaction transaction = Transaction.openRoot()) {
            if (!insertProducts(recipe, transaction, true).isEmpty()) return;
            for (ProcessRecipe.ItemInput input : recipe.items()) {
                int needed = input.count();
                for (int slot : getInputSlots()) {
                    if (needed <= 0) break;
                    ItemResource resource = itemHandler.getResource(slot);
                    if (!input.test(resource.toStack(1))) continue;
                    needed -= itemHandler.extract(slot, resource, needed, transaction);
                }
                if (needed > 0) return;
            }
            for (int f = 0; f < assigned.length; f++) {
                MachineTank tank = tanks.get(assigned[f]);
                int amount = recipe.fluids().get(f).amount();
                if (tank.extract(0, tank.getResource(), amount, transaction) != amount) return;
            }
            recipe.catalyst().ifPresent(catalyst -> wearCatalyst(catalyst, transaction));
            transaction.commit();
        }
    }

    /** Whether this run takes the last durability from the catalyst in the slot. */
    private boolean catalystWearsOut(ProcessRecipe recipe) {
        if (recipe.catalyst().isEmpty() || recipe.catalyst().get().damage() <= 0 || def.catalystIndex() < 0) return false;
        ItemStack stack = itemHandler.getResource(def.catalystIndex()).toStack(1);
        return stack.isDamageableItem() && stack.getDamageValue() + recipe.catalyst().get().damage() >= stack.getMaxDamage();
    }

    private void wearCatalyst(ProcessRecipe.Catalyst catalyst, TransactionContext transaction) {
        int slot = def.catalystIndex();
        if (catalyst.damage() <= 0 || slot < 0) return;
        ItemResource resource = itemHandler.getResource(slot);
        ItemStack stack = resource.toStack(1);
        if (!stack.isDamageableItem()) return;
        itemHandler.extract(slot, resource, 1, transaction);
        int damage = stack.getDamageValue() + catalyst.damage();
        if (damage >= stack.getMaxDamage()) return;
        stack.setDamageValue(damage);
        itemHandler.insert(slot, ItemResource.of(stack), 1, transaction);
    }

    @Override
    public String getStatusKey() {
        if (!isWorking() && !reason.isEmpty()) return "tooltip.iuce.status." + reason;
        return super.getStatusKey();
    }

    // ---- saving ----

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Mold", mold);
        output.putInt("ProcessLayout", 2);
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).save(output, "tank" + i);
        if (current != null) output.putString("recipe", current.id().identifier().toString());
    }

    @Override
    protected void loadExtra(ValueInput input) {
        for (int i = 0; i < tanks.size(); i++) {
            boolean old = input.getIntOr("ProcessLayout", 0) == 0;
            int index = old && def.id().equals("polymerization_reactor") ? i + 2 : i;
            int mapped = index + (input.getIntOr("ProcessLayout",0) < 2 && def.id().equals("catalyst_prep_tank") && i >= 1 ? 1 : 0);
            if (mapped < tanks.size()) input.child("tank" + i).ifPresent(child -> tanks.get(mapped).deserialize(child));
        }
        current = null;
        searchNeeded = true;
        mold = Math.clamp(input.getIntOr("Mold", 0), 0, MOLDS.size() - 1);
        if (input.getIntOr("ProcessLayout", 0) == 0) {
            int first = switch (def.id()) {
                case "polymerization_reactor", "catalyst_prep_tank" -> 1;
                case "evaporator", "distillation_column" -> 0;
                default -> -1;
            };
            if (first >= 0) {
                for (int slot = def.slotCount() - 1; slot > first; slot--)
                    itemHandler.set(slot, itemHandler.getResource(slot - 1), itemHandler.getAmountAsInt(slot - 1));
                itemHandler.set(first, ItemResource.EMPTY, 0);
            }
        }
        if (input.getIntOr("ProcessLayout",0) < 2 && def.id().equals("strand_pelletizer")) {
            for (int slot = def.slotCount()-1; slot > 0; slot--) itemHandler.set(slot,itemHandler.getResource(slot-1),itemHandler.getAmountAsInt(slot-1));
            itemHandler.set(0,ItemResource.EMPTY,0);
        }
        // the recipe is looked up again on the first tick; progress is kept if it is still the one that runs
        savedRecipe = input.getString("recipe").map(Identifier::tryParse).orElse(null);
    }

    @Nullable private Identifier savedRecipe;

    @Override
    public void onLoad() {
        super.onLoad();
        if (savedRecipe == null || level == null || level.isClientSide()) return;
        int kept = progress;
        for (RecipeHolder<ProcessRecipe> holder : ProcessRecipeCache.recipes(level, def.id())) {
            if (holder.id().identifier().equals(savedRecipe) && check(holder.value()).isEmpty()) {
                current = holder;
                progress = kept;
                break;
            }
        }
        if (current == null) progress = 0;
        savedRecipe = null;
    }

    @Override
    public BlockPos position() { return worldPosition; }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            UtilityNetwork.of(server).remove(worldPosition);
            reported.clear();
        }
        super.setRemoved();
    }

    // ---- menu ----

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ProcessMachineMenu(containerId, inventory, this);
    }
}
