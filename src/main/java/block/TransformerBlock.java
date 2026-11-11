package block;

import blockentity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import energy.EUVoltage;
import machine.transformer.TransformerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 变压器方块：没有界面，空手右键循环切换输出电压等级。 */
public class TransformerBlock extends MachineBlock {

    public static final MapCodec<TransformerBlock> TRANSFORMER_CODEC = BlockBehaviour.simpleCodec(TransformerBlock::new);

    public TransformerBlock(Properties properties) {
        super(properties, TransformerBlockEntity::new, () -> ModBlockEntities.TRANSFORMER.get());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return TRANSFORMER_CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TransformerBlockEntity transformer) {
            EUVoltage output = transformer.cycleOutput();
            player.sendOverlayMessage(Component.translatable("message.iuce.transformer.output",
                output.getName(), output.getMaxVoltage(), transformer.getRate()));
        }
        return InteractionResult.SUCCESS;
    }
}
