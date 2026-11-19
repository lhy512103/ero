package machine.utility;

import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import blockentity.TickableBlockEntity;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import fluid.FluidProvider;
import fluid.FluidTags;
import fluid.MachineTank;
import fluid.MachineTanksHandler;
import fluid.MultiTankProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * The two blocks that supply a utility. They share one class because what differs is only what
 * they run on and how many shares they give.
 * <p>
 * Burn time is counted in <em>share ticks</em>: one tick of burning at one share pays one water
 * and one unit of burn time, so a lump of coal at {@code fuelMultiplier 2} keeps eight machines
 * going for 800 ticks. The boiler only burns while it is serving somebody, so an idle boiler
 * costs nothing and the fuel already loaded stays in the firebox.
 * </p>
 * <p>
 * The cooling tower has no fuel. Each busy share takes 8 EU and 2 mB of water a tick, so eight
 * shares drink a bucket every 60 ticks and the tower only spends power when it is used.
 * </p>
 */
public class UtilityPlantBlockEntity extends MachineBlockEntity implements EnergyDisplay, FluidProvider, MultiTankProvider, UtilitySupplier, TickableBlockEntity {

    /** Which block this is. Registered blocks pick one; the definition holds the numbers. */
    public enum Kind {
        BOILER(Utility.STEAM, 8, 0, 1, 2, 20_000, 8_000, true),
        COOLING_TOWER(Utility.COOLING_WATER, 8, 8, 2, 0, 64_000, 8_000, false);

        public final Utility utility;
        /** Shares this block hands out when everything it needs is there. */
        public final int shares;
        /** EU per tick for each share in use. */
        public final long energyPerShare;
        /** Water in millibuckets per tick for each share in use. */
        public final int waterPerShare;
        /** Share ticks gained per tick of fuel burning, i.e. how far one pile of fuel stretches. */
        public final int fuelMultiplier;
        public final int tankCapacity;
        public final long energyCapacity;
        /** Whether the fuel slot is used. */
        public final boolean burnsFuel;

        Kind(Utility utility, int shares, long energyPerShare, int waterPerShare, int fuelMultiplier,
             int tankCapacity, long energyCapacity, boolean burnsFuel) {
            this.utility = utility;
            this.shares = shares;
            this.energyPerShare = energyPerShare;
            this.waterPerShare = waterPerShare;
            this.fuelMultiplier = fuelMultiplier;
            this.tankCapacity = tankCapacity;
            this.energyCapacity = energyCapacity;
            this.burnsFuel = burnsFuel;
        }
    }

    /** Millibuckets of gas one ignition of the firebox burns. */
    public static final int GAS_CHARGE = 250;
    /** Share ticks one charge of gas gives: a little more than a lump of coal, since gas has no hauling cost. */
    public static final int GAS_SHARE_TICKS = 4000;

    private final Kind kind;
    private final MachineTank tank;
    /** Flammable gas for the firebox; null on a block that does not burn fuel. */
    @Nullable private final MachineTank gas;
    private final List<MachineTank> tanks;
    private final MachineTanksHandler fluidHandler;
    private final ItemStacksResourceHandler itemHandler;
    private final EUEnergyStorageImpl energyStorage;
    private final Map<Utility, Integer> occupied = new EnumMap<>(Utility.class);

    /** Share ticks left in the firebox. */
    private int burnTime;
    private int totalBurnTime;
    /** Whether the firebox is running on gas rather than on what is in the fuel slot. */
    private boolean burningGas;

    public UtilityPlantBlockEntity(BlockPos pos, BlockState state, Kind kind) {
        super(ModBlockEntities.UTILITY_PLANT.get(), pos, state);
        this.kind = kind;
        this.tank = MachineTank.of(Fluids.WATER, kind.tankCapacity, this::setChanged);
        this.gas = kind.burnsFuel
            ? new MachineTank(kind.tankCapacity, resource -> resource.getFluid().is(FluidTags.FLAMMABLE_GASES), this::setChanged)
            : null;
        this.tanks = gas == null ? List.of(tank) : List.of(tank, gas);
        this.fluidHandler = new MachineTanksHandler(tanks, tanks.size());
        this.energyStorage = new EUEnergyStorageImpl(kind.energyCapacity, EUVoltage.LV);
        this.itemHandler = new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int slot, ItemResource resource) {
                return fuelValue(resource.toStack(1)) > 0;
            }

            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
    }

    public Kind kind() { return kind; }

    @Override public String getMachineName() { return kind.name().toLowerCase(Locale.ROOT); }
    @Override public ItemStacksResourceHandler getItemHandler() { return itemHandler; }
    @Override public int[] getInputSlots() { return kind.burnsFuel ? new int[]{0} : new int[0]; }
    @Override public int[] getOutputSlots() { return new int[0]; }
    @Override public MachineTank getTank() { return tank; }
    @Override public List<MachineTank> getTanks() { return tanks; }
    @Override public int getInputTankCount() { return tanks.size(); }
    @Override public ResourceHandler<FluidResource> getFluidHandler() { return fluidHandler; }
    @Override public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public boolean acceptsAutomationInput(int slot, ItemResource resource) { return kind.burnsFuel && fuelValue(resource.toStack(1)) > 0; }
    @Override public int requestSlot(ItemResource resource) { return acceptsAutomationInput(0, resource) ? slotFor(resource, 0) : -1; }

    // ---- the network ----

    @Override
    public int shares(Utility utility) { return utility == kind.utility ? kind.shares : 0; }

    /**
     * Whether this block can still serve one more share. The network asks before it counts the
     * shares, so a boiler that has run out of water or coal stops being handed out at the next
     * round instead of promising steam it cannot deliver.
     */
    @Override
    public boolean ready() {
        if (kind.burnsFuel) {
            return tank.getAmount() >= kind.waterPerShare && (burnTime > 0 || fuelInFirebox() || gasTank().getAmount() >= GAS_CHARGE);
        }
        return energyStorage.getEnergyStored() >= kind.energyPerShare && tank.getAmount() >= kind.waterPerShare;
    }

    /** The firebox's gas tank. Only the boiler has one; the cooling tower burns nothing. */
    @Nullable
    private MachineTank gasTank() { return gas; }

    /** Whether the firebox holds any fuel, without burning it to find out. */
    private boolean fuelInFirebox() {
        ItemResource resource = itemHandler.getResource(0);
        return !resource.isEmpty() && fuelValue(resource.toStack(1)) > 0;
    }

    @Override
    public void setOccupied(Utility utility, int occupied) {
        Integer before = this.occupied.put(utility, occupied);
        if (before == null || before != occupied) setChanged();
    }

    /** Shares of this block's utility that are in use right now. */
    public int occupied() { return occupied.getOrDefault(kind.utility, 0); }

    @Override public BlockPos position() { return worldPosition; }

    // ---- running ----

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        int used = occupied();
        boolean wasRunning = isWorking();

        if (used <= 0) {
            // nothing attached: no water, no fuel, no power
        } else if (kind.burnsFuel) {
            if (burnTime <= 0) loadFuel();
            if (burnTime >= used && tank.getAmount() >= kind.waterPerShare * used) {
                burnTime -= used;
                tank.drain(kind.waterPerShare * used);
            } else {
                dropOneShare();
            }
        } else {
            long price = kind.energyPerShare * used;
            if (energyStorage.getEnergyStored() >= price && tank.getAmount() >= kind.waterPerShare * used) {
                energyStorage.removeEnergy(price);
                tank.drain(kind.waterPerShare * used);
            } else {
                dropOneShare();
            }
        }

        if (wasRunning != isWorking()) {
            updateRunningState();
            setChanged();
        }
    }

    /**
     * Turns one share off and has the network recount right away rather than at the end of the
     * round, so a dry boiler stops glowing on the machines it can no longer serve.
     */
    private void dropOneShare() {
        int used = occupied();
        if (used <= 0) return;
        if (level instanceof ServerLevel server) UtilityNetwork.of(server).invalidate();
    }

    /**
     * Loads one charge into the firebox: a single item from the fuel slot if there is one, otherwise
     * a charge of gas. The slot comes first so a stack of coal is not left sitting while the gas
     * tank drains.
     */
    private void loadFuel() {
        if (loadSolidFuel()) {
            burningGas = false;
            return;
        }
        MachineTank gas = gasTank();
        if (gas == null || gas.getAmount() < GAS_CHARGE) return;
        if (gas.drain(GAS_CHARGE) != GAS_CHARGE) return;
        burningGas = true;
        burnTime = GAS_SHARE_TICKS;
        totalBurnTime = burnTime;
        setChanged();
    }

    /** Moves one item from the fuel slot into the firebox; false when the slot has no usable fuel. */
    private boolean loadSolidFuel() {
        ItemResource resource = itemHandler.getResource(0);
        if (resource.isEmpty()) return false;
        int value = fuelValue(resource.toStack(1));
        if (value <= 0) return false;
        try (Transaction tx = Transaction.openRoot()) {
            if (itemHandler.extract(0, resource, 1, tx) != 1) return false;
            tx.commit();
        }
        burnTime = value * kind.fuelMultiplier;
        totalBurnTime = burnTime;
        setChanged();
        return true;
    }

    private int fuelValue(ItemStack stack) {
        if (level == null || stack.isEmpty()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    private void updateRunningState() {
        var state = getBlockState();
        if (state.hasProperty(block.MachineBlock.RUNNING) && state.getValue(block.MachineBlock.RUNNING) != isWorking()) {
            level.setBlockAndUpdate(worldPosition, state.setValue(block.MachineBlock.RUNNING, isWorking()));
        }
    }

    public boolean isWorking() { return occupied() > 0; }
    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }

    // ---- bucket access to the water tank ----

    /** A full bucket empties into the tank, an empty one fills from it; false for anything else. */
    public boolean useBucket(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        if (stack.is(net.minecraft.world.item.Items.BUCKET)) {
            if (tank.getAmount() >= 1000 && tank.drain(1000) == 1000) {
                player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player,
                    new ItemStack(Fluids.WATER.getBucket())));
            } else {
                return false;
            }
        } else if (stack.getItem() instanceof net.minecraft.world.item.BucketItem bucket && bucket.content == Fluids.WATER) {
            boolean fits = tank.getSpace() >= 1000;
            if (fits && tank.fill(Fluids.WATER, 1000) == 1000 && !player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
            } else {
                return false;
            }
        } else {
            return false;
        }
        player.sendOverlayMessage(describe());
        return true;
    }

    public Component describe() {
        Component water = Component.translatable("message.iuce.utility.stored", tank.getAmount(), tank.getCapacity());
        if (gas == null) return water;
        return water.copy().append(Component.literal("  ")).append(
            Component.translatable("message.iuce.utility.gas", gas.getAmount(), gas.getCapacity()));
    }

    /** Whether the firebox is currently running on gas; for the panel and for tests. */
    public boolean burningGas() { return burningGas; }
    /** The gas tank, or null on a block that does not burn fuel. */
    @Nullable public MachineTank gasTankForDisplay() { return gas; }

    /** Fuel in hand goes into the firebox one item at a time; false when it is not fuel or the slot is full. */
    public boolean insertFuel(Player player, ItemStack stack) {
        if (!kind.burnsFuel || stack.isEmpty()) return false;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = itemHandler.insert(0, ItemResource.of(stack), 1, tx);
            if (moved <= 0) return false;
            tx.commit();
            if (!player.hasInfiniteMaterials()) stack.shrink(moved);
            return true;
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("burnTime", burnTime);
        output.putInt("totalBurnTime", totalBurnTime);
        output.putBoolean("burningGas", burningGas);
        tank.save(output, "WaterTank");
        if (gas != null) gas.save(output, "GasTank");
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energyStorage::setEnergy);
        burnTime = input.getIntOr("burnTime", 0);
        totalBurnTime = input.getIntOr("totalBurnTime", 0);
        burningGas = input.getBooleanOr("burningGas", false);
        input.child("WaterTank").ifPresent(tank::deserialize);
        if (gas != null) input.child("GasTank").ifPresent(gas::deserialize);
        itemHandler.deserialize(input);
    }

    @Override
    public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) UtilityNetwork.of(server).remove(worldPosition);
        super.setRemoved();
    }

    @Override protected boolean canReceiveEU() { return !kind.burnsFuel; }
    @Override protected boolean canExtractEU() { return false; }
}
