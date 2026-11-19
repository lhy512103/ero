package machine.process;

import fluid.MachineTank;
import fluid.MachineTanksHandler;
import fluid.MultiTankProvider;
import machine.PoweredMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
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
import java.util.List;

/**
 * The block entity of every data-driven process machine. The slots and tanks come from the
 * block's {@link ProcessMachineDef}; recipes are {@code iuce:process} recipes for that machine id.
 * <p>
 * A recipe runs when its inputs are present and all of its products fit. Inputs are taken and
 * products added in one step when the timer finishes, so pulling an input out halfway only resets
 * the timer. When nothing can run, {@link #getStatusKey()} names the first thing that is missing.
 * </p>
 */
public class ProcessMachineBlockEntity extends PoweredMachineBlockEntity implements MenuProvider, MultiTankProvider {

    /** Reasons a machine is not running, in the order they are checked. Index = sync id. */
    public static final List<String> REASONS = List.of("", "no_recipe", "no_card", "wrong_card", "no_catalyst",
        "missing_input", "missing_utility", "missing_segment", "output_full", "tank_full");

    private final ProcessMachineDef def;
    private final List<MachineTank> tanks = new ArrayList<>();
    private final MachineTanksHandler fluidHandler;

    @Nullable private RecipeHolder<ProcessRecipe> current;
    private boolean searchNeeded = true;
    private String reason = "";

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

    @Override
    public boolean acceptsAutomationInput(int slot, ItemResource resource) {
        return usesItem(resource);
    }

    @Override
    public int requestSlot(ItemResource resource) {
        return usesItem(resource) ? slotFor(resource, getInputSlots()) : -1;
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

        if (current != null && !check(current.value()).isEmpty()) {
            current = null;
            progress = 0;
            searchNeeded = true;
        }
        if (current == null && searchNeeded) {
            searchNeeded = false;
            search();
        }

        if (current != null) {
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

        if (wasWorking != isWorking()) updateRunningState();
    }

    /** Picks the first recipe that can run; otherwise remembers why the closest one cannot. */
    private void search() {
        reason = "";
        int best = 0;
        for (RecipeHolder<ProcessRecipe> holder : ProcessRecipeCache.recipes(level, def.id())) {
            String why = check(holder.value());
            if (why.isEmpty()) {
                current = holder;
                return;
            }
            int rank = REASONS.indexOf(why);
            if (rank > best && rank > 1) {
                best = rank;
                reason = why;
            }
        }
        if (reason.isEmpty() && !inputsEmpty()) reason = "no_recipe";
    }

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
        if (recipe.card().isPresent()) {
            if (def.cardIndex() < 0 || itemHandler.getAmountAsInt(def.cardIndex()) <= 0) return "no_card";
            return "wrong_card";                                        // process cards arrive in M2
        }
        if (recipe.catalyst().isPresent()) {
            int slot = def.catalystIndex();
            if (slot < 0 || !recipe.catalyst().get().ingredient().test(itemHandler.getResource(slot).toStack(1))) return "no_catalyst";
        }
        if (hits < needed || !itemsAvailable(recipe) || assignTanks(recipe) == null) return "missing_input";
        if (!recipe.utilities().isEmpty()) return "missing_utility";   // utility suppliers arrive in M1
        try (Transaction simulation = Transaction.openRoot()) {
            String full = insertProducts(recipe, simulation, false);
            if (!full.isEmpty()) return full;
        }
        return "";
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
        for (ProcessRecipe.Output output : recipe.results()) {
            if (roll && output.chance() < 1 && level.getRandom().nextFloat() >= output.chance()) continue;
            ItemStack stack = output.create();
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
            if (!insertProducts(recipe, transaction, true).isEmpty()) return;
            recipe.catalyst().ifPresent(catalyst -> wearCatalyst(catalyst, transaction));
            transaction.commit();
        }
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
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).save(output, "tank" + i);
        if (current != null) output.putString("recipe", current.id().identifier().toString());
    }

    @Override
    protected void loadExtra(ValueInput input) {
        for (int i = 0; i < tanks.size(); i++) {
            int index = i;
            input.child("tank" + i).ifPresent(child -> tanks.get(index).deserialize(child));
        }
        current = null;
        searchNeeded = true;
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
