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

/**
 * 储能箱方块实体
 * <p>
 * 基础能量存储设备，可以接收和输出EU能量。
 * 默认配置：
 * - 电压等级：LV (32 EU/t)
 * - 内部缓冲：40000 EU
 * - 输入/输出速率：32 EU/t
 * </p>
 */
public class BatboxBlockEntity extends MachineBlockEntity implements EnergyDisplay, StorageGuiProvider {

    /** 能量存储 */
    private final EUEnergyStorageImpl energyStorage;

    /** 电压等级 */
    private final EUVoltage voltage;

    /** 物品处理器：充电槽、放电槽、2升级槽、充电槽1、充电槽2、4装备充电槽 */
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(10) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };

    /**
     * 创建储能箱方块实体
     *
     * @param pos   方块位置
     * @param state 方块状态
     */
    public BatboxBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, EUVoltage.LV, 40000);
    }

    /**
     * 创建储能箱方块实体（自定义配置）
     *
     * @param pos            方块位置
     * @param state          方块状态
     * @param voltage        电压等级
     * @param bufferCapacity 内部缓冲容量 (EU)
     */
    public BatboxBlockEntity(BlockPos pos, BlockState state, EUVoltage voltage, long bufferCapacity) {
        super(ModBlockEntities.BATBOX.get(), pos, state);
        this.voltage = voltage;
        this.energyStorage = new EUEnergyStorageImpl(bufferCapacity, voltage);
    }
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) {
            return;
        }
        // 储能箱自身不需要主动处理逻辑，能量传输由网络系统处理
    }
    @Override
    public String getMachineName() {
        return "储能箱";
    }
    @Override
    public EUEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    private static final Component DISPLAY_NAME = Component.literal("储能箱");

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
