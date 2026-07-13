package machine.storage;




import blockentity.EnergyDisplay;

import blockentity.MachineBlockEntity;

import blockentity.ModBlockEntities;

import energy.EUEnergyStorage;

import energy.EUEnergyStorageImpl;

import energy.EUVoltage;

import machine.StorageGuiProvider;

import machine.storage.menu.StorageMenu;

import network.performance.MachinePriority;

import net.minecraft.core.BlockPos;

import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.storage.ValueInput;

import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;




public class MfeBlockEntity extends MachineBlockEntity implements EnergyDisplay, StorageGuiProvider {




    private final EUEnergyStorageImpl energyStorage;


    private final EUVoltage voltage;


    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(10) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };




    public MfeBlockEntity(BlockPos pos, BlockState state) {


        this(pos, state, EUVoltage.HV, 4000000);


    }




    public MfeBlockEntity(BlockPos pos, BlockState state, EUVoltage voltage, long bufferCapacity) {


        super(ModBlockEntities.MFE.get(), pos, state);


        this.voltage = voltage;


        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);


    }


    @Override


    public void tick() {
        super.tick();


        if (level == null || level.isClientSide()) {


            return;


        }


    }


    @Override


    public String getMachineName() {


        return "MFE";


    }


    @Override


    public EUEnergyStorage getEnergyStorage() {


        return energyStorage;


    }


    @Override
    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    private static final Component DISPLAY_NAME = Component.literal("MFE");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageMenu(containerId, playerInventory, this);
    }


    public EUVoltage getVoltage() {


        return voltage;


    }


    @Override
    public MachinePriority getMachinePriority() {
        return MachinePriority.CRITICAL;
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


        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));


        itemHandler.deserialize(input);


    }

}
