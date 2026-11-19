package block;

import blockentity.FluidTankBlockEntity;
import blockentity.SiloBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Silo (one kind of item, 64 stacks) or fluid tank (16 buckets); which one is decided at registration. */
public class StorageBlock extends BaseEntityBlock {
    public static final MapCodec<StorageBlock> CODEC = BlockBehaviour.simpleCodec(properties -> new StorageBlock(properties, false));
    private final boolean fluid;

    public StorageBlock(Properties properties, boolean fluid) {
        super(properties);
        this.fluid = fluid;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return fluid ? new FluidTankBlockEntity(pos, state) : new SiloBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SiloBlockEntity silo) return silo.deposit(player, stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        if (entity instanceof FluidTankBlockEntity tank) return tank.useBucket(player, hand, stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SiloBlockEntity silo) silo.withdraw(player);
        if (entity instanceof FluidTankBlockEntity tank) player.sendOverlayMessage(tank.describe());
        return InteractionResult.SUCCESS;
    }
}
