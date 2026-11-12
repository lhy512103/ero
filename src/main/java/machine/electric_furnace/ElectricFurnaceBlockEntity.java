package machine.electric_furnace;

import block.MachineBlock;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import machine.PoweredMachineBlockEntity;
import blockentity.ProcessingDisplay;
import blockentity.ModBlockEntities;
import energy.EUEnergyStorageImpl;
import energy.EUEnergyStorage;
import energy.EUVoltage;
import item.OverclockerUpgradeItem;
import machine.electric_furnace.menu.ElectricFurnaceMenu;
import machine.induction_furnace.InductionFurnaceBlockEntity;
import network.performance.MachinePriority;
import recipe.MachineRecipe;
import recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/**
 * 电炉方块实体
 * <p>
 * 用电能驱动的熔炉，使用原版熔炼配方。
 * 默认配置：
 * - 电压等级：LV (32 EU/t)
 * - 内部缓冲：1000 EU
 * - 能量消耗：2 EU/t
 * - 基础熔炼时间：200 tick（与原版熔炉一致）
 * <p>
 * 感应炉加成（每个相邻感应炉）：
 * - 加工速度 +50%
 * - 并行处理数 +1
 * - 能耗按加成倍率等比增加
 * - 可从感应炉抽取能量使用
 * </p>
 */
public class ElectricFurnaceBlockEntity extends PoweredMachineBlockEntity implements MenuProvider {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final long BASE_ENERGY_CONSUMPTION = 2;
    private static final int BASE_PROCESS_TIME = 200;


    /** 缓存的原版熔炼配方 */
    @Nullable
    private AbstractCookingRecipe cachedAbstractCookingRecipe;
    /** 缓存的自定义机器配方 */
    @Nullable
    private MachineRecipe cachedMachineRecipe;
    /** 缓存的熔炼输出物品 */
    private ItemStack cachedOutput = ItemStack.EMPTY;
    /** 缓存的输入 ItemResource，用于检测输入变化 */
    private ItemResource cachedInputResource = ItemResource.EMPTY;
    private Object cachedRecipeMap;


    /** 缓存的相邻感应炉数量 */
    private int cachedInductionCount = -1;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        // 1 输入槽 + 1 输出槽 + 4 升级槽
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state, EUVoltage.LV, 1000, 6, 2, BASE_PROCESS_TIME);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) {
            return;
        }

        // 处理升级效果
        processUpgrades();

        // 每 20 tick 重新扫描相邻感应炉
        if (tickCount % 20 == 0) {
            cachedInductionCount = -1;
        }

        boolean wasWorking = isWorking();

        ItemResource inputResource = itemHandler.getResource(INPUT_SLOT);
        int inputAmount = itemHandler.getAmountAsInt(INPUT_SLOT);

        if (!inputResource.isEmpty() && inputAmount > 0) {
            // 仅在输入变化时重新查找配方
            Object recipeMap = RecipeHelper.getRecipeManager(level).recipeMap();
            if (!inputResource.equals(cachedInputResource) || cachedRecipeMap != recipeMap) {
                progress = 0;
                cachedRecipeMap = recipeMap;
                ItemStack inputStack = inputResource.toStack(1);
                cachedInputResource = inputResource;

                // 优先查找自定义机器配方，然后查找原版熔炼配方
                cachedMachineRecipe = RecipeHelper.findMachineRecipe(level, "electric_furnace", inputStack);
                if (cachedMachineRecipe != null) {
                    cachedAbstractCookingRecipe = null;
                    cachedOutput = cachedMachineRecipe.getOutput().copy();
                } else {
                    cachedAbstractCookingRecipe = findAbstractCookingRecipe(inputStack);
                    cachedOutput = cachedAbstractCookingRecipe != null
                        ? cachedAbstractCookingRecipe.assemble(new SingleRecipeInput(inputStack))
                        : ItemStack.EMPTY;
                }
            }

            if (!cachedOutput.isEmpty() && !canOutputAccept(cachedOutput)) {
                updateRunningState();
                return;
            }
            if (!cachedOutput.isEmpty()) {
                // 计算感应炉加成
                int inductionCount = getAdjacentInductionCount();
                double speedMultiplier = 1.0 + inductionCount * InductionFurnaceBlockEntity.SPEED_BOOST_PER_FURNACE;
                int parallelCount = 1 + inductionCount * InductionFurnaceBlockEntity.PARALLEL_PER_FURNACE;


                // 自定义配方使用配方的时间和能量，原版配方使用默认值
                int processTime;
                long energyPerTick;
                if (cachedMachineRecipe != null) {
                    processTime = overclockedTime(cachedMachineRecipe.getProcessTime());
                    energyPerTick = overclockedEnergy(cachedMachineRecipe.getEnergyPerTick());
                } else {
                    processTime = overclockedTime(cachedAbstractCookingRecipe.cookingTime());
                    energyPerTick = overclockedEnergy(BASE_ENERGY_CONSUMPTION);
                }

                // 感应炉加速：缩短加工时间，增加能耗
                if (inductionCount > 0) {
                    processTime = (int) Math.max(1, processTime / speedMultiplier);
                    energyPerTick = (long) (energyPerTick * speedMultiplier);
                }
                maxProgress = processTime;

                // 尝试消耗能量（先从自身缓冲，不足时从感应炉抽取）
                long consumed = consumeEnergyWithInduction(energyPerTick, inductionCount);
                if (consumed <= 0) {
                    if (wasWorking) {
                        updateRunningState();
                        setChanged();
                    }
                    return;
                }

                progress++;

                if (progress >= maxProgress) {
                    // 并行处理：一次完成时尝试处理多个物品
                    int processed = tryProcessOutput(parallelCount);
                    if (processed > 0) {
                        progress = 0;
                    }
                }
            } else {
                progress = 0;
                cachedOutput = ItemStack.EMPTY;
                // Cache misses too: do not scan every recipe again on every tick.
            }
        } else if (inputResource.isEmpty() || inputAmount <= 0) {
            progress = 0;
            cachedAbstractCookingRecipe = null;
            cachedMachineRecipe = null;
            cachedOutput = ItemStack.EMPTY;
            cachedInputResource = ItemResource.EMPTY;
        }

        if (wasWorking != isWorking()) {
            updateRunningState();
            setChanged();
        }
    }

    /**
     * 获取相邻感应炉数量（实例方法，使用缓存）
     */
    private int getAdjacentInductionCount() {
        if (cachedInductionCount >= 0) {
            return cachedInductionCount;
        }
        cachedInductionCount = getAdjacentInductionCount(level, worldPosition);
        return cachedInductionCount;
    }

    /**
     * 获取相邻感应炉数量（静态方法，供 Jade 等外部调用）
     */
    public static int getAdjacentInductionCount(net.minecraft.world.level.Level level, BlockPos pos) {
        int count = 0;
        if (level == null) return count;
        for (Direction direction : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(direction));
            if (be instanceof InductionFurnaceBlockEntity) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从自身缓冲和相邻感应炉统一消耗能量
     *
     * @param required       需要的能量
     * @param inductionCount 相邻感应炉数量
     * @return 实际消耗的能量
     */
    private long consumeEnergyWithInduction(long required, int inductionCount) {
        // 先从自身缓冲消耗
        long consumed = energyStorage.removeEnergy(required);
        if (consumed >= required) {
            return consumed;
        }

        // 不足部分从相邻感应炉抽取
        if (inductionCount > 0) {
            long remaining = required - consumed;
            long perFurnace = (long) Math.ceil((double) remaining / inductionCount);

            for (Direction direction : Direction.values()) {
                if (remaining <= 0) break;
                BlockEntity be = level.getBlockEntity(worldPosition.relative(direction));
                if (be instanceof InductionFurnaceBlockEntity induction) {
                    long pulled = induction.provideEnergy(Math.min(perFurnace, remaining));
                    consumed += pulled;
                    remaining -= pulled;
                }
            }
        }

        return consumed;
    }

    /**
     * 并行处理输出
     *
     * @param parallelCount 并行数
     * @return 成功处理的物品数
     */
    private int tryProcessOutput(int parallelCount) {
        if (cachedOutput.isEmpty()) return 0;

        ItemResource inputResource = itemHandler.getResource(INPUT_SLOT);
        int inputAmount = itemHandler.getAmountAsInt(INPUT_SLOT);
        ItemStack outputStack = cachedOutput;
        ItemResource outputResource = ItemResource.of(outputStack);
        int processed = 0;

        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 0; i < parallelCount; i++) {
                // 检查输入是否还有
                int remainingInput = itemHandler.getAmountAsInt(INPUT_SLOT);
                if (remainingInput <= 0) break;

                // 检查输出是否可接收
                ItemResource currentOutputRes = itemHandler.getResource(OUTPUT_SLOT);
                int currentOutputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);
                if (!currentOutputRes.isEmpty() && !currentOutputRes.equals(outputResource)) break;
                if (currentOutputAmount + outputStack.getCount() > outputStack.getMaxStackSize()) break;

                int inserted = itemHandler.insert(OUTPUT_SLOT, outputResource, outputStack.getCount(), tx);
                if (inserted > 0) {
                    itemHandler.extract(INPUT_SLOT, inputResource, 1, tx);
                    processed++;

                    // 概率副产（尽力插入输出槽，放不下则舍弃，不阻塞主产物）
                    if (cachedMachineRecipe != null && cachedMachineRecipe.hasExtraResults() && level != null) {
                        for (MachineRecipe.ExtraResult extra : cachedMachineRecipe.getExtraResults()) {
                            if (level.getRandom().nextFloat() >= extra.chance()) continue;
                            ItemStack extraStack = extra.create();
                            itemHandler.insert(OUTPUT_SLOT, ItemResource.of(extraStack), extraStack.getCount(), tx);
                        }
                    }
                } else {
                    break;
                }
            }

            if (processed > 0) {
                tx.commit();
            }
        }

        return processed;
    }

    /**
     * 查找原版熔炼配方
     */
    @Nullable
    private AbstractCookingRecipe findAbstractCookingRecipe(ItemStack input) {
        return RecipeHelper.findCookingRecipe(level, input);
    }

    /**
     * 检查输出槽是否可以接收指定物品
     */
    private boolean canOutputAccept(ItemStack output) {
        ItemResource outputResource = itemHandler.getResource(OUTPUT_SLOT);
        int outputAmount = itemHandler.getAmountAsInt(OUTPUT_SLOT);

        if (outputResource.isEmpty() || outputAmount <= 0) {
            return true;
        }

        // 输出槽已有物品，检查是否相同且未满
        ItemStack existingStack = outputResource.toStack(1);
        if (ItemStack.isSameItemSameComponents(existingStack, output)) {
            return outputAmount + output.getCount() <= existingStack.getMaxStackSize();
        }

        return false;
    }

    @Override
    public String getMachineName() {
        return "电炉";
    }

    private static final Component DISPLAY_NAME = Component.literal("电炉");

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    public MachinePriority getMachinePriority() {
        return MachinePriority.HIGH;
    }

    @Override
    public String getProgressLabel() {
        return "";
    }

    /**
     * 兼容旧版 INT[] 格式和新版 number 格式加载整数字段
     */
    private static int loadIntField(ValueInput input, String key, int defaultValue) {
        // 优先尝试读取 number 格式
        var optInt = input.getInt(key);
        if (optInt.isPresent()) return optInt.get();

        // 兼容旧版 INT[] 格式：取数组第一个元素
        var intArr = input.getIntArray(key);
        if (intArr != null && intArr.isPresent()) {
            int[] arr = intArr.get();
            if (arr.length > 0) return arr[0];
        }

        return defaultValue;
    }

    @Override
    protected void loadExtra(ValueInput input) {
        // 兼容旧版把进度存成 INT[] 的存档
        progress = loadIntField(input, "progress", 0);
        maxProgress = loadIntField(input, "maxProgress", BASE_PROCESS_TIME);
        cachedAbstractCookingRecipe = null;
        cachedMachineRecipe = null;
        cachedOutput = ItemStack.EMPTY;
        cachedInputResource = ItemResource.EMPTY;
        cachedInductionCount = -1;
    }
}
