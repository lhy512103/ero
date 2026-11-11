package machine.extractor;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import machine.ExtractorGuiProvider;
import machine.extractor.menu.ExtractorMenu;
import network.performance.MachinePriority;
import item.OverclockerUpgradeItem;
import recipe.MachineRecipe;
import recipe.RecipeHelper;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.Arrays;

public class ExtractorBlockEntity extends MachineBlockEntity implements EnergyDisplay, ProcessingDisplay, ExtractorGuiProvider {

    protected final EUEnergyStorageImpl energyStorage;
    protected final EUVoltage voltage;
    protected final int processSlots;
    protected final int upgradeSlots;
    protected final long energyConsumption;
    protected final int baseProcessTime;
    protected final ItemStacksResourceHandler itemHandler;
    protected final int[] progress;
    protected final int[] maxProgress;

    protected final MachineRecipe[] cachedRecipes;
    protected final ItemResource[] cachedInputResources;
    protected int cachedOverclockerCount = -1;

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.EXTRACTOR.get(), pos, state, EUVoltage.LV, 1, 4, 3, 1500, 200);
    }

    protected ExtractorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                    EUVoltage voltage, int processSlots, int upgradeSlots,
                                    long energyConsumption, long bufferCapacity, int baseProcessTime) {
        super(type, pos, state);
        this.voltage = voltage;
        this.processSlots = processSlots;
        this.upgradeSlots = upgradeSlots;
        this.energyConsumption = energyConsumption;
        this.baseProcessTime = baseProcessTime;
        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);

        int totalSlots = processSlots * 2 + upgradeSlots;
        this.itemHandler = new ItemStacksResourceHandler(totalSlots) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };

        this.progress = new int[processSlots];
        this.maxProgress = new int[processSlots];
        this.cachedRecipes = new MachineRecipe[processSlots];
        this.cachedInputResources = new ItemResource[processSlots];
        Arrays.fill(maxProgress, baseProcessTime);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) {
            return;
        }

        processUpgrades();

        boolean wasWorking = isWorking();
        boolean anyWorking = false;

        for (int i = 0; i < processSlots; i++) {
            int inputSlot = i;
            int outputSlot = processSlots + i;

            ItemResource inputResource = itemHandler.getResource(inputSlot);
            int inputAmount = itemHandler.getAmountAsInt(inputSlot);

            if (!inputResource.isEmpty() && inputAmount > 0 && energyStorage.getEnergyStored() > 0) {
                if (cachedRecipes[i] == null || !inputResource.equals(cachedInputResources[i])) {
                    ItemStack inputStack = inputResource.toStack(1);
                    cachedRecipes[i] = RecipeHelper.findMachineRecipe(level, "extractor", inputStack);
                    cachedInputResources[i] = inputResource;
                }

                MachineRecipe recipe = cachedRecipes[i];
                if (recipe == null) {
                    progress[i] = 0;
                    continue;
                }
                if (inputAmount < recipe.getInputCount()) {
                    progress[i] = 0;
                    continue;
                }

                if (cachedOverclockerCount < 0) {
                    cachedOverclockerCount = OverclockerUpgradeItem.countOverclockers(this);
                }
                int overclockerCount = cachedOverclockerCount;
                int overclockedProcessTime = OverclockerUpgradeItem.getOverclockedProcessTime(recipe.getProcessTime(), overclockerCount);
                long overclockedEnergyConsumption = OverclockerUpgradeItem.getOverclockedEnergyConsumption(recipe.getEnergyPerTick(), overclockerCount);
                maxProgress[i] = overclockedProcessTime;

                long consumed = energyStorage.removeEnergy(overclockedEnergyConsumption);
                if (consumed <= 0) continue;

                progress[i]++;
                anyWorking = true;

                if (progress[i] >= maxProgress[i]) {
                    ItemStack outputStack = recipe.getOutput();
                    ItemResource outputResource = ItemResource.of(outputStack);

                    try (Transaction tx = Transaction.openRoot()) {
                        int inserted = itemHandler.insert(outputSlot, outputResource, outputStack.getCount(), tx);
                        if (inserted > 0) {
                            itemHandler.extract(inputSlot, inputResource, recipe.getInputCount(), tx);
                            tx.commit();
                        }
                    }
                    progress[i] = 0;
                }
            } else if (inputResource.isEmpty() || inputAmount <= 0) {
                progress[i] = 0;
                cachedRecipes[i] = null;
                cachedInputResources[i] = ItemResource.EMPTY;
            }
        }

        if (wasWorking != anyWorking) {
            updateRunningState();
            setChanged();
        }
    }

    protected void updateRunningState() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(MachineBlock.RUNNING)) {
                boolean currentRunning = state.getValue(MachineBlock.RUNNING);
                boolean shouldRun = isWorking();
                if (currentRunning != shouldRun) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(MachineBlock.RUNNING, shouldRun));
                }
            }
        }
    }

    private static final Component DISPLAY_NAME = Component.literal("提取机");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExtractorMenu(containerId, playerInventory, this);
    }

    @Override
    public String getMachineName() { return "提取机"; }

    public boolean isWorking() {
        for (int i = 0; i < processSlots; i++) {
            if (progress[i] > 0) return true;
        }
        return false;
    }

    @Override
    public ItemStacksResourceHandler getItemHandler() { return itemHandler; }

    @Override
    public int getUpgradeSlotCount() { return upgradeSlots; }

    @Override
    public int getUpgradeSlotStart() { return 2 * processSlots; }

    @Override
    public EUEnergyStorage getEnergyStorage() { return energyStorage; }

    public EUVoltage getVoltage() { return voltage; }
    public int getProcessSlots() { return processSlots; }
    public int getUpgradeSlots() { return upgradeSlots; }
    public long getEnergyConsumption() { return energyConsumption; }
    public int getBaseProcessTime() { return baseProcessTime; }
    public int getProgress(int slot) { return progress[slot]; }
    public int getMaxProgress(int slot) { return maxProgress[slot]; }
    public void setProgress(int slot, int value) { this.progress[slot] = value; }
    public void setMaxProgress(int slot, int value) { this.maxProgress[slot] = value; }

    @Override
    public MachinePriority getMachinePriority() { return MachinePriority.HIGH; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));

        int[] savedProgress = input.getIntArray("progress").orElse(new int[processSlots]);
        int[] savedMaxProgress = input.getIntArray("maxProgress").orElse(new int[processSlots]);

        for (int i = 0; i < Math.min(processSlots, savedProgress.length); i++) {
            progress[i] = savedProgress[i];
        }
        for (int i = 0; i < Math.min(processSlots, savedMaxProgress.length); i++) {
            maxProgress[i] = savedMaxProgress[i];
        }

        itemHandler.deserialize(input);
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    public float getProcessingProgress() {
        if (!isWorking()) return 0f;
        float maxRatio = 0f;
        for (int i = 0; i < processSlots; i++) {
            if (maxProgress[i] > 0) {
                float ratio = (float) progress[i] / maxProgress[i];
                if (ratio > maxRatio) maxRatio = ratio;
            }
        }
        return maxRatio;
    }

    @Override
    public boolean isProcessing() { return isWorking(); }
}
