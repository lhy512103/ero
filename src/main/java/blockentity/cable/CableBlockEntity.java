package blockentity.cable;

import block.cable.CableBlock;
import blockentity.TickableBlockEntity;
import energy.EUCapability;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import network.EnergyTransferManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import javax.annotation.Nullable;

/** Cable topology endpoint. Transfer is performed once by EnergyTransferManager, not once per cable. */
public class CableBlockEntity extends BlockEntity implements TickableBlockEntity {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final EUVoltage voltage;
    private final float thickness;
    private final EUEnergyStorageImpl legacyBuffer;
    private int disabledSides;
    private boolean initialized;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        this(((CableBlock) state.getBlock()).getBlockEntityType(), pos, state);
    }
    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        CableBlock block = (CableBlock) state.getBlock();
        voltage = block.getVoltage();
        thickness = block.getThickness();
        // Preserve previously saved wire energy rather than deleting it on upgrade.
        legacyBuffer = new EUEnergyStorageImpl(voltage.getMaxVoltage() * 4, voltage);
    }
    public EUVoltage getVoltage() { return voltage; }
    public float getThickness() { return thickness; }
    public EUEnergyStorage getEnergyStorage() { return legacyBuffer; }
    public record NetworkEnergyInfo(long energy, long capacity, int cableCount) {}
    public NetworkEnergyInfo getNetworkEnergyInfo() {
        return new NetworkEnergyInfo(legacyBuffer.getEnergyStored(), legacyBuffer.getMaxEnergyStored(), 1);
    }
    public boolean isSideEnabled(Direction side) { return (disabledSides & (1 << side.ordinal())) == 0; }
    public void toggleSide(Direction side) {
        disabledSides ^= 1 << side.ordinal();
        setChanged();
        updateConnections();
        if (level != null) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (neighbor instanceof CableBlockEntity cable) cable.updateConnections();
            EnergyTransferManager.invalidate(level);
        }
    }
    private static BooleanProperty property(Direction side) {
        return switch (side) {
            case NORTH -> CableBlock.NORTH; case SOUTH -> CableBlock.SOUTH;
            case EAST -> CableBlock.EAST; case WEST -> CableBlock.WEST;
            case UP -> CableBlock.UP; case DOWN -> CableBlock.DOWN;
        };
    }
    public void updateConnections() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        BlockState updated = state;
        for (Direction side : DIRECTIONS) {
            BlockPos pos = worldPosition.relative(side);
            boolean connect = false;
            if (isSideEnabled(side) && level.hasChunkAt(pos)) {
                BlockEntity neighbor = level.getBlockEntity(pos);
                if (neighbor instanceof CableBlockEntity cable) connect = cable.isSideEnabled(side.getOpposite());
                else connect = level.getCapability(EUCapability.BLOCK, pos, side.getOpposite()) != null;
            }
            updated = updated.setValue(property(side), connect);
        }
        if (updated != state) level.setBlockAndUpdate(worldPosition, updated);
    }
    @Override public void tick() {
        if (level == null || level.isClientSide() || initialized) return;
        initialized = true;
        updateConnections();
        for (Direction side : DIRECTIONS) {
            BlockPos pos = worldPosition.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof CableBlockEntity cable) cable.updateConnections();
        }
        EnergyTransferManager.invalidate(level);
    }
    @Override public void setRemoved() {
        if (level != null) EnergyTransferManager.invalidate(level);
        initialized = false;
        super.setRemoved();
    }
    public void findOrCreateSharedStorage() { if (level != null) EnergyTransferManager.invalidate(level); }
    public static void redistributeStorageOnRemoval(Level level, BlockPos pos) { EnergyTransferManager.invalidate(level); }
    public void invalidateNetworkCache() { if (level != null) EnergyTransferManager.invalidate(level); }
    @Nullable public EUCapability getEUCapability(@Nullable Direction side) {
        if (side == null || !isSideEnabled(side)) return null;
        // Expose only legacy stored energy; new transfers never fill a second wire buffer.
        return new energy.EUEnergyCapabilityAdapter(legacyBuffer, voltage) {
            @Override public boolean canReceive() { return false; }
            @Override public long receiveEnergy(long amount, boolean simulate) { return 0; }
        };
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("DisabledSides", disabledSides);
        output.putLong("Energy", legacyBuffer.getEnergyStored());
    }
    @Override public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        disabledSides = input.getIntOr("DisabledSides", 0) & 63;
        legacyBuffer.setEnergy(input.getLongOr("Energy", 0));
        initialized = false;
    }
}
