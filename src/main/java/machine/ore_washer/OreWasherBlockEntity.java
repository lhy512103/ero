package machine.ore_washer;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import fluid.FluidProvider;
import fluid.MachineTank;
import net.minecraft.world.level.material.Fluids;
import machine.OreWasherGuiProvider;
import machine.PoweredMachineBlockEntity;
import machine.ore_washer.menu.OreWasherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import recipe.MachineRecipe;
import recipe.RecipeHelper;

import javax.annotation.Nullable;

/**
 * 洗矿机：LV，缓冲 2000 EU。槽位：2 输入 + 1 输出 + 1 副产 + 4 升级。
 * 用水量由配方决定（现有配方每次 1000 mB），内部最多存 16000 mB。
 */
public class OreWasherBlockEntity extends PoweredMachineBlockEntity implements OreWasherGuiProvider, FluidProvider {

    private static final int INPUT1_SLOT = 0;
    private static final int INPUT2_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int EXTRA_OUTPUT_SLOT = 3;
    private static final int MAX_STORED_WATER = 16000;
    private static final Component DISPLAY_NAME = Component.literal("洗矿机");

    /** 内部水箱 */
    private final MachineTank tank = MachineTank.of(Fluids.WATER, MAX_STORED_WATER, this::setChanged);

    @Nullable
    private MachineRecipe cachedRecipe;
    private ItemStack cachedOutput = ItemStack.EMPTY;
    private ItemResource cachedInput1Resource = ItemResource.EMPTY;
    private ItemResource cachedInput2Resource = ItemResource.EMPTY;

    public OreWasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_WASHER.get(), pos, state, EUVoltage.LV, 2000, 8, 4, 200);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        processUpgrades();

        boolean wasWorking = isWorking();
        ItemResource input1Resource = itemHandler.getResource(INPUT1_SLOT);
        int input1Amount = itemHandler.getAmountAsInt(INPUT1_SLOT);

        if (!input1Resource.isEmpty() && input1Amount > 0) {
            ItemResource input2Resource = itemHandler.getResource(INPUT2_SLOT);

            if (!input1Resource.equals(cachedInput1Resource) || !input2Resource.equals(cachedInput2Resource)) {
                cachedInput1Resource = input1Resource;
                cachedInput2Resource = input2Resource;
                ItemStack input1Stack = input1Resource.toStack(1);
                cachedRecipe = input2Resource.isEmpty()
                    ? RecipeHelper.findMachineRecipe(level, "ore_washer", input1Stack)
                    : RecipeHelper.findMachineRecipe(level, "ore_washer", input1Stack, input2Resource.toStack(1));
                cachedOutput = cachedRecipe != null ? cachedRecipe.getOutput().copy() : ItemStack.EMPTY;
            }

            if (!cachedOutput.isEmpty() && cachedRecipe != null) {
                maxProgress = overclockedTime(cachedRecipe.getProcessTime());
                long energyPerTick = overclockedEnergy(cachedRecipe.getEnergyPerTick());

                // 水不够时不开始新的一轮；已经开始的继续做完
                // 用水量由配方的 fluid_input 决定；配方要的不是水时无法加工
                int water = cachedRecipe.fluidInputAmount(Fluids.WATER);
                boolean stalled = water < 0 || (tank.getAmount() < water && progress == 0)
                    || energyStorage.removeEnergy(energyPerTick) <= 0;
                if (stalled) {
                    if (wasWorking) {
                        updateRunningState();
                        setChanged();
                    }
                    return;
                }

                progress++;
                if (progress >= maxProgress && tryProcessOutput()) {
                    tank.drain(water);
                    progress = 0;
                }
            } else {
                progress = 0;
                cachedOutput = ItemStack.EMPTY;
                cachedRecipe = null;
            }
        } else {
            progress = 0;
            clearCache();
        }

        if (wasWorking != isWorking()) {
            updateRunningState();
            setChanged();
        }
    }

    private boolean tryProcessOutput() {
        if (cachedOutput.isEmpty() || cachedRecipe == null) return false;

        ItemStack outputStack = cachedOutput;
        ItemResource outputResource = ItemResource.of(outputStack);

        try (Transaction tx = Transaction.openRoot()) {
            ItemResource currentOutputRes = itemHandler.getResource(OUTPUT_SLOT);
            int currentOutputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);
            if (!currentOutputRes.isEmpty() && !currentOutputRes.equals(outputResource)) return false;
            if (currentOutputAmount + outputStack.getCount() > outputStack.getMaxStackSize()) return false;

            ItemResource input1Resource = itemHandler.getResource(INPUT1_SLOT);
            if (itemHandler.extract(INPUT1_SLOT, input1Resource, cachedRecipe.getInputCount(), tx) < cachedRecipe.getInputCount()) return false;

            if (cachedRecipe.hasDualInput()) {
                ItemResource input2Resource = itemHandler.getResource(INPUT2_SLOT);
                if (itemHandler.extract(INPUT2_SLOT, input2Resource, cachedRecipe.getInput2Count(), tx) < cachedRecipe.getInput2Count()) return false;
            }

            if (itemHandler.insert(OUTPUT_SLOT, outputResource, outputStack.getCount(), tx) <= 0) return false;

            // 副产物（概率产出，副产槽满则丢弃）
            if (cachedRecipe.hasExtraResults() && level != null) {
                for (MachineRecipe.ExtraResult extra : cachedRecipe.getExtraResults()) {
                    if (level.getRandom().nextFloat() >= extra.chance()) continue;
                    ItemStack extraStack = extra.create();
                    itemHandler.insert(EXTRA_OUTPUT_SLOT, ItemResource.of(extraStack), extraStack.getCount(), tx);
                }
            }

            tx.commit();
        }
        return true;
    }

    private void clearCache() {
        cachedRecipe = null;
        cachedOutput = ItemStack.EMPTY;
        cachedInput1Resource = ItemResource.EMPTY;
        cachedInput2Resource = ItemResource.EMPTY;
    }

    @Override public String getMachineName() { return "洗矿机"; }
    @Override public Component getDisplayName() { return DISPLAY_NAME; }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OreWasherMenu(containerId, playerInventory, this);
    }

    @Override public MachineTank getTank() { return tank; }

    public int getStoredWater() { return tank.getAmount(); }
    public int getMaxStoredWater() { return MAX_STORED_WATER; }


    @Override
    protected void saveExtra(ValueOutput output) {
        tank.save(output, "tank");
    }

    @Override
    protected void loadExtra(ValueInput input) {
        tank.load(input, "tank", "storedWater", Fluids.WATER, 1);
        clearCache();
    }
}
