package machine.generator;





import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import machine.GeneratorGuiProvider;
import machine.generator.menu.GeneratorMenu;
import network.performance.MachinePriority;


import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


import net.minecraft.world.level.block.entity.BlockEntityType;


import net.minecraft.world.level.block.state.BlockState;


import net.minecraft.world.level.storage.ValueInput;


import net.minecraft.world.level.storage.ValueOutput;


import net.neoforged.neoforge.capabilities.Capabilities;


import net.neoforged.neoforge.transfer.access.ItemAccess;


import net.neoforged.neoforge.transfer.item.ItemResource;


import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;


import net.neoforged.neoforge.transfer.transaction.Transaction;


import net.neoforged.neoforge.transfer.energy.EnergyHandler;





import javax.annotation.Nullable;





public class GeneratorBlockEntity extends MachineBlockEntity implements EnergyDisplay, GeneratorGuiProvider {





    private final EUEnergyStorageImpl energyStorage;


    private final long generationRate;


    private final EUVoltage voltage;


    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(3) {


        @Override


        public boolean isValid(int slot, ItemResource resource) {


            if (slot == 2) {


                if (resource.isEmpty()) return true;


                ItemStack testStack = resource.toStack(1);


                EnergyHandler energy = testStack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(testStack));


                return energy != null;


            }


            return super.isValid(slot, resource);


        }





        @Override


        protected void onContentsChanged(int index, ItemStack previousContents) {


            setChanged();


        }


    };





    private int burnTime = 0;


    private int totalBurnTime = 0;





    public GeneratorBlockEntity(BlockPos pos, BlockState state) {


        this(ModBlockEntities.GENERATOR.get(), pos, state, EUVoltage.LV, 10, 4000);


    }





    public GeneratorBlockEntity(BlockPos pos, BlockState state, EUVoltage voltage,


                                long generationRate, long bufferCapacity) {


        this(ModBlockEntities.GENERATOR.get(), pos, state, voltage, generationRate, bufferCapacity);


    }





    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,


                                   EUVoltage voltage, long generationRate, long bufferCapacity) {


        super(type, pos, state);


        this.voltage = voltage;


        this.generationRate = generationRate;


        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);


    }


    @Override

    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) {


            return;


        }





        boolean wasBurning = isBurning();





        if (isBurning() && !energyStorage.isFull()) {


            burnTime--;


        }





        if (!isBurning() && !energyStorage.isFull()) {


            consumeFuel();


        }





        if (isBurning() && !energyStorage.isFull()) {


            generateEnergy();


        }





        chargeItem();





        if (wasBurning != isBurning()) {


            updateRunningState();


            setChanged();


        }


    }





    private void updateRunningState() {


        if (level != null && !level.isClientSide()) {


            BlockState state = getBlockState();


            if (state.hasProperty(MachineBlock.RUNNING)) {


                boolean currentRunning = state.getValue(MachineBlock.RUNNING);


                boolean shouldRun = isBurning();


                if (currentRunning != shouldRun) {


                    level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, shouldRun));


                }


            }


        }


    }


    @Override
    public String getMachineName() {
        return "发电机";
    }

    private static final Component DISPLAY_NAME = Component.translatable("block.iuce.generator");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GeneratorMenu(containerId, playerInventory, this);
    }





    public boolean isBurning() {


        return burnTime > 0;


    }





    private void consumeFuel() {


        if (tryConsumeFuel(0)) return;


        tryConsumeFuel(1);


    }





    private boolean tryConsumeFuel(int slot) {


        ItemResource resource = itemHandler.getResource(slot);


        int amount = itemHandler.getAmountAsInt(slot);


        if (!resource.isEmpty() && amount > 0) {


            ItemStack fuelStack = resource.toStack(amount);


            int fuelBurnTime = getFuelBurnTime(fuelStack);


            if (fuelBurnTime > 0) {


                totalBurnTime = fuelBurnTime;


                burnTime = fuelBurnTime;


                try (Transaction tx = Transaction.openRoot()) {


                    int extracted = itemHandler.extract(slot, resource, 1, tx);


                    if (extracted > 0) {


                        if (fuelStack.is(Items.LAVA_BUCKET)) {


                            ItemStack bucket = new ItemStack(Items.BUCKET);


                            ItemResource bucketResource = ItemResource.of(bucket);


                            itemHandler.insert(slot, bucketResource, 1, tx);


                        }


                        tx.commit();


                    }


                }


                setChanged();


                return true;


            }


        }


        return false;


    }





    /** 槽 0、1 是燃料槽；槽 2 是充电槽，不对漏斗和管道开放 */
    @Override
    public int[] getInputSlots() { return new int[]{0, 1}; }

    @Override
    public int[] getOutputSlots() { return new int[0]; }

    @Override
    public boolean acceptsAutomationInput(int slot, ItemResource resource) {
        return getFuelBurnTime(resource.toStack(1)) > 0;
    }

    private int getFuelBurnTime(ItemStack fuel) {


        if (level == null || fuel.isEmpty()) return 0;


        return fuel.getBurnTime(null, level.fuelValues());


    }





    private void generateEnergy() {


        if (!energyStorage.isFull()) {


            energyStorage.addEnergy(generationRate);


            setChanged();


        }


    }





    private void chargeItem() {
        ItemResource resource = itemHandler.getResource(2);
        int amount = itemHandler.getAmountAsInt(2);

        if (resource.isEmpty() || amount <= 0 || energyStorage.getEnergyStored() <= 0) {
            return;
        }

        ItemStack chargeStack = resource.toStack(amount);

        EnergyHandler itemEnergy = chargeStack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(chargeStack));

        if (itemEnergy == null) {
            return;
        }

        int maxTransfer = (int) Math.min(energyStorage.getEnergyStored(), generationRate);

        try (Transaction tx = Transaction.openRoot()) {
            int transferred = itemEnergy.insert(maxTransfer, tx);
            if (transferred > 0) {
                tx.commit();
                energyStorage.removeEnergy(transferred);

                // 仅在能量实际变化时才更新ItemResource
                ItemResource updatedResource = ItemResource.of(chargeStack);
                if (!updatedResource.equals(resource)) {
                    try (Transaction tx2 = Transaction.openRoot()) {
                        itemHandler.extract(2, resource, amount, tx2);
                        itemHandler.insert(2, updatedResource, amount, tx2);
                        tx2.commit();
                    }
                }
                setChanged();
            }
        }

    }





    public EUEnergyStorage getEnergyStorage() {


        return energyStorage;


    }





    public boolean isWorking() {


        return isBurning();


    }





    public long getGenerationRate() {


        return generationRate;


    }





    public EUVoltage getVoltage() {


        return voltage;


    }





    public ItemStacksResourceHandler getItemHandler() {


        return itemHandler;


    }





    public int getBurnTime() {


        return burnTime;


    }





    public void setBurnTime(int burnTime) {


        this.burnTime = burnTime;


    }





    public int getTotalBurnTime() {


        return totalBurnTime;


    }





    public void setTotalBurnTime(int totalBurnTime) {


        this.totalBurnTime = totalBurnTime;


    }





    public float getBurnProgress() {

        if (totalBurnTime == 0) {

            return 0f;

        }

        return (float) burnTime / totalBurnTime;

    }


    @Override
    public MachinePriority getMachinePriority() {
        return MachinePriority.CRITICAL;
    }


    @Override


    protected void saveAdditional(ValueOutput output) {


        super.saveAdditional(output);


        output.putLong("energy", energyStorage.getEnergyStored());


        output.putInt("burnTime", burnTime);


        output.putInt("totalBurnTime", totalBurnTime);


        itemHandler.serialize(output);


    }


    @Override


    public void loadAdditional(ValueInput input) {


        super.loadAdditional(input);


        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));


        input.getInt("burnTime").ifPresent(time -> burnTime = time);


        input.getInt("totalBurnTime").ifPresent(time -> totalBurnTime = time);


        itemHandler.deserialize(input);


    }





    @Override protected boolean canReceiveEU() { return false; }
    @Override protected boolean canExtractEU() { return true; }

}


