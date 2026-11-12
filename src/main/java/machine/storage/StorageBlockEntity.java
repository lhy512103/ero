package machine.storage;

import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import machine.StorageGuiProvider;
import machine.storage.menu.StorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import network.performance.MachinePriority;

/**
 * Shared logic of the energy storage blocks. Power moves in and out through the transfer manager;
 * item charging is handled by {@link MachineBlockEntity#tick()}. Tiers differ only in voltage,
 * capacity and names.
 */
public abstract class StorageBlockEntity extends MachineBlockEntity implements EnergyDisplay, StorageGuiProvider {

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final String machineName;
    private final Component displayName;
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(10) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };

    protected StorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 EUVoltage voltage, long capacity, String machineName, String displayName) {
        super(type, pos, state);
        this.voltage = voltage;
        this.machineName = machineName;
        this.displayName = Component.literal(displayName);
        this.energyStorage = new EUEnergyStorageImpl(capacity, voltage);
    }

    @Override public String getMachineName() { return machineName; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    @Override public ItemStacksResourceHandler getItemHandler() { return itemHandler; }
    @Override public MachinePriority getMachinePriority() { return MachinePriority.CRITICAL; }

    public EUVoltage getVoltage() { return voltage; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energyStorage::setEnergy);
        itemHandler.deserialize(input);
    }
}
