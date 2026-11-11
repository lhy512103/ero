package blockentity;

import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FissionControllerBlockEntity extends MachineBlockEntity implements MenuProvider, EnergyDisplay {

    public static final long CAPACITY = 100_000L;
    public static final long MAX_RECEIVE = 512L;

    private final EUEnergyStorageImpl energyStorage = new EUEnergyStorageImpl(CAPACITY, MAX_RECEIVE, EUVoltage.HV);

    public FissionControllerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FISSION_CONTROLLER.get(), pos, state);
    }

    public FissionControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public String getMachineName() {
        return "fission_controller";
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iuce.fission_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }

    @Override
    public EUEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
}
