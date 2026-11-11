package machine.electrolyzer;

import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import fluid.FluidProvider;
import item.OverclockerUpgradeItem;
import machine.ElectrolyzerGuiProvider;
import machine.electrolyzer.menu.ElectrolyzerMenu;
import recipe.MachineRecipe;
import recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/**
 * 电解机方块实体
 * <p>
 * 默认配置：
 * - 电压等级：MV (128 EU/t)
 * - 内部缓冲：8000 EU
 * - 基础加工时间：200 tick
 * - 流体输入槽：1 个
 * - 流体输出槽：3 个
 * - 升级槽：4 个
 * </p>
 */
public class ElectrolyzerBlockEntity extends MachineBlockEntity
        implements EnergyDisplay, ProcessingDisplay, ElectrolyzerGuiProvider, FluidProvider {

    private static final int FLUID_INPUT_SLOT = 0;
    private static final int FLUID_OUTPUT1_SLOT = 1;
    private static final int FLUID_OUTPUT2_SLOT = 2;
    private static final int FLUID_OUTPUT3_SLOT = 3;
    private static final int BASE_PROCESS_TIME = 200;
    private static final int MAX_STORED_FLUID = 16000; // mB
    private static final int WATER_PER_OPERATION = 1000;

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final ItemStacksResourceHandler itemHandler;
    private int progress = 0;
    private int maxProgress = BASE_PROCESS_TIME;

    /** 内部存储的流体量（mB） */
    private int storedFluid = 0;

    @Nullable
    private MachineRecipe cachedRecipe;
    private ItemResource cachedInputResource = ItemResource.EMPTY;
    private int cachedOverclockerCount = -1;

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER.get(), pos, state);
        this.voltage = EUVoltage.MV;  // 电解需要较高电压
        this.energyStorage = new EUEnergyStorageImpl(8000, voltage);

        // 1 流体输入槽 + 3 流体输出槽 + 4 升级槽
        this.itemHandler = new ItemStacksResourceHandler(8) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        processUpgrades();
        pullFluidFromAdjacent();
        processRecipe();
    }

    private void processRecipe() {
        if (level == null || level.isClientSide()) return;

        ItemResource inputResource = itemHandler.getResource(FLUID_INPUT_SLOT);
        int inputAmount = itemHandler.getAmountAsInt(FLUID_INPUT_SLOT);

        if (inputResource.isEmpty() || inputAmount <= 0) {
            progress = 0;
            cachedRecipe = null;
            cachedInputResource = ItemResource.EMPTY;
            return;
        }

        if (!inputResource.equals(cachedInputResource)) {
            cachedInputResource = inputResource;
            cachedRecipe = RecipeHelper.findMachineRecipe(level, "electrolyzer", inputResource.toStack(1));
        }

        if (cachedRecipe == null) {
            progress = 0;
            return;
        }

        if (cachedOverclockerCount < 0) {
            cachedOverclockerCount = OverclockerUpgradeItem.countOverclockers(this);
        }

        int processTime = OverclockerUpgradeItem.getOverclockedProcessTime(
            cachedRecipe.getProcessTime(), cachedOverclockerCount);
        long energyPerTick = OverclockerUpgradeItem.getOverclockedEnergyConsumption(
            cachedRecipe.getEnergyPerTick(), cachedOverclockerCount);
        maxProgress = processTime;

        if (storedFluid < WATER_PER_OPERATION && progress == 0) return;

        long consumed = energyStorage.removeEnergy(energyPerTick);
        if (consumed <= 0) return;

        progress++;

        if (progress >= maxProgress) {
            if (tryProcessOutput()) {
                storedFluid -= WATER_PER_OPERATION;
                progress = 0;
                setChanged();
            }
        }
    }

    private boolean tryProcessOutput() {
        if (cachedRecipe == null) return false;

        ItemStack outputStack = cachedRecipe.getOutput();
        ItemResource outputResource = ItemResource.of(outputStack);

        try (Transaction tx = Transaction.openRoot()) {
            ItemResource inputResource = itemHandler.getResource(FLUID_INPUT_SLOT);
            int extracted = itemHandler.extract(FLUID_INPUT_SLOT, inputResource, cachedRecipe.getInputCount(), tx);
            if (extracted < cachedRecipe.getInputCount()) return false;

            int inserted = itemHandler.insert(FLUID_OUTPUT1_SLOT, outputResource, outputStack.getCount(), tx);
            if (inserted < outputStack.getCount()) return false;

            // 副产物依次尝试放入输出槽 2、3
            if (cachedRecipe.hasExtraResults()) {
                for (MachineRecipe.ExtraResult extra : cachedRecipe.getExtraResults()) {
                    if (level.getRandom().nextFloat() >= extra.chance()) continue;
                    ItemStack extraStack = extra.create();
                    ItemResource extraResource = ItemResource.of(extraStack);
                    int remaining = extraStack.getCount();
                    remaining -= itemHandler.insert(FLUID_OUTPUT2_SLOT, extraResource, remaining, tx);
                    if (remaining > 0) {
                        itemHandler.insert(FLUID_OUTPUT3_SLOT, extraResource, remaining, tx);
                    }
                }
            }

            tx.commit();
        }

        return true;
    }

    /**
     * 当蓄水条未满时，从相邻的蓄水槽（如泵）抽取流体到液体输入槽
     */
    private void pullFluidFromAdjacent() {
        if (level == null || level.isClientSide()) return;
        if (storedFluid >= MAX_STORED_FLUID) return;

        for (Direction direction : Direction.values()) {
            if (storedFluid >= MAX_STORED_FLUID) break;

            BlockPos adjPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(adjPos);
            if (be instanceof FluidProvider provider && provider.getStoredFluid() > 0) {
                int needed = MAX_STORED_FLUID - storedFluid;
                int transferAmount = Math.min(needed, 1000);
                int extracted = provider.extractFluid(transferAmount);
                if (extracted > 0) {
                    storedFluid += extracted;
                    setChanged();
                }
            }
        }
    }

    @Override
    public String getMachineName() { return "电解机"; }

    private static final Component DISPLAY_NAME = Component.literal("电解机");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectrolyzerMenu(containerId, playerInventory, this);
    }

    public EUEnergyStorage getEnergyStorage() { return energyStorage; }

    public EUVoltage getVoltage() { return voltage; }

    @Override
    public ItemStacksResourceHandler getItemHandler() { return itemHandler; }

    public boolean isWorking() { return progress > 0; }

    public int getProgress() { return progress; }

    public int getMaxProgress() { return maxProgress; }

    // ProcessingDisplay 接口
    @Override
    public boolean isProcessing() { return progress > 0; }

    @Override
    public float getProcessingProgress() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }

    public int getStoredFluid() { return storedFluid; }

    public int getMaxStoredFluid() { return MAX_STORED_FLUID; }

    @Override
    public int getUpgradeSlotCount() { return 4; }

    @Override
    public int getUpgradeSlotStart() { return 4; } // 1 input + 3 output

    // FluidProvider 接口实现
    @Override
    public int extractFluid(int maxAmount) {
        int extracted = Math.min(maxAmount, storedFluid);
        storedFluid -= extracted;
        setChanged();
        return extracted;
    }

    @Override
    public int insertFluid(int maxAmount) {
        int canInsert = Math.min(maxAmount, MAX_STORED_FLUID - storedFluid);
        storedFluid += canInsert;
        setChanged();
        return canInsert;
    }

    private static final ItemStack WATER_STACK = new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);

    @Override
    public ItemStack getFluidItem() {
        return storedFluid > 0 ? WATER_STACK : ItemStack.EMPTY;
    }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        output.putInt("storedFluid", storedFluid);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        progress = input.getInt("progress").orElse(0);
        maxProgress = input.getInt("maxProgress").orElse(BASE_PROCESS_TIME);
        storedFluid = input.getInt("storedFluid").orElse(0);
        itemHandler.deserialize(input);
        cachedRecipe = null;
        cachedInputResource = ItemResource.EMPTY;
        cachedOverclockerCount = -1;
    }
}
