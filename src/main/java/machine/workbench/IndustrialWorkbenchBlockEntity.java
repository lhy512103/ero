package machine.workbench;

import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import machine.workbench.menu.IndustrialWorkbenchMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class IndustrialWorkbenchBlockEntity extends MachineBlockEntity implements EnergyDisplay, ProcessingDisplay, MenuProvider {

    /** 合成输入槽 (0-8)、输出槽 (9)、升级槽 (10-13)、供电槽 (14)、额外物品栏 (15-32) */
    private static final int CRAFTING_START = 0;
    private static final int CRAFTING_SIZE = 9;
    private static final int OUTPUT_SLOT = 9;
    private static final int UPGRADE_START = 10;
    private static final int UPGRADE_SIZE = 4;
    private static final int BATTERY_SLOT = 14;
    private static final int EXTRA_INVENTORY_START = 15;
    private static final int EXTRA_INVENTORY_SIZE = 18;
    private static final int PROCESS_TIME = 200;

    private final EUEnergyStorageImpl energyStorage;
    private final EUVoltage voltage;
    private final ItemStacksResourceHandler itemHandler;

    private int progress = 0;
    private int maxProgress = PROCESS_TIME;
    @Nullable
    private CraftingRecipe cachedRecipe = null;
    private ItemStack cachedOutput = ItemStack.EMPTY;
    private boolean recipeChecked;
    private Object cachedRecipeMap;

    private static final Component DISPLAY_NAME = Component.literal("工业工作台");

    public IndustrialWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_WORKBENCH.get(), pos, state);
        this.voltage = EUVoltage.LV;
        this.energyStorage = new EUEnergyStorageImpl(1000, voltage);

        // 9 合成输入 + 1 输出 + 4 升级 + 1 供电 + 18 额外物品栏 = 33
        this.itemHandler = new ItemStacksResourceHandler(33) {
            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
                // 输入变化时重置配方缓存
                if (index >= CRAFTING_START && index < CRAFTING_START + CRAFTING_SIZE) {
                    cachedRecipe = null;
                    recipeChecked = false;
                    cachedOutput = ItemStack.EMPTY;
                    progress = 0;
                }
            }
        };
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) return;

        // 供电槽：红石粉转化为能量
        processPowerSlot();

        // 查找配方
        Object recipeMap = RecipeHelper.getRecipeManager(level).recipeMap();
        if (!recipeChecked || cachedRecipeMap != recipeMap) {
            cachedRecipeMap = recipeMap;
            findRecipe();
        }

        if (cachedRecipe != null && !cachedOutput.isEmpty()) {
            // 检查输出槽是否可用
            ItemResource outputRes = itemHandler.getResource(OUTPUT_SLOT);
            int outputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);
            if (!outputRes.isEmpty() && !outputRes.equals(ItemResource.of(cachedOutput))) {
                progress = 0;
                return;
            }
            if (outputAmount + cachedOutput.getCount() > cachedOutput.getMaxStackSize()) {
                progress = 0;
                return;
            }

            progress++;
            if (progress >= maxProgress) {
                if (tryCraft()) {
                    progress = 0;
                    cachedRecipe = null;
                    recipeChecked = false;
                    cachedOutput = ItemStack.EMPTY;
                }
            }
        } else {
            progress = 0;
        }
    }

    private static final long REDSTONE_ENERGY = 500;

    private void processPowerSlot() {
        ItemResource batteryRes = itemHandler.getResource(BATTERY_SLOT);
        if (batteryRes.isEmpty()) return;
        if (!batteryRes.getItem().equals(net.minecraft.world.item.Items.REDSTONE)) return;
        if (energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) return;

        try (Transaction tx = Transaction.openRoot()) {
            int extracted = itemHandler.extract(BATTERY_SLOT, batteryRes, 1, tx);
            if (extracted > 0) {
                energyStorage.addEnergy(REDSTONE_ENERGY);
                tx.commit();
            }
        }
    }

    private void findRecipe() {
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < CRAFTING_SIZE; i++) {
            ItemResource res = itemHandler.getResource(CRAFTING_START + i);
            inputs.add(res.isEmpty() ? ItemStack.EMPTY : res.toStack(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, inputs);
        cachedRecipe = RecipeHelper.findCraftingRecipe(level, inputs);
        if (cachedRecipe == null) {
            cachedRecipe = RecipeHelper.getRecipeManager(level).getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).orElse(null);
        }
        cachedOutput = cachedRecipe != null ? cachedRecipe.assemble(input) : ItemStack.EMPTY;
        recipeChecked = true;
    }

    private boolean tryCraft() {
        if (cachedRecipe == null || cachedOutput.isEmpty()) return false;

        ItemResource outputResource = ItemResource.of(cachedOutput);
        int outputCount = cachedOutput.getCount();
        List<ItemStack> grid = new ArrayList<>(CRAFTING_SIZE);
        for (int i = 0; i < CRAFTING_SIZE; i++) grid.add(itemHandler.getResource(i).toStack(itemHandler.getAmountAsInt(i)));
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, grid);
        if (!cachedRecipe.matches(positioned.input(), level)) { recipeChecked = false; return false; }
        var remainders = cachedRecipe.getRemainingItems(positioned.input());
        // 记录合成前每个槽的物品类型，用于自动补充
        ItemResource[] slotResources = new ItemResource[CRAFTING_SIZE];
        for (int i = 0; i < CRAFTING_SIZE; i++) {
            slotResources[i] = itemHandler.getResource(CRAFTING_START + i);
        }

        try (Transaction tx = Transaction.openRoot()) {
            // 检查输出槽
            ItemResource currentOutputRes = itemHandler.getResource(OUTPUT_SLOT);
            int currentOutputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);
            if (!currentOutputRes.isEmpty() && !currentOutputRes.equals(outputResource)) return false;
            if (currentOutputAmount + cachedOutput.getCount() > cachedOutput.getMaxStackSize()) return false;

            // 消耗输入
            for (int i = 0; i < CRAFTING_SIZE; i++) {
                ItemResource res = itemHandler.getResource(CRAFTING_START + i);
                if (!res.isEmpty()) {
                    int extracted = itemHandler.extract(CRAFTING_START + i, res, 1, tx);
                    if (extracted < 1) return false;
                }
            }

            // 插入输出
            int inserted = itemHandler.insert(OUTPUT_SLOT, outputResource, outputCount, tx);
            if (inserted != outputCount) return false;

            // Return remainders to their original (untrimmed) grid slot, then the buffer.
            // If there is no room, abort the whole craft; never delete containers or duplicate output.
            for (int i = 0; i < remainders.size(); i++) {
                ItemStack remainder = remainders.get(i);
                if (remainder.isEmpty()) continue;
                int x = i % positioned.input().width() + positioned.left();
                int y = i / positioned.input().width() + positioned.top();
                ItemResource resource = ItemResource.of(remainder);
                int left = remainder.getCount() - itemHandler.insert(x + y * 3, resource, remainder.getCount(), tx);
                for (int slot = EXTRA_INVENTORY_START; slot < EXTRA_INVENTORY_START + EXTRA_INVENTORY_SIZE && left > 0; slot++) {
                    left -= itemHandler.insert(slot, resource, left, tx);
                }
                if (left != 0) return false;
            }
            tx.commit();
        }

        // 自动补充：从额外物品栏提取相同物品填充空的合成槽
        for (int i = 0; i < CRAFTING_SIZE; i++) {
            if (slotResources[i].isEmpty()) continue;
            if (!itemHandler.getResource(CRAFTING_START + i).isEmpty()) continue;

            // 在额外物品栏中查找相同物品
            for (int j = EXTRA_INVENTORY_START; j < EXTRA_INVENTORY_START + EXTRA_INVENTORY_SIZE; j++) {
                ItemResource extraRes = itemHandler.getResource(j);
                if (extraRes.equals(slotResources[i])) {
                    try (Transaction tx = Transaction.openRoot()) {
                        int extracted = itemHandler.extract(j, extraRes, 1, tx);
                        if (extracted > 0) {
                            itemHandler.insert(CRAFTING_START + i, extraRes, 1, tx);
                            tx.commit();
                        }
                    }
                    break;
                }
            }
        }

        return true;
    }

    @Override
    public String getMachineName() { return "工业工作台"; }

    @Override
    public Component getDisplayName() { return DISPLAY_NAME; }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IndustrialWorkbenchMenu(containerId, playerInventory, this);
    }

    @Override
    public ItemStacksResourceHandler getItemHandler() { return itemHandler; }

    @Override
    public int getUpgradeSlotCount() { return UPGRADE_SIZE; }

    @Override
    public int getUpgradeSlotStart() { return UPGRADE_START; }

    public EUEnergyStorage getEnergyStorage() { return energyStorage; }
    public EUVoltage getVoltage() { return voltage; }

    public int getCraftingStart() { return CRAFTING_START; }
    public int getCraftingSize() { return CRAFTING_SIZE; }
    public int getOutputSlot() { return OUTPUT_SLOT; }
    public int getBatterySlot() { return BATTERY_SLOT; }
    public int getExtraInventoryStart() { return EXTRA_INVENTORY_START; }
    public int getExtraInventorySize() { return EXTRA_INVENTORY_SIZE; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }

    // ProcessingDisplay 接口
    @Override
    public boolean isProcessing() { return progress > 0; }

    @Override
    public float getProcessingProgress() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }

    @Override
    public String getProgressLabel() { return "合成进度"; }

    @Override protected boolean canReceiveEU() { return true; }
    @Override protected boolean canExtractEU() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energyStorage.getEnergyStored());
        output.putInt("progress", progress);
        itemHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("energy").ifPresent(energy -> energyStorage.setEnergy(energy));
        progress = input.getInt("progress").orElse(0);
        itemHandler.deserialize(input);
        cachedRecipe = null;
        cachedOutput = ItemStack.EMPTY;
    }
}
