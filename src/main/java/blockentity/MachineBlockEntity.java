package blockentity;

import energy.EUCapability;
import energy.EUEnergyCapabilityAdapter;
import energy.EUEnergyStorage;
import item.EjectorUpgradeItem;
import item.FluidEjectorUpgradeItem;
import item.FluidPullingUpgradeItem;
import item.PullingUpgradeItem;
import network.performance.MachinePriority;
import network.performance.TickThrottler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 机器方块实体基类
 */
public abstract class MachineBlockEntity extends BlockEntity implements TickableBlockEntity {

    /** 当前tick计数 */
    protected long tickCount = 0;

    /** 是否已注册到网络（避免重复注册） */
    private boolean registeredToNetwork = false;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract String getMachineName();

    @Override
    public void tick() {
        tickCount++;

        // 确保机器已注册到网络管理器（处理世界重载情况）
        if (!registeredToNetwork && level != null && !level.isClientSide()) {
            network.EnergyTransferManager.register(this);
            registeredToNetwork = true;
        }

        if (level != null && !level.isClientSide() && level.tickRateManager().runsNormally()
                && this instanceof machine.StorageGuiProvider storage) {
            energy.ItemCharging.charge(storage.getEnergyStorage(), storage.getItemHandler(), tickCount);
        }

        // 服务器冻结时跳过机器处理，避免无意义运算
        if (level != null && !level.isClientSide() && !level.tickRateManager().runsNormally()) {
            return;
        }

        // 性能节流：根据当前性能状态和机器优先级决定是否跳过此tick
        if (level != null && !level.isClientSide()) {
            if (!TickThrottler.getInstance().shouldTick(getMachinePriority(), tickCount)) {
                return;
            }
        }
    }

    /**
     * 获取机器的性能优先级
     * <p>
     * 子类可覆写此方法以指定不同的优先级。默认为MEDIUM。
     * 储能设备应返回CRITICAL，工作中的机器返回HIGH，空闲机器返回LOW。
     * </p>
     */
    public MachinePriority getMachinePriority() {
        return MachinePriority.MEDIUM;
    }

    /**
     * 获取机器的物品处理器（用于升级系统）
     * 子类应覆写此方法以提供实际的物品处理器
     */
    @Nullable
    public ItemStacksResourceHandler getItemHandler() {
        return null;
    }

    /**
     * 获取机器的升级槽数量
     * 子类应覆写此方法以提供实际的升级槽数量
     */
    public int getUpgradeSlotCount() {
        return 0;
    }

    /**
     * 获取升级槽的起始索引
     * 子类应覆写此方法以提供实际的升级槽起始索引
     */
    public int getUpgradeSlotStart() {
        return 0;
    }

    /**
     * Slots that accept raw materials. Item-transfer upgrades insert only into these.
     * The default assumes the slots before the upgrade slots are split evenly into inputs then outputs;
     * machines with a different layout must override both methods.
     */
    public int[] getInputSlots() {
        return range(0, getUpgradeSlotStart() / 2);
    }

    /** Slots that hold products. Item-transfer upgrades extract only from these. */
    public int[] getOutputSlots() {
        return range(getUpgradeSlotStart() / 2, getUpgradeSlotStart());
    }

    @Nullable
    private AutomationItemHandler automationItems;

    /** Extra filter for items arriving from hoppers and pipes; {@code slot} is one of {@link #getInputSlots()}. */
    public boolean acceptsAutomationInput(int slot, net.neoforged.neoforge.transfer.item.ItemResource resource) {
        return true;
    }

    /**
     * The inventory view for hoppers and other mods' pipes (inputs in, outputs out, upgrades hidden),
     * or null when the machine has no input or output slots.
     */
    @Nullable
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getAutomationItemHandler() {
        ItemStacksResourceHandler inventory = getItemHandler();
        if (inventory == null) return null;
        if (automationItems == null) {
            int[] inputs = getInputSlots();
            int[] outputs = getOutputSlots();
            if (inputs.length == 0 && outputs.length == 0) return null;
            automationItems = new AutomationItemHandler(this, inventory, inputs, outputs);
        }
        return automationItems;
    }

    protected static int[] range(int from, int to) {
        int[] slots = new int[Math.max(0, to - from)];
        for (int i = 0; i < slots.length; i++) slots[i] = from + i;
        return slots;
    }

    /**
     * 处理升级效果
     * 在每个tick中调用，处理所有安装的升级
     * <p>
     * 优化：先通过tick间隔判断是否需要执行操作，避免每tick创建ItemStack对象。
     * 仅在间隔匹配时才创建ItemStack，大幅减少GC压力。
     * </p>
     */
    protected void processUpgrades() {
        ItemStacksResourceHandler itemHandler = getItemHandler();
        if (itemHandler == null) return;

        int upgradeSlotCount = getUpgradeSlotCount();
        int upgradeSlotStart = getUpgradeSlotStart();

        // 先检查是否有任何升级需要在此tick执行，避免不必要的ItemStack创建
        boolean isTransferTick = tickCount % EjectorUpgradeItem.TRANSFER_INTERVAL == 0;
        if (!isTransferTick) return;

        for (int i = 0; i < upgradeSlotCount; i++) {
            int slotIndex = upgradeSlotStart + i;
            ItemResource resource = itemHandler.getResource(slotIndex);
            int amount = itemHandler.getAmountAsInt(slotIndex);

            if (resource.isEmpty() || amount <= 0) continue;

            // 直接通过 ItemResource 获取 Item 类型，避免创建 ItemStack
            net.minecraft.world.item.Item item = resource.getItem();

            // 处理抽取升级
            if (item instanceof PullingUpgradeItem pullingUpgrade) {
                ItemStack upgradeStack = resource.toStack(amount);
                PullingUpgradeItem.tryPull(upgradeStack, this);
            }

            // 处理弹出升级
            if (item instanceof EjectorUpgradeItem ejectorUpgrade) {
                ItemStack upgradeStack = resource.toStack(amount);
                EjectorUpgradeItem.tryEject(upgradeStack, this);
            }

            // 处理流体输出升级
            if (item instanceof FluidEjectorUpgradeItem) {
                ItemStack upgradeStack = resource.toStack(amount);
                FluidEjectorUpgradeItem.tryEjectFluid(upgradeStack, this);
            }

            // 处理流体输入升级
            if (item instanceof FluidPullingUpgradeItem) {
                ItemStack upgradeStack = resource.toStack(amount);
                FluidPullingUpgradeItem.tryPull(upgradeStack, this);
            }
        }
    }

    /**
     * Drops everything held in the item slots when the block is broken (by hand, explosion or piston).
     * The wrench dismantles differently: it collects the contents itself before removing the block.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ItemStacksResourceHandler inventory = getItemHandler();
        if (inventory == null || level == null) return;
        for (int slot = 0; slot < inventory.size(); slot++) {
            int amount = inventory.getAmountAsInt(slot);
            if (amount <= 0) continue;
            ItemResource resource = inventory.getResource(slot);
            inventory.set(slot, ItemResource.EMPTY, 0);
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), resource.toStack(amount));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("TickCount", tickCount);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // 不再重置 tickCount，使用 registeredToNetwork 标志控制注册
        registeredToNetwork = false;
    }

    /**
     * 方块被放置时调用
     */
    public void onPlace() {
        if (level != null && !level.isClientSide()) {
            network.EnergyTransferManager.register(this);
            registeredToNetwork = true;
        }
    }

    /**
     * 方块被移除时调用
     */
    public void onRemove() {
        if (level != null && !level.isClientSide()) {
            network.EnergyTransferManager.remove(this);
        }
    }

    @Override
    public void setRemoved() {
        network.EnergyTransferManager.remove(this);
        registeredToNetwork = false;
        super.setRemoved();
    }

    @Nullable
    private EUCapability cachedEUCapability;

    /**
     * 子类覆写以指定是否可以接收能量（默认true）
     */
    protected boolean canReceiveEU() { return true; }

    /**
     * 子类覆写以指定是否可以输出能量（默认true）
     */
    protected boolean canExtractEU() { return true; }

    @Nullable
    public EUCapability getEUCapability(@Nullable Direction direction) {
        if (cachedEUCapability != null) return cachedEUCapability;
        if (this instanceof EnergyDisplay display) {
            EUEnergyStorage storage = display.getEnergyStorage();
            boolean canReceive = canReceiveEU();
            boolean canExtract = canExtractEU();
            cachedEUCapability = new EUEnergyCapabilityAdapter(storage, storage.getVoltage()) {
                @Override public boolean canReceive() { return canReceive; }
                @Override public boolean canExtract() { return canExtract; }
            };
            return cachedEUCapability;
        }
        return null;
    }
}
