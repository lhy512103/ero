package machine.electrolyzer;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import fluid.FluidProvider;
import fluid.MachineTank;
import net.minecraft.world.level.material.Fluids;
import machine.ElectrolyzerGuiProvider;
import machine.PoweredMachineBlockEntity;
import machine.electrolyzer.menu.ElectrolyzerMenu;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;
import recipe.MachineRecipe;
import recipe.RecipeHelper;

import javax.annotation.Nullable;

/**
 * 电解机：MV，缓冲 8000 EU。槽位：1 输入 + 3 输出 + 4 升级。
 * 用水量由配方决定（现有配方每次 1000 mB），内部最多存 16000 mB，会从相邻的供水设备（如泵）取水。
 */
public class ElectrolyzerBlockEntity extends PoweredMachineBlockEntity implements ElectrolyzerGuiProvider, FluidProvider {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT1_SLOT = 1;
    private static final int OUTPUT2_SLOT = 2;
    private static final int OUTPUT3_SLOT = 3;
    private static final int MAX_STORED_FLUID = 16000;
    private static final Component DISPLAY_NAME = Component.translatable("block.iuce.electrolyzer");

    /** 内部水箱 */
    private final MachineTank tank = MachineTank.of(Fluids.WATER, MAX_STORED_FLUID, this::setChanged);

    @Nullable
    private MachineRecipe cachedRecipe;
    private ItemResource cachedInputResource = ItemResource.EMPTY;

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER.get(), pos, state, EUVoltage.MV, 8000, 8, 4, 200);
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

        ItemResource inputResource = itemHandler.getResource(INPUT_SLOT);
        if (inputResource.isEmpty() || itemHandler.getAmountAsInt(INPUT_SLOT) <= 0) {
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

        maxProgress = overclockedTime(cachedRecipe.getProcessTime());
        long energyPerTick = overclockedEnergy(cachedRecipe.getEnergyPerTick());

        // 水不够时不开始新的一轮；已经开始的继续做完
        // 用水量由配方的 fluid_input 决定；配方要的不是水时无法加工
        int water = cachedRecipe.fluidInputAmount(Fluids.WATER);
        if (water < 0 || (tank.getAmount() < water && progress == 0)) return;
        if (energyStorage.removeEnergy(energyPerTick) <= 0) return;

        progress++;
        if (progress >= maxProgress && tryProcessOutput()) {
            tank.drain(water);
            progress = 0;
            setChanged();
        }
    }

    private boolean tryProcessOutput() {
        if (cachedRecipe == null) return false;

        ItemStack outputStack = cachedRecipe.getOutput();
        ItemResource outputResource = ItemResource.of(outputStack);

        try (Transaction tx = Transaction.openRoot()) {
            ItemResource inputResource = itemHandler.getResource(INPUT_SLOT);
            if (itemHandler.extract(INPUT_SLOT, inputResource, cachedRecipe.getInputCount(), tx) < cachedRecipe.getInputCount()) return false;
            if (itemHandler.insert(OUTPUT1_SLOT, outputResource, outputStack.getCount(), tx) < outputStack.getCount()) return false;

            // 副产物依次尝试放入输出槽 2、3
            if (cachedRecipe.hasExtraResults()) {
                for (MachineRecipe.ExtraResult extra : cachedRecipe.getExtraResults()) {
                    if (level.getRandom().nextFloat() >= extra.chance()) continue;
                    ItemStack extraStack = extra.create();
                    ItemResource extraResource = ItemResource.of(extraStack);
                    int remaining = extraStack.getCount();
                    remaining -= itemHandler.insert(OUTPUT2_SLOT, extraResource, remaining, tx);
                    if (remaining > 0) itemHandler.insert(OUTPUT3_SLOT, extraResource, remaining, tx);
                }
            }

            tx.commit();
        }
        return true;
    }

    /** 水箱未满时，从相邻的供水设备取水，每个方向每 tick 最多 1000 mB */
    private void pullFluidFromAdjacent() {
        if (level == null || level.isClientSide()) return;
        for (Direction direction : Direction.values()) {
            if (tank.getSpace() <= 0) break;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(direction));
            if (be instanceof FluidProvider provider && provider.getStoredFluid() > 0) {
                // 先确认自己装得下，再从对方取，避免水在途中丢失
                int wanted = Math.min(tank.getSpace(), 1000);
                int extracted = provider.getTankFluid() == Fluids.WATER ? provider.extractFluid(wanted) : 0;
                if (extracted > 0) tank.fill(Fluids.WATER, extracted);
            }
        }
    }

    @Override public String getMachineName() { return "电解机"; }
    @Override public Component getDisplayName() { return DISPLAY_NAME; }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectrolyzerMenu(containerId, playerInventory, this);
    }

    @Override public int[] getInputSlots() { return new int[]{INPUT_SLOT}; }
    @Override public int[] getOutputSlots() { return new int[]{OUTPUT1_SLOT, OUTPUT2_SLOT, OUTPUT3_SLOT}; }

    @Override public MachineTank getTank() { return tank; }


    @Override
    protected void saveExtra(ValueOutput output) {
        tank.save(output, "tank");
    }

    @Override
    protected void loadExtra(ValueInput input) {
        tank.load(input, "tank", "storedFluid", Fluids.WATER, 1);
        cachedRecipe = null;
        cachedInputResource = ItemResource.EMPTY;
    }
}
